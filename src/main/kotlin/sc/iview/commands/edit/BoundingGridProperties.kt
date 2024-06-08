package sc.iview.commands.edit

import graphics.scenery.BoundingGrid
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB

@Plugin(type = Command::class, initializer = "initValues", visible = false)
class BoundingGridProperties : InspectorInteractiveCommand() {
    @Parameter(label = "Grid Color", callback = "updateNodeProperties", style = "group:Grid")
    private var gridColor: ColorRGB? = null

    @Parameter(label = "Ticks only", callback = "updateNodeProperties", style = "group:Grid")
    private var ticksOnly = false

    override fun updateCommandFields() {
        val node = currentSceneNode as? BoundingGrid ?: return

        fieldsUpdating = true

        gridColor = ColorRGB(node.gridColor.x().toInt() * 255,
                             node.gridColor.y().toInt() * 255,
                             node.gridColor.z().toInt() * 255)
        ticksOnly = node.ticksOnly > 0

        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? BoundingGrid ?: return
        if(fieldsUpdating) {
            return
        }

        val ticks = if (ticksOnly) {
            1
        } else {
            0
        }
        node.ticksOnly = ticks
        node.gridColor = Vector3f(
            gridColor!!.red / 255.0f,
            gridColor!!.green / 255.0f,
            gridColor!!.blue / 255.0f
        )    }
}