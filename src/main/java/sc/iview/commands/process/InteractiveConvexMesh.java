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
package sc.iview.commands.process;

import cleargl.GLVector;
import graphics.scenery.Camera;
import graphics.scenery.Material;
import graphics.scenery.Node;
import graphics.scenery.Sphere;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.util.ColorRGB;
import org.scijava.widget.Button;
import sc.iview.SciView;
import sc.iview.ops.DefaultConvexHullSciview;
import sc.iview.process.ControlPoints;
import sc.iview.vector.Vector3;

import java.util.List;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_INTERACTIVE_CONVEX_MESH;

/*
 * Interactively place points that are used to seed a convex hull
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Process", weight = PROCESS), //
                 @Menu(label = "Interactively Create Convex Mesh", weight = PROCESS_INTERACTIVE_CONVEX_MESH) })
public class InteractiveConvexMesh extends InteractiveCommand {

    @Parameter
    private OpService opService;

    @Parameter
    private SciView sciView;

    @Parameter(callback = "createMesh")
    private Button createMesh;

    @Parameter(callback = "savePoints")
    private Button savePoints;

    @Parameter(callback = "loadPoints")
    private Button loadPoints;

    @Parameter(callback = "clearPoints")
    private Button clearPoints;

    private ControlPoints controlPoints;

    protected float controlPointDistance = 10;

    @Override
    public void run() {
        controlPoints = new ControlPoints();

        // This is where the command should change the current inputs setup
        sciView.stashControls();

        sciView.getSceneryInputHandler().addBehaviour( "place_control_point",
                placeControlPointBehaviour() );
        sciView.getSceneryInputHandler().addKeyBinding( "place_control_point", "double-click button1" );

        // TODO: Setup the scrolling behavior to adjust the control point distance

    }

    private Behaviour placeControlPointBehaviour() {
        ClickBehaviour b = new ClickBehaviour() {
            @Override
            public void click(int x, int y) {
                placeControlPoint( sciView.getCamera(), x, y, controlPointDistance );
            }
        };
        return b;
    }

    private void placeControlPoint(Camera camera, int x, int y, float distance) {
        GLVector target = camera.getTarget();
        GLVector position = camera.getPosition();

        GLVector view = target.minus(position).normalize();
        GLVector h = view.cross(camera.getUp()).normalize();
        GLVector v = h.cross(view);

        double fov = camera.getFov() * Math.PI / 180.0f;
        double lengthV = Math.tan(fov / 2.0) * camera.getNearPlaneDistance();
        double lengthH = lengthV * (camera.getWidth() / camera.getHeight());

        v = v.times((float) lengthV);
        h = h.times((float) lengthH);

        float posX = (x - camera.getWidth() / 2.0f) / (camera.getWidth() / 2.0f);
        float posY = -1.0f * (y - camera.getHeight() / 2.0f) / (camera.getHeight() / 2.0f);

        GLVector worldPos = position.plus(view.times(camera.getNearPlaneDistance())).plus(h.times(posX)).plus(v.times(posY));
        GLVector worldDir = (worldPos.minus(position)).getNormalized();

        Sphere controlPoint = new Sphere(ControlPoints.DEFAULT_RADIUS, ControlPoints.DEFAULT_SEGMENTS);
        Material mat = new Material();
        // TODO: Set to the control point color here
        controlPoint.setMaterial(mat);

        controlPoint.setPosition( worldPos.plus(worldDir.times(distance) ) );
        controlPoints.addPoint( controlPoint );
        sciView.addNode( controlPoint, false );
    }

    public void loadPoints() {
        // TODO: Load control points from a file.
    }

    public void savePoints() {
        // TODO: Save the current control points, perhaps as a point cloud xyz
    }

    public void clearPoints() {
        // TODO: Clear all curreent control points
    }

    /* Create a ConvexHulls of controlPoints */
    public void createMesh() {
        Mesh mesh = new NaiveDoubleMesh();

        for( Vector3 v : controlPoints.getVertices() ) {
            mesh.vertices().add(v.xf(), v.yf(), v.zf());
        }

        final List<?> result = (List<?>) opService.run(DefaultConvexHull3D.class, mesh );
        Mesh hull = (Mesh) result.get(0);

        sciView.addMesh(hull);

        sciView.restoreControls();
        // Remove all control points
        for( Node n : controlPoints.getNodes() ) {
            sciView.deleteNode(n,false);
        }
    }
}
