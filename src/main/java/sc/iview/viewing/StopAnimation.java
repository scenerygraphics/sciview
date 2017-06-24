package sc.iview.viewing;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;
import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>View>Stop Animation")
public class StopAnimation  implements Command {

	@Parameter
	private SciViewService sceneryService;

	@Override
	public void run() {
		Thread animator = sceneryService.getActiveSceneryViewer().getAnimationThread();
		if( animator != null ) {
			animator.stop();
			sceneryService.getActiveSceneryViewer().setAnimationThread( null );
		}

	}

}

