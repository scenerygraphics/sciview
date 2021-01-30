package sc.iview.commands.view

import graphics.scenery.volumes.Volume
import org.scijava.command.Command
import org.scijava.command.InteractiveCommand
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.NumberWidget
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SET_TRANSFER_FUNCTION

/**
 * Command to set the transfer function of a Volume
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Set Transfer Function", weight = VIEW_SET_TRANSFER_FUNCTION)])
class SetTransferFunction : InteractiveCommand() {
    @Parameter(label = "Target Volume")
    private lateinit var volume: Volume

    @Parameter(label = "TF Ramp Min", style = NumberWidget.SLIDER_STYLE, min = "0", max = "1.0", stepSize = "0.001", callback = "updateTransferFunction")
    private var rampMin = 0f

    @Parameter(label = "TF Ramp Max", style = NumberWidget.SLIDER_STYLE, min = "0", max = "1.0", stepSize = "0.001", callback = "updateTransferFunction")
    private var rampMax = 1.0f

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    override fun cancel() {}

    /**
     * Nothing is done here, as the refreshing of the objects properties works via
     * callback methods.
     */
    override fun run() {}
    protected fun updateTransferFunction() {
        val tf = volume.transferFunction
        //float currentOffset = tf.getControlPoint$scenery(1).getValue();
        //float currentFactor = tf.getControlPoint$scenery(2).getFactor();
        tf.clear()
        tf.addControlPoint(0.0f, 0.0f)
        tf.addControlPoint(rampMin, 0.0f)
        tf.addControlPoint(1.0f, rampMax)
    }
}