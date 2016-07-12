package sc.fiji.threedviewer.io;

import java.io.File;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Import>Obj")
public class ImportObj  implements Command {
	
	@Parameter
	private File objFile;

	@Override
	public void run() {
		if ( objFile != null )
		{
			try
			{
				ThreeDViewer.addObj( objFile.getAbsolutePath() );
			}
			catch ( final Exception e )
			{
				throw new RuntimeException( e );
			}
		}		
	}

}
