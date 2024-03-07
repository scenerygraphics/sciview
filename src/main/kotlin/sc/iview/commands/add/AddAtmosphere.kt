package sc.iview.commands.add

import graphics.scenery.primitives.Atmosphere
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.NumberWidget
import sc.iview.SciView
import sc.iview.commands.MenuWeights

/**
 * Command to add an atmosphere background to the scene
 *
 * @author Samuel Pantze
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = MenuWeights.ADD), Menu(
        label = "Atmosphere...",
        weight = MenuWeights.EDIT_ADD_ATMOSPHERE
    )]
)
class AddAtmosphere : Command {
    @Parameter
    private lateinit var sciView: SciView

    private val atmos = Atmosphere()

    private fun runAttachControls() {
        sciView.sceneryInputHandler?.let { atmos.attachRotateBehaviours(it) }
    }

    override fun run() {
        sciView.addNode(atmos)
    }
}