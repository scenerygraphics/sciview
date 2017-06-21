package graphics.scenery.viewer.controls;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Controls>Arc Ball")
public class ArcBallControl  implements Command {

	@Parameter
	SceneryService sceneryService;

	@Override
	public void run() {
		sceneryService.getActiveSceneryViewer().enableArcBallControl();
		
	}

}

