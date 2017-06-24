package sc.iview.io;

import java.io.File;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;
import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Import>Obj")
public class ImportObj  implements Command {
	
	@Parameter
	private File objFile;

	@Parameter
	private SciViewService sceneryService;

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
