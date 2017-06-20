package graphics.scenery.viewer;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Add>Box")
public class AddBox  implements Command {

	@Parameter
	SceneryService sceneryService;
		
	@Override
	public void run() {

		sceneryService.getActiveSceneryViewer().addBox();
	}

}
