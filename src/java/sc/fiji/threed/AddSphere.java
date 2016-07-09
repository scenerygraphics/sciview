package sc.fiji.threed;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Add>Sphere")
public class AddSphere  implements Command {
		
	@Override
	public void run() {
		ThreeDViewer.addSphere();
	}

}
