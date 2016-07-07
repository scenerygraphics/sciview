package sc.fiji.ui.swing.viewer.threed;

import net.imagej.Dataset;
import net.imagej.display.DataView;
import sc.fiji.display.ThreeDDisplay;

import org.scijava.ui.viewer.DisplayViewer;

public interface ThreeDDisplayViewer extends DisplayViewer<DataView> {

	Dataset capture();

	@Override
	ThreeDDisplay getDisplay();

}
