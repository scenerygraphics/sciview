package sc.iview.commands.edit

import graphics.scenery.Camera
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin

@Plugin(type = Command::class, initializer = "initValues", visible = false)
class CameraProperties : InspectorInteractiveCommand() {
    @Parameter(label = "Active", required = false, callback = "updateNodeProperties", style = "group:Camera")
    private var active = false

    override fun updateCommandFields() {
        val node = currentSceneNode as? Camera ?: return
        fieldsUpdating = true

        val scene = node.getScene()
        active = if (scene != null) {
            scene.findObserver() === node
        } else {
            false
        }

        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? Camera ?: return
        if(fieldsUpdating) {
            return
        }

        val scene = node.getScene()
        if (active && scene != null) {
            scene.activeObserver = node
        }
    }

}