package sc.iview.io;

import java.io.File;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;
import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "SciView>Import>STL", label = "Import STL")
public class ImportSTL  implements Command {
	
	@Parameter
	private File stlFile;

	@Parameter
	SciView sciView;

	@Parameter
	private LogService logService;
	
	@Override
	public void run() {
		if ( stlFile != null )
		{
			try
			{
				sciView.addSTL( stlFile.getAbsolutePath() );
			}
			catch ( final Exception e )
			{
				logService.trace( e );
			}
		}		
	}

}
