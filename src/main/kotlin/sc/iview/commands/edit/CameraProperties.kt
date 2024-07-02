package sc.iview.commands.edit

import graphics.scenery.Camera
import okio.withLock
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

/**
 * Inspector panel for [Camera] nodes.
 */
@Plugin(type = Command::class, initializer = "updateCommandFields", visible = false)
class CameraProperties : InspectorInteractiveCommand() {
    @Parameter(label = "Active", required = false, callback = "updateNodeProperties", style = "group:Camera")
    private var active = false

    /** Updates this command fields with the node's current properties. */
    override fun updateCommandFields() {
        val node = currentSceneNode as? Camera ?: return

        fieldsUpdating.withLock {
            val scene = node.getScene()
            active = if(scene != null) {
                scene.findObserver() === node
            } else {
                false
            }
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? Camera ?: return
        fieldsUpdating.withLock {

            val scene = node.getScene()
            if(active && scene != null) {
                scene.activeObserver = node
            }
        }
    }

}