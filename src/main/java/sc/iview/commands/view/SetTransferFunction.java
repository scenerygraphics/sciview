package sc.iview.commands.view;

import graphics.scenery.Node;
import graphics.scenery.volumes.TransferFunction;
import graphics.scenery.volumes.Volume;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.NumberWidget;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_SET_TRANSFER_FUNCTION;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = {@Menu(label = "View", weight = VIEW), //
                @Menu(label = "Set Transfer Function", weight = VIEW_SET_TRANSFER_FUNCTION)})
public class SetTransferFunction extends InteractiveCommand {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Parameter(label = "TF Ramp Min", style = NumberWidget.SLIDER_STYLE, //
            min = "0", max = "1.0", stepSize = "0.001", callback = "updateTransferFunction")
    private float rampMin = 0;

    @Parameter(label = "TF Ramp Max", style = NumberWidget.SLIDER_STYLE, //
            min = "0", max = "1.0", stepSize = "0.001", callback = "updateTransferFunction")
    private float rampMax = 1.0f;

    @Parameter(label = "Target Volume")
    private Volume volume;

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    @Override
    public void cancel() {

    }

    /**
     * Nothing is done here, as the refreshing of the objects properties works via
     * callback methods.
     */
    @Override
    public void run() {

    }

    protected void updateTransferFunction() {
        TransferFunction tf = volume.getTransferFunction();
        //float currentOffset = tf.getControlPoint$scenery(1).getValue();
        //float currentFactor = tf.getControlPoint$scenery(2).getFactor();
        tf.clear();
        tf.addControlPoint(0.0f, 0.0f);
        tf.addControlPoint(rampMin, 0.0f);
        tf.addControlPoint(1.0f, rampMax);
    }
}