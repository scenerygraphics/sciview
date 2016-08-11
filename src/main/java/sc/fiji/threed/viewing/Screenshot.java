package sc.fiji.threed.viewing;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Screenshot")
public class Screenshot implements Command {
		
	
	
	@Override
	public void run() {
		ThreeDViewer.takeScreenshot();
	}

}

