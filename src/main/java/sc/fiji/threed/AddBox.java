package sc.fiji.threed;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Add>Box")
public class AddBox  implements Command {
		
	@Override
	public void run() {
		ThreeDViewer.addBox();
	}

}
