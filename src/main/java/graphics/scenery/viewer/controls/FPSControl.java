package graphics.scenery.viewer.controls;

import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Controls>FPS")
public class FPSControl  implements Command {
		
	@Override
	public void run() {
		SceneryViewer.enableFPSControl();
		
	}

}

