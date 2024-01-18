/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
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
package sc.iview.commands.demo.basic;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo of meshes.
 *
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, label = "Mesh Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Basic", weight = DEMO_BASIC), //
                 @Menu(label = "Point Cloud", weight = DEMO_BASIC_POINTCLOUD) })
public class PointCloudDemo implements Command {

    @Parameter
    private IOService io;

    @Parameter
    private LogService log;

    @Parameter
    private SciView sciView;

    @Parameter
    private CommandService commandService;

    @Override
    public void run() {

        int numPoints = 100;

        double w = 1;
        double h = w;
        double d = w;

        List<RealLocalizable> spots = new ArrayList<RealLocalizable>();

        Random rng = new Random();

        for( int k = 0; k < numPoints; k++ ) {
            double x = rng.nextDouble() * w - w / 2;
            double y = rng.nextDouble() * h - h / 2;
            double z = rng.nextDouble() * d - d / 2;
            spots.add(new RealPoint(x, y, z));
        }

        sciView.addPointCloud(spots, "Point Cloud Demo", 50.0f);
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(PointCloudDemo.class, true, argmap);
    }
}
