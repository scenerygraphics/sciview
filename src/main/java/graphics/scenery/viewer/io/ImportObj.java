package graphics.scenery.viewer.io;

import java.io.File;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Import>Obj")
public class ImportObj  implements Command {
	
	@Parameter
	private File objFile;

	@Parameter
	private SceneryService sceneryService;

	@Override
	public void run() {
		if ( objFile != null )
		{
			try
			{
				sceneryService.getActiveSceneryViewer().addObj( objFile.getAbsolutePath() );
			}
			catch ( final Exception e )
			{
				throw new RuntimeException( e );
			}
		}		
	}

}
