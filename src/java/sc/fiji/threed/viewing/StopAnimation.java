package sc.fiji.threed.viewing;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import scenery.Node;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Stop Animation")
public class StopAnimation  implements Command {
		
	@Override
	public void run() {
		Thread animator = ThreeDViewer.getAnimationThread();
		if( animator != null ) {
			ThreeDViewer.setAnimationThread( null );
		}

	}

}

