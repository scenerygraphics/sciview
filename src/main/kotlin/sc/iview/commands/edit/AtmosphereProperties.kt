package sc.iview.commands.edit

import graphics.scenery.primitives.Atmosphere
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.NumberWidget

@Plugin(type = Command::class, initializer = "initValues", visible = false)
class AtmosphereProperties : InspectorInteractiveCommand() {

    @Parameter(label = "Latitude", style = NumberWidget.SPINNER_STYLE+"group:Atmosphere"+",format:0.0", min = "-90", max = "90", stepSize = "1", callback = "updateNodeProperties")
    private var atmosphereLatitude = 50f

    @Parameter(label = "Enable keybindings and manual control", style = "group:Atmosphere", callback = "updateNodeProperties", description = "Use key bindings for controlling the sun.\nCtrl + Arrow Keys = large increments.\nCtrl + Shift + Arrow keys = small increments.")
    private var isSunManual = false

    @Parameter(label = "Sun Azimuth", style = "group:Atmosphere" + ",format:0.0", callback = "updateNodeProperties", description = "Azimuth value of the sun in degrees", min = "0", max = "360", stepSize = "1")
    private var sunAzimuth = 180f

    @Parameter(label = "Sun Elevation", style = "group:Atmosphere" + ",format:0.0", callback = "updateNodeProperties", description = "Elevation value of the sun in degrees", min = "-90", max = "90", stepSize = "1")
    private var sunElevation = 45f

    @Parameter(label = "Emission Strength", style = NumberWidget.SPINNER_STYLE+"group:Atmosphere"+",format:0.00", min = "0", max="10", stepSize = "0.1", callback = "updateNodeProperties")
    private var atmosphereStrength = 1f


    override fun updateCommandFields() {
        val node = currentSceneNode as? Atmosphere ?: return

        fieldsUpdating = true

        isSunManual = !node.isSunAnimated
        atmosphereLatitude = node.latitude
        atmosphereStrength = node.emissionStrength
        sunAzimuth = node.azimuth
        sunElevation = node.elevation

        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? Atmosphere ?: return
        if(fieldsUpdating) {
            return
        }

        node.latitude = atmosphereLatitude
        node.emissionStrength = atmosphereStrength
        // attach/detach methods also handle the update of node.updateControls
        if (isSunManual) {
            sciView.sceneryInputHandler?.let { node.attachBehaviors(it) }
            node.setSunPosition(sunElevation, sunAzimuth)
        } else {
            sciView.sceneryInputHandler?.let { node.detachBehaviors(it) }
            // Update the sun position immediately
            node.setSunPositionFromTime()
        }
    }

}