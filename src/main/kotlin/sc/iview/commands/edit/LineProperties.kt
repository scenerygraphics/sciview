package sc.iview.commands.edit

import graphics.scenery.primitives.Line
import okio.withLock
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

/**
 * Inspector panel for [Line] nodes.
 */
@Plugin(type = Command::class, initializer = "updateCommandFields", visible = false)
class LineProperties : InspectorInteractiveCommand() {
    /* Line properties */

    @Parameter(label = "Edge width", callback = "updateNodeProperties", style = "group:Line")
    private var edgeWidth = 0

    /** Updates this command fields with the node's current properties. */
    override fun updateCommandFields() {
        val node = currentSceneNode as? Line ?: return

        fieldsUpdating.withLock {
            edgeWidth = node.edgeWidth.toInt()
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? Line ?: return
        fieldsUpdating.withLock {
            node.edgeWidth = edgeWidth.toFloat()
        }
    }

}