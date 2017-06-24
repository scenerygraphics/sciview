package sc.iview.controls;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;
import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Controls>Arc Ball")
public class ArcBallControl  implements Command {

	@Parameter
	SciViewService sceneryService;

	@Override
	public void run() {
		sceneryService.getActiveSceneryViewer().enableArcBallControl();
		
	}

}

