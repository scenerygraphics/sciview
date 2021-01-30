package sc.iview.commands.edit.add

import net.imagej.Dataset
import net.imagej.axis.DefaultAxisType
import net.imagej.axis.DefaultLinearAxis
import net.imagej.ops.OpService
import net.imagej.units.UnitService
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights

/**
 * Command to add a volume to the scene.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Edit", weight = MenuWeights.EDIT), Menu(label = "Add", weight = MenuWeights.EDIT_ADD), Menu(label = "Volume", weight = MenuWeights.EDIT_ADD_VOLUME)])
class AddVolume : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var unitService: UnitService

    @Parameter
    private lateinit var image: Dataset

    @Parameter(label = "Use voxel dimensions from image", callback = "setVoxelDimensions")
    private var inheritFromImage = true

    @Parameter(label = "Voxel Size X")
    private var voxelWidth = 1.0f

    @Parameter(label = "Voxel Size Y")
    private var voxelHeight = 1.0f

    @Parameter(label = "Voxel Size Z")
    private var voxelDepth = 1.0f

    override fun run() {
        if (inheritFromImage) {
            val n = sciView.addVolume(image)
            n?.name = image.name ?: "Volume"
        } else {
            val n = sciView.addVolume(image, floatArrayOf(voxelWidth, voxelHeight, voxelDepth))
            n?.name = image.name ?: "Volume"
        }
    }

    private fun setVoxelDimension() {
        val axis = arrayOf(
                DefaultLinearAxis(DefaultAxisType("X", true), "um", 1.0),
                DefaultLinearAxis(DefaultAxisType("Y", true), "um", 1.0),
                DefaultLinearAxis(DefaultAxisType("Z", true), "um", 1.0)
        )

        val voxelDims = FloatArray(minOf(image.numDimensions(), 3))

        for (d in voxelDims.indices) {
            val inValue = image.axis(d).averageScale(0.0, 1.0)
            if (image.axis(d).unit() == null) {
                voxelDims[d] = inValue.toFloat()
            } else {
                voxelDims[d] = unitService.value(inValue, image.axis(d).unit(), axis[d].unit()).toFloat()
            }
        }

        voxelWidth = voxelDims.getOrElse(0) { 1.0f }
        voxelHeight = voxelDims.getOrElse(1) { 1.0f }
        voxelDepth = voxelDims.getOrElse(2) { 1.0f }
    }
}