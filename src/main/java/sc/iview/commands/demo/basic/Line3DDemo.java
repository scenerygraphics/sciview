/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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

import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.iview.SciView;
import sc.iview.node.Line3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo of edges.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Line3D Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Basic", weight = DEMO_BASIC), //
                 @Menu(label = "Line3D", weight = DEMO_BASIC_LINE3D) })
public class Line3DDemo implements Command {

    @Parameter
    private SciView sciView;

    @Override
    public void run() {
        int numPoints = 25;
        List<Vector3f> points = new ArrayList<>();
        List<ColorRGB> colors = new ArrayList<>();

        for( int k = 0; k < numPoints; k++ ) {
            points.add( new Vector3f( ( float ) ( 10.0f * Math.random() - 5.0f ), //
                                            ( float ) ( 10.0f * Math.random() - 5.0f ), //
                                            ( float ) ( 10.0f * Math.random() - 5.0f ) ) );
            colors.add(new ColorRGB((int) (Math.random()*255), (int) (Math.random()*255), (int) (Math.random()*255)));
        }

        double edgeWidth = 0.1;

        Line3D line = new Line3D(points, colors, edgeWidth);
        line.setName( "Line3D Demo" );

        sciView.addNode(line, true);
        sciView.getFloor().setVisible(false);

        sciView.centerOnNode( line );
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(Line3DDemo.class, true, argmap);
    }
}
