package sc.fiji.threed;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Add>Point Light")
public class PointLight implements Command {
		
	@Override
	public void run() {
		ThreeDViewer.addPointLight();
	}

}
