package graphics.scenery.viewer.io;

import java.io.File;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Import>STL", label = "Import STL")
public class ImportSTL  implements Command {
	
	@Parameter
	private File stlFile;

	@Parameter
	private SceneryService sceneryService;

	@Override
	public void run() {
		if ( stlFile != null )
		{
			try
			{
				sceneryService.getActiveSceneryViewer().addSTL( stlFile.getAbsolutePath() );
			}
			catch ( final Exception e )
			{
				throw new RuntimeException( e );
			}
		}		
	}

}
