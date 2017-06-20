package graphics.scenery.viewer;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Add>Box")
public class AddBox  implements Command {
		
	@Override
	public void run() {
		SceneryViewer.addBox();
	}

}
