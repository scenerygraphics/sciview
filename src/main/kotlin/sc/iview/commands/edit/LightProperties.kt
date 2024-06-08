package sc.iview.commands.edit

import graphics.scenery.PointLight
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.NumberWidget

@Plugin(type = Command::class, initializer = "initValues", visible = false)
class LightProperties : InspectorInteractiveCommand() {
    /* Light properties */

    @Parameter(label = "Intensity", style = NumberWidget.SPINNER_STYLE+ ",group:Lighting", stepSize = "0.1", callback = "updateNodeProperties")
    private var intensity = 0f

    override fun updateCommandFields() {
        val node = currentSceneNode as? PointLight ?: return

        fieldsUpdating = true
        intensity = node.intensity
        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? PointLight ?: return
        if(fieldsUpdating) {
            return
        }

        node.intensity = intensity
    }

}