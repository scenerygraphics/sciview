/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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
package sc.iview.commands.demo;

import graphics.scenery.Node;
import graphics.scenery.volumes.Volume;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;
import sc.iview.process.MeshConverter;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_VOLUME_RENDER;

/**
 * A demo of volume rendering.
 *
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, label = "Show segmentation", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Show segmentation", weight = DEMO_VOLUME_RENDER) })
public class ShowSegmentationDemo implements Command {

    @Parameter
    private DatasetIOService datasetIO;

    @Parameter
    private LogService log;

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    private Random rng = new Random();

    @Override
    public void run() {
        int numSegments = 5;

        sciView.getFloor().setVisible(false);

        RandomAccessibleInterval<UnsignedByteType> inputImage = generateDemo(100, 100, 100, numSegments);

        Volume v = (Volume) sciView.addVolume( inputImage, new float[] { 1, 1, 1 } );
        v.setPixelToWorldRatio(0.1f);// FIXME
        v.setName( "Volume Render Demo" );
        v.setDirty(true);
        v.setNeedsUpdate(true);

        sciView.setActiveNode(v);
        sciView.centerOnNode( sciView.getActiveNode() );

        for( int k = 0; k < numSegments; k++ ) {
            int segmentLabel = k + 1;

            RandomAccessibleInterval<UnsignedByteType> segmentImg = getSegmentImg(inputImage, segmentLabel);

            Img<BitType> bitImg = ( Img<BitType> ) ops.threshold().apply( Views.iterable(segmentImg), new UnsignedByteType( 1 ) );

            Mesh m = ops.geom().marchingCubes( bitImg, 1, new BitTypeVertexInterpolator() );

            graphics.scenery.Mesh isoSurfaceMesh = MeshConverter.toScenery(m,false);

            Vector3f c = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());

            isoSurfaceMesh.getMaterial().setDiffuse(c);
            isoSurfaceMesh.getMaterial().setAmbient(c);
            isoSurfaceMesh.getMaterial().setSpecular(c);

            v.addChild(isoSurfaceMesh);

            isoSurfaceMesh.setName( "segment " + k );
            isoSurfaceMesh.setScale(new Vector3f(v.getPixelToWorldRatio(),
                    v.getPixelToWorldRatio(),
                    v.getPixelToWorldRatio()));
        }

    }

    private RandomAccessibleInterval<UnsignedByteType> getSegmentImg(RandomAccessibleInterval<UnsignedByteType> inputImage, int segmentLabel) {
        RandomAccessibleInterval<UnsignedByteType> out = Converters.convert(inputImage, (a, b) -> b.set(a.get() == segmentLabel ? 255 : 0), new UnsignedByteType());
        return out;
    }


    private RandomAccessibleInterval<UnsignedByteType> generateDemo(int w, int h, int d, int numSegments) {
        int dimension = 3;
        double scale = 1.0;
        double stretch = 1.0;
        double radiusSq = 36;

        List<RealPoint> points = new ArrayList<>();

        for( int k = 0; k < numSegments; k++ ) {
            points.add( new RealPoint(rng.nextFloat() * w, rng.nextFloat() * h, rng.nextFloat() * d) );
        }

        long[] pos = new long[3];
        RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(w, h, d);
        Cursor<UnsignedByteType> cur = Views.iterable(img).cursor();
        while(cur.hasNext()) {
            cur.fwd();
            cur.localize(pos);

            cur.get().set(0);
            for( int k = 0; k < points.size(); k++ ) {
                double dt = dist(pos, points.get(k));
                //System.out.println(dt + " " + Arrays.toString(pos) + " " + points.get(k));
                if( dt < radiusSq ) {
                    cur.get().set(k+1);
                }
            }
        }
        return img;
    }

    private double dist(long[] pos, RealPoint realPoint) {
        return ( pos[0] - realPoint.getDoublePosition(0) ) * ( pos[0] - realPoint.getDoublePosition(0) ) +
                ( pos[1] - realPoint.getDoublePosition(1) ) * ( pos[1] - realPoint.getDoublePosition(1) ) +
                ( pos[2] - realPoint.getDoublePosition(2) ) * ( pos[2] - realPoint.getDoublePosition(2) );
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.createSciView();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<String, Object>();

        command.run(ShowSegmentationDemo.class, true, argmap);
    }
}
