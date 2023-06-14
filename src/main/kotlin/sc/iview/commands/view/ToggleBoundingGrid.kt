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
package sc.iview.commands.view

import graphics.scenery.BoundingGrid
import graphics.scenery.Node
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_TOGGLE_BOUNDING_GRID

/**
 * Command to toggle the bounding grid around a Node
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Toggle Bounding Grid", weight = VIEW_TOGGLE_BOUNDING_GRID)])
class ToggleBoundingGrid : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var node: Node

    @Parameter(label = "Apply to children")
    private var applyToChildren: Boolean = true

    fun toggleBoundingGrid(node: Node) {
        val bg = node.children.findLast { it is BoundingGrid } as? BoundingGrid
        if (bg != null) {
            bg.node = null
            sciView.deleteNode(bg)
        } else {
            val newBg = BoundingGrid()
            newBg.node = node
            sciView.publishNode(newBg)
        }
    }

    override fun run() {
        if (applyToChildren) {
            node.runRecursive({ node -> toggleBoundingGrid(node)})
        } else {
            toggleBoundingGrid(node)
        }
    }
}