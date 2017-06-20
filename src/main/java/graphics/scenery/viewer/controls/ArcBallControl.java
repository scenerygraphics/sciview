package graphics.scenery.viewer.controls;

import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Controls>Arc Ball")
public class ArcBallControl  implements Command {
		
	@Override
	public void run() {
		SceneryViewer.enableArcBallControl();
		
	}

}

