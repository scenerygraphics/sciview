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
package sc.iview.commands.add

import graphics.scenery.DetachedHeadCamera
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.ADD
import sc.iview.commands.MenuWeights.EDIT_ADD_CAMERA
import kotlin.math.max
import kotlin.math.min

/**
 * Command to add a camera to the scene
 *
 * @author Kyle Harrington
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = ADD),Menu(
        label = "Camera...",
        weight = EDIT_ADD_CAMERA
    )]
)
class AddCamera : Command {
    @Parameter
    private lateinit var sciView: SciView

    // FIXME
    //	@Parameter
    //	private String position = "0; 0; 0";
    @Parameter(label = "Field of View")
    private var fov = 50.0f

    @Parameter(label = "Near plane")
    private var nearPlane = 0.1f

    @Parameter(label = "farPlane")
    private var farPlane = 500.0f
    override fun run() {
        //final Vector3 pos = ClearGLVector3.parse( position );
        val pos = Vector3f(0.0f)
        val cam = DetachedHeadCamera()
        cam.perspectiveCamera(
            fov,
            sciView!!.windowWidth,
            sciView.windowHeight,
            min(nearPlane, farPlane),
            max(nearPlane, farPlane)
        )
        cam.ifSpatial {
            position = pos
        }
        sciView.addNode(cam)
    }
}
