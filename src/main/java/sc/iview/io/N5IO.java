/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.io;

import bdv.util.AxisOrder;
import bvv.core.VolumeViewerOptions;
import graphics.scenery.Group;
import graphics.scenery.Node;
import graphics.scenery.primitives.PointCloud;
import graphics.scenery.volumes.Volume;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;
import sc.iview.SciViewService;
import sc.iview.process.MeshConverter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** {@link IOPlugin} adapter for N5 as a data source (volume or mesh)
 *
 * @author Kyle Harrington
 *
 * */
@Plugin(type = IOPlugin.class)
public class N5IO extends AbstractIOPlugin<graphics.scenery.Node> {

    @Override
    public graphics.scenery.Node open( final String source ) throws IOException {

        int splitPoint = source.indexOf(".n5/") + 3;
        String n5Path = source.substring(0, splitPoint);
        String dataset = source.substring(splitPoint) + "/";

        N5Reader n5Reader = new N5FSReader(n5Path);
        // TODO: select the dataset (show n5 tree)

        Node node;
        //if( n5Reader.datasetExists( dataset ) ) {
        if( new File(source).exists() ) {
            node = open(n5Reader, dataset);
        } else {
            throw new IOException("Dataset " + dataset + " does not exist in n5: " + n5Reader.toString() );
        }

        // TODO: Remember to also read the metadata for the node. this is generic to Node

        return node;
    }

    public graphics.scenery.Node open( final N5Reader n5Reader, final String dataset ) throws IOException {
        Node node;
        String nodeType = getNodeType( n5Reader, dataset );

        // Fail if SciView is not open. We use the active sciview
        SciViewService sciViewService = context().service(SciViewService.class);
        if( sciViewService.numSciView() == 0 ) {
            throw new IOException("SciView is not open, needed for file opening.");
        }

        SciView sv = sciViewService.getActiveSciView();

        if( nodeType.startsWith("sciview") ) {
            node = openSciview( n5Reader, dataset, nodeType );
        } else {
            // TODO check for multiresolution and such here

            // Note: UnsignedByteType is currently hard coded due to BVV constraints
            RandomAccessibleInterval<UnsignedByteType> image = N5Utils.open(n5Reader, dataset);

            long[] dimensions = new long[image.numDimensions()];
            image.dimensions( dimensions );

            long[] minPt = new long[image.numDimensions()];

            // Get type at min point
            RandomAccess<UnsignedByteType> imageRA = image.randomAccess();
            image.min(minPt);
            imageRA.setPosition(minPt);

            node = Volume.fromRAI(image, new UnsignedByteType(), AxisOrder.DEFAULT, dataset, sv.getHub(), new VolumeViewerOptions());
        }

        return node;
    }

    private Node openSciview(N5Reader n5Reader, String dataset, String nodeType ) throws IOException {
        Node node;
        // If it is a mesh, then load

        if( nodeType.compareToIgnoreCase("sciview-1.0.0 trimesh") == 0 ) {
            node = MeshConverter.toScenery( N5.openMesh(n5Reader, dataset) );
        } else if( nodeType.compareToIgnoreCase("sciview-1.0.0 points") == 0 ) {
            // TODO this can be better
            List<RealLocalizable> points = N5.openPoints(n5Reader, dataset);
            if( points.size() == 0 ) {
                node = new Group();
            } else {
                int numDim = points.get(0).numDimensions();
                float[] array = new float[points.size() * numDim];

                for( int k = 0; k < points.size(); k++ ) {
                    RealLocalizable point = points.get(k);
                    for (int d = 0; d < numDim; d++) {
                        array[k * numDim + d] = point.getFloatPosition(d);
                    }
                }

                node = PointCloud.Companion.fromArray(array);
            }
        } else {
            throw new IOException("Cannot open dataset: " + dataset + " in " + n5Reader);
        }

        node.setName(dataset);

        return node;
    }

    private String getNodeType(N5Reader n5Reader, String dataset) throws IOException {
        return n5Reader.getAttribute(dataset, "nodeType", String.class);
    }

    @Override
    public Class<graphics.scenery.Node> getDataType() {
        return graphics.scenery.Node.class;
    }

    @Override
    public boolean supportsOpen(final String source) {
        return source.contains(".n5/");// TODO check what N5 folks are doing, this assumes n5+dataset in same path and parses accordingly
        //return FileUtils.getExtension(source).toLowerCase().equals(EXTENSION);
    }

    String EXTENSION = "n5";
}
