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
package sc.iview.ops;

import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciViewService;
import sc.iview.process.MeshConverter;

import cleargl.GLVector;
import graphics.scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "SciView>Mesh>Convex Hull")
public class ConvexHull implements Command {
	
	@Parameter
	private OpService ops;

	@Parameter
	private SciViewService sceneryService;

	@Parameter
	private LogService logService;
		
	@Override
	public void run() {
		if( sceneryService.getActiveSciView().getActiveNode() instanceof  Mesh ) {
			Mesh currentMesh = (Mesh)sceneryService.getActiveSciView().getActiveNode();
			DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh(currentMesh,logService);

			currentMesh.getMaterial().setDiffuse(new GLVector(1.0f, 0.0f, 0.0f));

			net.imagej.ops.geom.geom3d.mesh.Mesh smoothMesh = ops.geom().convexHull((net.imagej.ops.geom.geom3d.mesh.Mesh) opsMesh);

			sceneryService.getActiveSciView().addMesh(smoothMesh);
		}

	}

}
