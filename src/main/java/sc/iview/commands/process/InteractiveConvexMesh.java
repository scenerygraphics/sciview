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
package sc.iview.commands.process;

import graphics.scenery.Node;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultConvexHull3D;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.iview.SciView;
import sc.iview.process.ControlPoints;

import java.util.List;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_INTERACTIVE_CONVEX_MESH;

/**
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

    private Node targetPoint;
    private ControlPoints controlPoints;

    protected float controlPointDistance = 10;

    @Override
    public void run() {
        controlPoints = new ControlPoints();

        controlPoints.initializeSciView(sciView, controlPointDistance);
    }

    /* Create a ConvexHulls of controlPoints */
    public void createMesh() {
        Mesh mesh = new NaiveDoubleMesh();

        for( Vector3f v : controlPoints.getVertices() ) {
            mesh.vertices().add(v.x(), v.y(), v.z());
        }

        final List<?> result = (List<?>) opService.run(DefaultConvexHull3D.class, mesh );
        Mesh hull = (Mesh) result.get(0);

        sciView.addMesh(hull);

        controlPoints.cleanup(sciView);
    }

    @Override
    public void cancel() {
        controlPoints.cleanup(sciView);
    }
}
