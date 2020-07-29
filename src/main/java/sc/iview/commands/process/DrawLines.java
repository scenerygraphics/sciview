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
package sc.iview.commands.process;

import graphics.scenery.Cylinder;
import graphics.scenery.Node;
import net.imagej.ops.OpService;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.iview.SciView;
import sc.iview.node.Line3D;
import sc.iview.process.ControlPoints;
import sc.iview.vector.Vector3;

import java.util.ArrayList;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_DRAW_LINES;

/**
 * Interactively place points that are used to draw a line
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Process", weight = PROCESS), //
                 @Menu(label = "Draw Lines", weight = PROCESS_DRAW_LINES) })
public class DrawLines extends InteractiveCommand {

    @Parameter
    private OpService opService;

    @Parameter
    private SciView sciView;

    @Parameter(callback = "createLine")
    private Button createLine;

    private Node targetPoint;
    private ControlPoints controlPoints;

    protected float controlPointDistance = 10;

    @Override
    public void run() {
        controlPoints = new ControlPoints();

        controlPoints.initializeSciView(sciView,controlPointDistance);
    }

    /* Create a ConvexHulls of controlPoints */
    public void createLine() {
        //Line line = new Line();

        ArrayList<Vector3f> points = new ArrayList<>();
        for( Vector3 v : controlPoints.getVertices() ) {
            points.add(new Vector3f(v.xf(), v.yf(), v.zf()));
        }

        float r = 0.1f;
        float h;
        int s = 15;
        Vector3f p1,p2;

        // Add Group Node for lines, or make a Line3D
        Line3D line = new Line3D();
        for( int k = 0; k < points.size()-1; k++ ) {
            p1 = points.get(k);
            p2 = points.get(k+1);
            Cylinder c = Cylinder.betweenPoints(p1,p2,r,1,s);
            //sciView.addNode(c,false);
            line.addLine(c);
        }
        line.setName("Line3D");
        sciView.addNode(line, false);

        controlPoints.cleanup(sciView);
    }

    @Override
    public void cancel() {
        controlPoints.cleanup(sciView);
    }


}
