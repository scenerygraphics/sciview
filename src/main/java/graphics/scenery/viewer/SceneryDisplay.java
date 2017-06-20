package graphics.scenery.viewer;

import net.imagej.PositionableByAxis;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DataView;
import net.imagej.interval.CalibratedRealInterval;
import net.imglib2.Interval;
import org.scijava.display.Display;

/**
 * Created by kharrington on 6/16/17.
 */
public interface SceneryDisplay extends Display<DataView>,
        CalibratedRealInterval<CalibratedAxis>, PositionableByAxis, Interval {
    /**
     * Tests whether the given view should currently be visible in this display. A
     * view is visible when the current position falls within the bounds of the
     * view's space, including constant-value dimensions beyond the view's linked
     * data's space.
     */
    boolean isVisible(DataView view);

    DataView getActiveView();
}
