package sc.iview.commands.edit.add

import graphics.scenery.Protein
import graphics.scenery.RibbonDiagram
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights

/**
 * Command to add a box to the scene
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Edit", weight = MenuWeights.EDIT), Menu(label = "Add", weight = MenuWeights.EDIT_ADD), Menu(label = "Protein from PDB ...", weight = MenuWeights.EDIT_ADD_BOX)])
class AddProtein : Command {

    @Parameter
    private lateinit var sciView: SciView

    @Parameter(label = "Protein")
    private var protein: String = "2rnm"

    override fun run() {
        val ribbon = RibbonDiagram(Protein.fromID(protein))
        ribbon.name = protein
        ribbon.scale = Vector3f(0.1f)
        sciView.addNode(ribbon, true)
    }
}