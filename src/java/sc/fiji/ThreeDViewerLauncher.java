package sc.fiji;

import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, 
		menu = {@Menu(label = "ThreeDViewer"),
				@Menu(label = "Launch", weight = 3) })
public class ThreeDViewerLauncher implements Command {
	@Override
	public void run() {	
		ThreeDViewer.viewer = new ThreeDViewer( "ThreeDViewer", 800, 600 );
		ThreeDViewer.viewer.main();
	}
}
