package sc.fiji.ui.swing.viewer.threed;


import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DataView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.event.DelayedPositionEvent;
import net.imagej.display.event.PanZoomEvent;
import net.imagej.event.DatasetRestructuredEvent;
import net.imagej.event.DatasetUpdatedEvent;
import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCellImg;
import sc.fiji.display.ThreeDDisplay;
import sc.fiji.display.ThreeDDisplayService;

import org.scijava.display.Display;
import org.scijava.display.event.DisplayUpdatedEvent;
import org.scijava.display.event.window.WinActivatedEvent;
import org.scijava.event.EventHandler;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.tool.ToolService;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.util.UnitUtils;

public abstract class AbstractThreeDDisplayViewer extends
	AbstractDisplayViewer<DataView> implements ThreeDDisplayViewer
{

	protected enum ZoomScaleOption {
		OPTIONS_PERCENT_SCALE, OPTIONS_FRACTIONAL_SCALE
	}

	@Parameter
	private ThreeDDisplayService threeDDisplayService;

	@Parameter
	private ToolService toolService;

	@Parameter
	private OptionsService optionsService;

	private ThreeDDisplay display;

	// -- ImageDisplayViewer methods --

	@Override
	public ThreeDDisplay getDisplay() {
		return display;
	}

	// -- DisplayViewer methods --

	@Override
	public boolean canView(final Display<?> d) {
		return d instanceof ImageDisplay;
	}

	@Override
	public void view(final DisplayWindow w, final Display<?> d) {
		super.view(w, d);
		display = (ThreeDDisplay) d;
	}

	// -- Internal AbstractImageDisplayViewer methods --

	protected Dataset getDataset(final DataView view) {
		final Data data = view.getData();
		return data instanceof Dataset ? (Dataset) data : null;
	}

	/**
	 * Recalculate the label text and update it on the panel.
	 */
	protected void updateLabel() {
		if (getDisplay().getActiveView() != null) {
			getPanel().setLabel(makeLabel());
		}
	}

	/**
	 * Implement this in the derived class to get the user's preference for
	 * displaying zoom scale (as a fraction or percent)
	 * 
	 * @return {@link ZoomScaleOption#OPTIONS_PERCENT_SCALE} or
	 *         {@link ZoomScaleOption#OPTIONS_FRACTIONAL_SCALE}
	 */
	protected ZoomScaleOption getZoomScaleOption() {
		//FIXME
//		return optionsService.getOptions(OptionsAppearance.class)
//			.isDisplayFractionalScales() ? ZoomScaleOption.OPTIONS_FRACTIONAL_SCALE
//			: ZoomScaleOption.OPTIONS_PERCENT_SCALE;
		return ZoomScaleOption.OPTIONS_PERCENT_SCALE;
	}

	// -- Internal AbstractDisplayViewer methods --

	@Override
	protected void updateTitle() {
		String trailer = "";
		final Dataset ds = threeDDisplayService.getActiveDataset(display);
		if (ds != null) {
			final Img<?> img = ds.getImgPlus().getImg();
			if (AbstractCellImg.class.isAssignableFrom(img.getClass())) {
				trailer = " (V)";
			}
		}
		String name = getDisplay().getName();
		if (name == null) name = "";
		getWindow().setTitle(name + trailer);
	}

	// -- Helper methods --

	/** Makes some informative label text by inspecting the views. */
	private String makeLabel() {
		// CTR TODO - Fix window label to show beyond just the active view.
		final DataView view = getDisplay().getActiveView();
		final Dataset dataset = getDataset(view);

		final int xIndex = dataset.dimensionIndex(Axes.X);
		final int yIndex = dataset.dimensionIndex(Axes.Y);
		final Position pos = view.getPlanePosition();

		final StringBuilder sb = new StringBuilder();
		for (int i = 0, p = -1; i < dataset.numDimensions(); i++) {
			long dim = dataset.dimension(i);
			final AxisType axis = dataset.axis(i).type();
			if (axis.isXY()) continue;
			p++;
			if (dim == 1) continue;
			sb.append(axis);
			sb.append(": ");
			sb.append(pos.getLongPosition(p) + 1);
			sb.append("/");
			sb.append(dim);
			sb.append("; ");
		}

		sb.append(dataset.dimension(xIndex));
		sb.append("x");
		sb.append(dataset.dimension(yIndex));
		sb.append("; ");

		sb.append(dataset.getTypeLabelLong());
		sb.append("; ");

		sb.append(byteInfoString(dataset));
		sb.append("; ");

		final double zoomFactor = getDisplay().getCanvas().getZoomFactor();
		if (zoomFactor != 1) {
			sb.append("(");
			sb.append(getScaleConverter().getString(zoomFactor));
			sb.append(")");
		}

		return sb.toString();
	}

	private String byteInfoString(final Dataset ds) {
		final double byteCount = ds.getBytesOfInfo();
		return UnitUtils.getAbbreviatedByteLabel(byteCount);
	}

	private ScaleConverter getScaleConverter() {

		if (getZoomScaleOption().equals(ZoomScaleOption.OPTIONS_FRACTIONAL_SCALE)) {
			return new FractionalScaleConverter();
		}

		return new PercentScaleConverter();
	}

	// -- Helper classes --

	private interface ScaleConverter {

		String getString(double realScale);
	}

	private class PercentScaleConverter implements ScaleConverter {

		@Override
		public String getString(final double realScale) {
			return String.format("%.2f%%", realScale * 100);
		}

	}

	private class FractionalScaleConverter implements ScaleConverter {

		@Override
		public String getString(final double realScale) {
			final FractionalScale scale = new FractionalScale(realScale);
			// is fractional scale invalid?
			if (scale.getDenom() == 0) {
				if (realScale >= 1) return String.format("%.2fX", realScale);
				// else scale < 1
				return String.format("1/%.2fX", (1 / realScale));
			}
			// or do we have a whole number scale?
			if (scale.getDenom() == 1) {
				return String.format("%dX", scale.getNumer());
			}
			// else have valid fraction
			return String.format("%d/%dX", scale.getNumer(), scale.getDenom());
		}
	}

	private class FractionalScale {

		private int numer, denom;

		FractionalScale(final double realScale) {
			numer = 0;
			denom = 0;
			if (realScale >= 1) {
				final double floor = Math.floor(realScale);
				if ((realScale - floor) < 0.0001) {
					numer = (int) floor;
					denom = 1;
				}
			}
			else { // factor < 1
				final double recip = 1.0 / realScale;
				final double floor = Math.floor(recip);
				if ((recip - floor) < 0.0001) {
					numer = 1;
					denom = (int) floor;
				}
			}
			if (denom == 0) lookForBestFraction(realScale);
		}

		int getNumer() {
			return numer;
		}

		int getDenom() {
			return denom;
		}

		// This method attempts to find a simple fraction that describes the
		// specified scale. It searches a small set of numbers to minimize
		// time spent. If it fails to find scale it leaves fraction unchanged.

		private void lookForBestFraction(final double scale) {
			final int quickRange = 32;
			for (int n = 1; n <= quickRange; n++) {
				for (int d = 1; d <= quickRange; d++) {
					final double frac = 1.0 * n / d;
					if (Math.abs(scale - frac) < 0.0001) {
						numer = n;
						denom = d;
						return;
					}
				}
			}
		}

	}

	private boolean isMyDataset(final Dataset ds) {
		if (ds == null) return false;
		final ThreeDDisplay disp = getDisplay();
		return threeDDisplayService.getActiveDataset(disp) == ds;
	}

	// -- Event handlers --

	@EventHandler
	protected void onEvent(final WinActivatedEvent event) {
		if (event.getDisplay() != getDisplay()) return;
		final ThreeDDisplay d = getDisplay();
		if (d == null) return;
		d.getCanvas().setCursor(toolService.getActiveTool().getCursor());
	}

	@EventHandler
	protected void onEvent(final PanZoomEvent event) {
		if (event.getDisplay() != getDisplay()) return;
		updateLabel();
	}

	@EventHandler
	protected void onEvent(final DatasetRestructuredEvent event) {
		if (isMyDataset(event.getObject())) updateLabel();
	}

	@EventHandler
	protected void onEvent(final DelayedPositionEvent event) {
		if (event.getDisplay() != getDisplay()) return;
		updateLabel();
	}
	
	// NB - using less restrictive event type here. This captures both dataset
	// data type changes and dataset axis type changes. each affect label.
	
	@EventHandler
	protected void onEvent(final DatasetUpdatedEvent event) {
		if (!isMyDataset(event.getObject())) return;
		updateLabel();
		updateTitle();
	}

	@Override
	@EventHandler
	protected void onEvent(final DisplayUpdatedEvent event) {
		if (event.getDisplay() != getDisplay()) return;
		updateLabel();
		updateTitle();
	}

}

