package sc.iview.commands.view

import net.imagej.mesh.Mesh
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_CENTER_ON_ACTIVE_NODE

/**
 * Command to center the camera on the currently active Node
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Center On Active Node", weight = VIEW_CENTER_ON_ACTIVE_NODE)])
class CenterOnActiveNode : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        if (sciView.activeNode is Mesh) {
            val currentNode = sciView.activeNode
            sciView.centerOnNode(currentNode)
        }
    }
}