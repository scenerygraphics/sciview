package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_STOP_ANIMATION

/**
 * Command to stop all current animations
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Stop Animation", weight = VIEW_STOP_ANIMATION)])
class StopAnimation : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.stopAnimation()
    }
}