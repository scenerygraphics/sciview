package sc.iview.commands.edit

import graphics.scenery.PointLight
import okio.withLock
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.NumberWidget

/**
 * Inspector panel for [PointLight]s.
 */
@Plugin(type = Command::class, initializer = "updateCommandFields", visible = false)
class LightProperties : InspectorInteractiveCommand() {
    @Parameter(label = "Intensity", style = NumberWidget.SPINNER_STYLE+ ",group:Lighting", stepSize = "0.1", callback = "updateNodeProperties")
    private var intensity = 0f

    /** Updates this command fields with the node's current properties. */
    override fun updateCommandFields() {
        val node = currentSceneNode as? PointLight ?: return

        fieldsUpdating.withLock {
            intensity = node.intensity
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? PointLight ?: return
        fieldsUpdating.withLock {
            node.intensity = intensity
        }
    }

}