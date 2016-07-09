package sc.fiji.threedviewer.io;

import java.io.File;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Export>STL")
public class ExportSTL  implements Command {
	
	//@Parameter
	//private Mesh scMesh;
	
	@Parameter(style=FileWidget.SAVE_STYLE)
	private File stlFile = new File("");
	
	//@Parameter
	//private String stlFilename = "MyMesh.stl";

	@Override
	public void run() {
		Mesh mesh = ThreeDViewer.getMostRecentMesh();				
		
		if ( mesh != null )
		{
			try
			{
				ThreeDViewer.writeSCMesh( stlFile.getAbsolutePath(), mesh );
				//ThreeDViewer.writeSCMesh( stlFilename, mesh );
			}
			catch ( final Exception e )
			{
				throw new RuntimeException( e );
			}
		}		
	}

}
