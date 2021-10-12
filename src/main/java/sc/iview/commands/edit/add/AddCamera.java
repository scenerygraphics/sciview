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
package sc.iview.commands.edit.add;

import graphics.scenery.DetachedHeadCamera;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.*;

/**
 * Command to add a camera to the scene
 *
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
		menu = { @Menu(label = "Edit", weight = EDIT), //
				 @Menu(label = "Add", weight = EDIT_ADD), //
				 @Menu(label = "Camera...", weight = EDIT_ADD_CAMERA) })
public class AddCamera implements Command {

	@Parameter
	private DisplayService displayService;

	@Parameter
	private SciView sciView;

	// FIXME
//	@Parameter
//	private String position = "0; 0; 0";

	@Parameter(label = "Field of View")
	private float fov = 50.0f;

	@Parameter(label = "Near plane")
	private float nearPlane = 0.1f;

	@Parameter(label = "farPlane")
	private float farPlane = 500.0f;

	@Override
	public void run() {
		//final Vector3 pos = ClearGLVector3.parse( position );
		final Vector3f pos = new Vector3f(0, 0, 0);
		final DetachedHeadCamera cam = new DetachedHeadCamera();
		cam.perspectiveCamera( fov, sciView.getWindowWidth(), sciView.getWindowHeight(), Math.min(nearPlane, farPlane), Math.max(nearPlane, farPlane)  );
		cam.ifSpatial(spatial -> {
			spatial.setPosition( pos );
			return null;
		});

		sciView.addNode( cam );
	}
}
