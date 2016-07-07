package sc.fiji.display;

import net.imagej.Data;
import net.imagej.PositionableByAxis;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DataView;
import net.imagej.interval.CalibratedRealInterval;
import net.imglib2.Interval;

import org.scijava.display.Display;
import org.scijava.util.RealRect;

public interface ThreeDDisplay extends Display<DataView>,
	CalibratedRealInterval<CalibratedAxis>, PositionableByAxis, Interval
{

	/** Pop-up context menu root for image displays. */
	String CONTEXT_MENU_ROOT = "context-ImageDisplay";

	/** Gets the view currently designated as active. */
	DataView getActiveView();

	/** Gets the axis currently designated as active. */
	AxisType getActiveAxis();

	/** Sets the axis currently designated as active. */
	void setActiveAxis(AxisType axis);

	/**
	 * Tests whether the given view should currently be visible in this display. A
	 * view is visible when the current position falls within the bounds of the
	 * view's space, including constant-value dimensions beyond the view's linked
	 * data's space.
	 */
	boolean isVisible(DataView view);
	
	ThreeDCanvas getCanvas();
	
	/**
	 * Gets a rectangle defining the extents of the image in the current X/Y
	 * plane.
	 */
	RealRect getPlaneExtents();

}
