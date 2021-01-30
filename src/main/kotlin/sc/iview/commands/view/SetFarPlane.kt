package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SET_FAR_PLANE

/**
 * Command to set the far plane for the renderer. Everything beyond this **will not** be rendered
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Set Far Plane", weight = VIEW_SET_FAR_PLANE)])
class SetFarPlane : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private var farPlane = 1000f

    override fun run() {
        sciView.camera?.farPlaneDistance = farPlane
    }
}