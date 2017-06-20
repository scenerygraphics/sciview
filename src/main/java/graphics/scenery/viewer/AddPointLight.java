package graphics.scenery.viewer;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Add>Point Light")
public class AddPointLight implements Command {

	@Parameter
	private SceneryService sceneryService;

	@Override
	public void run() {
		sceneryService.getActiveSceneryViewer().addPointLight();
	}

}
