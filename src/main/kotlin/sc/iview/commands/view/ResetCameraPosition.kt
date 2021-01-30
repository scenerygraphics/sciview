package sc.iview.commands.view

import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_RESET_CAMERA_POSITION

/**
 * Command to set the camera position to the default position
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Reset Camera Position", weight = VIEW_RESET_CAMERA_POSITION)])
class ResetCameraPosition : Command {
    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.camera?.position = Vector3f(0.0f, 1.65f, 5f)
    }
}