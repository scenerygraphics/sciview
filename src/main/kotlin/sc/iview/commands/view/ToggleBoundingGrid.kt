package sc.iview.commands.view

import graphics.scenery.BoundingGrid
import graphics.scenery.Mesh
import graphics.scenery.Node
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_TOGGLE_BOUNDING_GRID

/**
 * Command to toggle the bounding grid around a Node
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Toggle Bounding Grid", weight = VIEW_TOGGLE_BOUNDING_GRID)])
class ToggleBoundingGrid : Command {
    @Parameter
    private lateinit var logService: LogService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var node: Node

    override fun run() {
        if (node is Mesh) {
            if (node.metadata.containsKey("BoundingGrid")) {
                val bg = node.metadata["BoundingGrid"] as BoundingGrid?
                bg!!.node = null
                node.metadata.remove("BoundingGrid")
                bg.getScene()!!.removeChild(bg)
            } else {
                val bg = BoundingGrid()
                bg.node = node
                node.metadata["BoundingGrid"] = bg
            }
        }
    }
}