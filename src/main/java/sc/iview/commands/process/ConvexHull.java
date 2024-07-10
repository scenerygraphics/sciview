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
package sc.iview.commands.process;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_CONVEX_HULL;

import graphics.scenery.Node;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;

import graphics.scenery.Mesh;
import sc.iview.process.MeshConverter;

/**
 * Command to create a convex hull of the currently open mesh
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
menu = {@Menu(label = "Process", weight = PROCESS), //
        @Menu(label = "Convex Hull", weight = PROCESS_CONVEX_HULL)})
public class ConvexHull implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Parameter
    private Node node;

    @Override
    public void run() {
        if( node instanceof Mesh ) {
            net.imglib2.mesh.Mesh ilMesh = MeshConverter.toImgLib((Mesh) node);

            net.imglib2.mesh.Mesh smoothMesh = ( net.imglib2.mesh.Mesh ) net.imglib2.mesh.alg.hull.ConvexHull.calculate( ilMesh );

            Node convexHull = sciView.addMesh(smoothMesh);
            convexHull.ifSpatial(spatial -> {
                spatial.setPosition(((Mesh) node).spatial().getPosition());
                return null;
            });
        }
    }

}
