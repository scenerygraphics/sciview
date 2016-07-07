package sc.fiji.display;

import org.scijava.input.MouseCursor;
import org.scijava.util.IntCoords;
import org.scijava.util.RealCoords;

import net.imagej.display.Pannable;
import net.imagej.display.Zoomable;

public interface ThreeDCanvas extends Pannable, Zoomable {

	/** Gets the canvas's display. */
	ThreeDDisplay getDisplay();

	/**
	 * Gets the current width of the canvas viewport in <em>panel</em>
	 * coordinates.
	 */
	int getViewportWidth();

	/**
	 * Gets the current height of the canvas viewport in <em>panel</em>
	 * coordinates.
	 */
	int getViewportHeight();

	/** Sets the dimensions of the viewport in <em>panel</em> coordinates. */
	void setViewportSize(int width, int height);

	/**
	 * Tests whether a given point in the panel falls within the boundaries of the
	 * display space.
	 * 
	 * @param point The point to check, in <em>panel</em> coordinates.
	 */
	boolean isInImage(IntCoords point);

	/**
	 * Converts the given <em>panel</em> coordinates into <em>data</em>
	 * coordinates.
	 */
	RealCoords panelToDataCoords(IntCoords panelCoords);

	/**
	 * Converts the given <em>data</em> coordinates into <em>panel</em>
	 * coordinates.
	 */
	IntCoords dataToPanelCoords(RealCoords dataCoords);

	/** Gets the current mouse cursor. */
	MouseCursor getCursor();

	/** Sets the mouse to the given {@link MouseCursor} type. */
	void setCursor(MouseCursor cursor);

}
