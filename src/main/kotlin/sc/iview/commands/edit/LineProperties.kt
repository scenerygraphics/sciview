package sc.iview.commands.edit

import graphics.scenery.primitives.Line
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

@Plugin(type = Command::class, initializer = "initValues", visible = false)
class LineProperties : InspectorInteractiveCommand() {
    /* Line properties */

    @Parameter(label = "Edge width", callback = "updateNodeProperties", style = "group:Line")
    private var edgeWidth = 0

    override fun updateCommandFields() {
        val node = currentSceneNode as? Line ?: return

        fieldsUpdating = true
        edgeWidth = node.edgeWidth.toInt()
        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? Line ?: return
        if(fieldsUpdating) {
            return
        }
        node.edgeWidth = edgeWidth.toFloat()
    }

}