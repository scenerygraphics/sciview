package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SET_SUPERSAMPLING_FACTOR

/**
 * Command to set scenery's Supersampling Factor
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Set Supersampling Factor", weight = VIEW_SET_SUPERSAMPLING_FACTOR)])
class SetSupersamplingFactor : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private var supersamplingFactor = 1.0

    override fun run() {
        sciView.getSceneryRenderer()?.settings?.set("Renderer.SupersamplingFactor", supersamplingFactor)
    }
}