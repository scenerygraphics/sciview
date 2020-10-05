package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights

/**
 * Command to toggle scenery's PushMode. If this is true the scene only renders when it is changed, otherwise it is
 * continuously rendered
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = MenuWeights.VIEW), Menu(label = "Toggle Unlimited Framerate", weight = MenuWeights.VIEW_TOGGLE_UNLIMITED_FRAMERATE)])
class ToggleUnlimitedFramerate : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.pushMode = !sciView.pushMode
    }
}