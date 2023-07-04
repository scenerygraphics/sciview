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
package sc.iview.commands.demo.advanced;

import graphics.scenery.attribute.material.Material;
import graphics.scenery.volumes.Volume;
import io.scif.services.DatasetIOService;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo of rendering meshes
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Show segmentation", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Advanced", weight = DEMO_ADVANCED), //
                 @Menu(label = "Show segmentation", weight = DEMO_ADVANCED_SEGMENTATION) })
public class ShowSegmentationDemo implements Command {

    @Parameter
    private DatasetIOService datasetIO;

    @Parameter
    private LogService log;

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter
    private int numSegments = 20;

    private Random rng = new Random();

    @Override
    public void run() {
        sciView.getFloor().setVisible(false);

        RandomAccessibleInterval<UnsignedByteType> inputImage = generateDemo(100, 100, 100, numSegments);

        Volume v = sciView.addVolume( inputImage, "Input Image", new float[] { 1, 1, 1 } );
        v.setPixelToWorldRatio(1f);
        v.setName( "Segmentation Viz Demo" );

        ImgLabeling<Integer, IntType> labeling = ops.labeling().cca(inputImage, ConnectedComponents.StructuringElement.FOUR_CONNECTED);
        LabelRegions<Integer> regions = new LabelRegions<>(labeling);

        for( LabelRegion region : regions ) {
            // Generate the mesh with imagej-ops
            Mesh m = Meshes.marchingCubes(region);

            // Convert the mesh into a scenery mesh for visualization
            graphics.scenery.Mesh isoSurfaceMesh = MeshConverter.toScenery(m,false, true);

            // Name the mesh after the segment label
            isoSurfaceMesh.setName( "region " + region );

            // Make a random color and assign it
            Vector3f c = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
            isoSurfaceMesh.material().setDiffuse(c);
            isoSurfaceMesh.material().setAmbient(c);
            isoSurfaceMesh.material().setSpecular(c);
            isoSurfaceMesh.material().setCullingMode(Material.CullingMode.Front);

            // Make the segmentation mesh a child of the parent
            v.addChild(isoSurfaceMesh);
            // We need to publish our node to sciview to update the UI
            sciView.publishNode(isoSurfaceMesh);
        }

        sciView.centerOnNode(v);
    }

    // Generate a demo image with a bunch of spheres at random positions
    private RandomAccessibleInterval<UnsignedByteType> generateDemo(int w, int h, int d, int numSegments) {
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
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<String, Object>();

        command.run(ShowSegmentationDemo.class, true, argmap);
    }
}
