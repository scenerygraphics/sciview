package sc.iview.commands.view

import graphics.scenery.Node
import net.imagej.lut.LUTService
import net.imglib2.display.ColorTable
import org.scijava.command.Command
import org.scijava.command.DynamicCommand
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SET_LUT
import java.io.IOException

/**
 * Command to set the currently used Look Up Table (LUT). This is a colormap for the volume.
 *
 * @author Kyle Harrington
 */
@Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Set LUT", weight = VIEW_SET_LUT)])
class SetLUT : DynamicCommand() {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var lutService: LUTService

    @Parameter(label = "Node")
    private lateinit var node: Node

    @Parameter(label = "Selected LUT", choices = [], callback = "lutNameChanged")
    private lateinit var lutName: String

    @Parameter(label = "LUT Selection")
    private lateinit var colorTable: ColorTable

    protected fun lutNameChanged() {
        val lutNameItem = info.getMutableInput("lutName", String::class.java)
        try {
            colorTable = lutService.loadLUT(lutService.findLUTs()[lutNameItem])
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun initialize() {
        try {
            colorTable = lutService.loadLUT(lutService.findLUTs()["Red.lut"])
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val lutNameItem = info.getMutableInput("lutName", String::class.java)
        lutNameItem.choices = ArrayList(lutService.findLUTs().keys)
    }

    override fun run() {
        sciView.setColormap(node, colorTable)
    }
}