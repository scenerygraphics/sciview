package sc.iview.swing;

import org.scijava.ui.viewer.DisplayPanel;
import org.scijava.ui.viewer.DisplayWindow;

import sc.iview.SciView;

public class SciViewDisplayWindow implements DisplayWindow {

	private SciView sv;
	
	public SciViewDisplayWindow(SciView sv) {
		this.sv = sv;
	}

	@Override
	public void setTitle(String s) {
		// TODO Auto-generated method stub
		sv.getName();
	}

	@Override
	public void setContent(DisplayPanel panel) {
		// Can do nothing
	}

	@Override
	public void pack() {
		// Can do nothing
	}

	@Override
	public void showDisplay(boolean visible) {
		// TODO Auto-generated method stub
		if ( !visible )
		{
			// Probably should have a var inside graphics.scenery.backends.Renderer for visibility
		}
		if ( visible )
		{
		
		}
	}

	@Override
	public void requestFocus() {
		// Bring focus to the front
	}

	@Override
	public void close() {
		// TODO
	}

	@Override
	public int findDisplayContentScreenX() {
		// Can do nothing
		return 0;
	}

	@Override
	public int findDisplayContentScreenY() {
		// Can do nothing
		return 0;
	}

}
