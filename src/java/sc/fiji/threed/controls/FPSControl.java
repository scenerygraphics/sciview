package sc.fiji.threed.controls;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Controls>FPS")
public class FPSControl  implements Command {
		
	@Override
	public void run() {		
		ThreeDViewer.enableFPSControl();
		
	}

}

