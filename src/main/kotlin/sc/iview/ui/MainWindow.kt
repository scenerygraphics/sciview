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
package sc.iview.ui

import graphics.scenery.BoundingGrid
import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.PointLight
import graphics.scenery.primitives.Atmosphere
import graphics.scenery.primitives.Line
import graphics.scenery.primitives.TextBoard
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
import sc.iview.commands.edit.*
import java.awt.Panel

/**
 * Interface for sciview main windows.
 *
 * @author Ulrik Guenther
 */
abstract class MainWindow {
    var nodeSpecificPropertyPanels: ArrayList<InspectorInteractiveCommand.UsageCondition> = arrayListOf()
        protected set

    init {

        val nodeSpecificPropertyMappings = hashMapOf(
            PointLight::class.java to LightProperties::class.java,
            Camera::class.java to CameraProperties::class.java,
            BoundingGrid::class.java to BoundingGridProperties::class.java,
            Line::class.java to LineProperties::class.java,
            SlicingPlane::class.java to SlicingPlaneProperties::class.java,
            TextBoard::class.java to TextBoardProperties::class.java,
            Atmosphere::class.java to AtmosphereProperties::class.java,
            Volume::class.java to VolumeProperties::class.java,
        )

        nodeSpecificPropertyMappings.forEach { (nodeClass, propertyClass) ->
            nodeSpecificPropertyPanels += InspectorInteractiveCommand.UsageCondition(
                { sceneNode -> nodeClass.isAssignableFrom(sceneNode.javaClass) }, propertyClass
            )
        }

    }

    /**
     * Adds a new custom panel to the inspector, with a usage [condition] given (e.g., checking for a
     * specific Node type, and the [panelClass] that represents the custom panel.
     */
    fun addNodeSpecificInspectorPanel(condition: (Node) -> Boolean, panelClass: Class<out InspectorInteractiveCommand>) {
        nodeSpecificPropertyPanels += InspectorInteractiveCommand.UsageCondition(
            condition, panelClass
        )
    }

    /**
     * Initializer for the REPL.
     */
    abstract fun initializeInterpreter()

    /**
     * Toggling the sidebar for inspector, REPL, etc, returns the new state, where true means visible.
     */
    abstract fun toggleSidebar(): Boolean

    /**
     * Shows a context menu in the rendering window at [x], [y].
     */
    abstract fun showContextNodeChooser(x: Int, y: Int)

    /**
     * Signal to select a specific [node].
     */
    abstract fun selectNode(node: Node?)

    /**
     * Signal to rebuild scene tree in the UI.
     */
    abstract fun rebuildSceneTree()

    /**
     * Closes the main window.
     */
    abstract fun close()
}
