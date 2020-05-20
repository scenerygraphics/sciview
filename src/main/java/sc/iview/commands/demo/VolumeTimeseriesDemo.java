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

import bdv.util.BdvFunctions;
import graphics.scenery.Node;
import graphics.scenery.numerics.OpenSimplexNoise;
import graphics.scenery.volumes.Volume;
import ij.IJ;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import sc.iview.SciView;
import sc.iview.process.MeshConverter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.BiConsumer;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_VOLUME_RENDER;

/**
 * A demo of volume rendering + time
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Volume Timeseries", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Volume Timeseries", weight = DEMO_VOLUME_RENDER) })
public class VolumeTimeseriesDemo implements Command {

    @Parameter
    private LogService log;

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter
    private IOService ioService;

    @Override
    public void run() {
        final RandomAccessibleInterval<UnsignedByteType> dataset = makeDataset();

//        try {
//            ioService.save(
//                    ImageJFunctions.wrap(dataset,"test"),
//                    "/home/kharrington/Data/sciview/test_volumetimeseries.tif");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        IJ.saveAsTiff(
                ImageJFunctions.wrap(dataset,"test"),
                "/home/kharrington/Data/sciview/test_volumetimeseries.tif");

        //ImageJFunctions.wrap(dataset, "test");
        BdvFunctions.show(dataset, "test");

        Volume v = (Volume) sciView.addVolume( dataset, new float[] { 1, 1, 1, 1 } );
        v.setPixelToWorldRatio(0.1f);// FIXME
        v.setName( "Volume Render Demo" );
        v.setDirty(true);
        v.setNeedsUpdate(true);

        sciView.setActiveNode(v);
        sciView.centerOnNode( sciView.getActiveNode() );
    }

    public RandomAccessibleInterval<UnsignedByteType> makeDataset() {
        // Interval is 30x30x30 w/ 10 timepoints
        FinalInterval interval = new FinalInterval(
                new long[]{0, 0, 0, 0},
                new long[]{30, 30, 30, 10});

        double center = interval.max(2) / 2;

        OpenSimplexNoise noise = new OpenSimplexNoise();


        Random rng = new Random(System.nanoTime());

        float dx, dy, dz;
        dx = rng.nextFloat();
        dy = rng.nextFloat();
        dz = rng.nextFloat();

        double f = 3.0 / (double) interval.max(2);
        double dt = 0.618;
        double radius = 0.35;

        BiConsumer<Localizable, UnsignedByteType> pixelmapper = new BiConsumer<Localizable, UnsignedByteType>() {
            @Override
            public void accept(Localizable localizable, UnsignedByteType val) {
                double x = center - localizable.getDoublePosition(0);
                double y = center - localizable.getDoublePosition(1);
                double z = center - localizable.getDoublePosition(2);
                double t = localizable.getDoublePosition(3);

                double d = Math.sqrt(x * x + y * y + z * z) / (double) interval.max(2);
                double offset = Math.abs(
                        noise.random3D(
                                (x + t * dt * dx) * f,
                                (y + t * dt * dy) * f,
                                (z + t * dt * dz) * f));

                //System.out.println(x + " " + y + " " + z + " " + t + " c " + center + " " + offset);

                double v;
                if ( d - offset < radius )
                    v = d - offset;
                else
                    v = 0;
                val.set((int) (255.0 * v));
            }
        };
        FunctionRandomAccessible<UnsignedByteType> fra = new FunctionRandomAccessible<UnsignedByteType>(
                4, pixelmapper, UnsignedByteType::new);

        return hardCopy(Views.interval(fra, interval));
    }

    public Img<UnsignedByteType> hardCopy(RandomAccessibleInterval<UnsignedByteType> img) {
        Img<UnsignedByteType> out =
                ArrayImgs.unsignedBytes(
                        img.dimension(0),
                        img.dimension(1),
                        img.dimension(2),
                        img.dimension(3));
        RandomAccess<UnsignedByteType> imgAccess = img.randomAccess();

        Cursor<UnsignedByteType> outCur = out.localizingCursor();
        while( outCur.hasNext() ) {
            outCur.fwd();
            imgAccess.setPosition(outCur);
            outCur.get().set(imgAccess.get());
        }

        return out;
    }


    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

//        ImageJ imagej = new ImageJ(sv.getScijavaContext());
//        imagej.ui().showUI();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<String, Object>();

        command.run(VolumeTimeseriesDemo.class, true, argmap);
    }
}
