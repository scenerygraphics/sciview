package sc.fiji.ui.swing.viewer.threed;

import net.imagej.display.DataView;
import sc.fiji.display.ThreeDDisplay;

import org.scijava.ui.viewer.DisplayPanel;

public interface ThreeDDisplayPanel extends DisplayPanel {

	@Override
	ThreeDDisplay getDisplay();

}
