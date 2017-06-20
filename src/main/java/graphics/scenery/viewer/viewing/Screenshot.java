package graphics.scenery.viewer.viewing;

import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Screenshot")
public class Screenshot implements Command {
		
	
	
	@Override
	public void run() {
		SceneryViewer.takeScreenshot();
	}

}

