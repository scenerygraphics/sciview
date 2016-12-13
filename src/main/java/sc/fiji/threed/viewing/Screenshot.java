package sc.fiji.threed.viewing;

import org.scijava.plugin.Plugin;

import sc.fiji.threed.ThreeDViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Screenshot")
public class Screenshot implements Command {
		
	
	
	@Override
	public void run() {
		ThreeDViewer.takeScreenshot();
	}

}

