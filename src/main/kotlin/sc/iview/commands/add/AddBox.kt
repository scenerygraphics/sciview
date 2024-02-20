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

import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.display.DisplayService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB
import sc.iview.SciView
import sc.iview.commands.MenuWeights.ADD
import sc.iview.commands.MenuWeights.EDIT_ADD_BOX

/**
 * Command to add a box to the scene
 *
 * @author Kyle Harrington
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = ADD), Menu(
        label = "Box...",
        weight = EDIT_ADD_BOX
    )]
)
class AddBox : Command {
    @Parameter
    private lateinit var displayService: DisplayService

    @Parameter
    private lateinit var sciView: SciView

    // FIXME
    //    @Parameter
    //    private String position = "0; 0; 0";
    @Parameter
    private var size = 1.0f

    @Parameter(required = false)
    private lateinit var color: ColorRGB

    @Parameter
    private var inside: Boolean = false

    override fun run() {
        //final Vector3 pos = ClearGLVector3.parse( position );
        val pos = Vector3f(0f, 0f, 0f)
        val vSize = Vector3f(size, size, size)

        if( !this::color.isInitialized ) {
            color = SciView.DEFAULT_COLOR
        }

        sciView.addBox(pos, vSize, color, inside)
    }
}