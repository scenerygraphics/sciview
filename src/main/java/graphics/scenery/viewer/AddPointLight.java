package graphics.scenery.viewer;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Add>Point Light")
public class AddPointLight implements Command {
		
	@Override
	public void run() {
		SceneryViewer.addPointLight();
	}

}
