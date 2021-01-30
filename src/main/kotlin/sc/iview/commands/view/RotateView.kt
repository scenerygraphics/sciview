package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_ROTATE

/**
 * Command to circle the camera around the currently active Node
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Circle camera around current object", weight = VIEW_ROTATE)])
class RotateView : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private var xSpeed = 3

    @Parameter
    private var ySpeed = 0

    override fun run() {
        sciView.animate(30) {
            sciView.targetArcball.init(1, 1)
            sciView.targetArcball.drag(1 + xSpeed, 1 + ySpeed)
            sciView.targetArcball.end(1 + xSpeed, 1 + ySpeed)
        }
    }
}