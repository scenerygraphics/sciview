package sc.iview.commands.edit

import graphics.scenery.BoundingGrid
import okio.withLock
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB

/**
 * Inspector panel for inspecting a [BoundingGrid]'s properties.
 */
@Plugin(type = Command::class, initializer = "updateCommandFields", visible = false)
class BoundingGridProperties : InspectorInteractiveCommand() {
    /** Parameter for the [BoundingGrid.gridColor] */
    @Parameter(label = "Grid Color", callback = "updateNodeProperties", style = "group:Grid")
    private var gridColor: ColorRGB? = null

    /** Parameter for the [BoundingGrid.ticksOnly], determining whether to show a box or only ticks. */
    @Parameter(label = "Ticks only", callback = "updateNodeProperties", style = "group:Grid")
    private var ticksOnly = false

    /** Updates this command fields with the node's current properties. */
    override fun updateCommandFields() {
        val node = currentSceneNode as? BoundingGrid ?: return

        fieldsUpdating.withLock {
            gridColor = ColorRGB(
                node.gridColor.x().toInt() * 255,
                node.gridColor.y().toInt() * 255,
                node.gridColor.z().toInt() * 255
            )
            ticksOnly = node.ticksOnly > 0
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? BoundingGrid ?: return
        fieldsUpdating.withLock {

            val ticks = if(ticksOnly) {
                1
            } else {
                0
            }
            node.ticksOnly = ticks
            node.gridColor = Vector3f(
                gridColor!!.red / 255.0f,
                gridColor!!.green / 255.0f,
                gridColor!!.blue / 255.0f
            )
        }
    }
}