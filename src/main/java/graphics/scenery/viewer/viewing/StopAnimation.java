package graphics.scenery.viewer.viewing;

import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Stop Animation")
public class StopAnimation  implements Command {
		
	@Override
	public void run() {
		Thread animator = SceneryViewer.getAnimationThread();
		if( animator != null ) {
			animator.stop();
			SceneryViewer.setAnimationThread( null );
		}

	}

}

