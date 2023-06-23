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
package sc.iview.commands.add

import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.MenuWeights.EDIT_ADD_LINE

/**
 * Command to add a line in the scene
 *
 * @author Kyle Harrington
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = MenuWeights.ADD), Menu(
        label = "Line...",
        weight = EDIT_ADD_LINE
    )]
)
class AddLine : Command {
    @Parameter
    private lateinit var sciView: SciView

    // FIXME
    //    @Parameter(label = "First endpoint")
    //    private String start = "0; 0; 0";
    //
    //    @Parameter(label = "Second endpoint")
    //    private String stop = "1; 1; 1";
    @Parameter(required = false)
    private lateinit var color: ColorRGB

    @Parameter(label = "Edge width", min = "0")
    private var edgeWidth = 1.0

    override fun run() {
        //Vector3[] endpoints = { JOMLVector3.parse( start ), JOMLVector3.parse( stop ) };
        val endpoints = arrayOf(Vector3f(0f, 0f, 0f), Vector3f(1f, 1f, 1f))
        if( !this::color.isInitialized ) {
            color = SciView.DEFAULT_COLOR
        }

        sciView.addLine(endpoints, color, edgeWidth)
    }
}