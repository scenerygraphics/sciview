package sc.fiji.threed.controls;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import scenery.Node;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Controls>Arc Ball")
public class ArcBallControl  implements Command {
		
	@Override
	public void run() {		
		ThreeDViewer.enableArcBallControl();
		
	}

}

