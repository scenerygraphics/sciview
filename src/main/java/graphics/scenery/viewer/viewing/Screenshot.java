package graphics.scenery.viewer.viewing;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>View>Screenshot")
public class Screenshot implements Command {

	@Parameter
	private SceneryService sceneryService;
	
	@Override
	public void run() {
		sceneryService.getActiveSceneryViewer().takeScreenshot();
	}

}

