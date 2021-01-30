package sc.iview.commands.view

import org.joml.Quaternionf
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_RESET_CAMERA_ROTATION

/**
 * Command to set the camera rotation to the default orientation.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Reset Camera Rotation", weight = VIEW_RESET_CAMERA_ROTATION)])
class ResetCameraRotation : Command {
    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.camera?.rotation = Quaternionf(0f, 0f, 0f, 1f)
    }
}