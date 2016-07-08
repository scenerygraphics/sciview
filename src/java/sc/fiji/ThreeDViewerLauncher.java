package sc.fiji;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>AALaunch")
public class ThreeDViewerLauncher implements Command {
	@Override
	public void run() {	
		ThreeDViewer.viewer = new ThreeDViewer( "ThreeDViewer", 800, 600 );
		ThreeDViewer.viewer.main();
	}
}
