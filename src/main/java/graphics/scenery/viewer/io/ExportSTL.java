package graphics.scenery.viewer.io;

import java.io.File;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

import graphics.scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Export>STL_", label = "Export STL")
public class ExportSTL  implements Command {
	
	//@Parameter
	//private Mesh scMesh;
	
	@Parameter(style=FileWidget.SAVE_STYLE)
	private File stlFile = new File("");
	
	//@Parameter
	//private String stlFilename = "MyMesh.stl";

	@Override
	public void run() {
		Mesh mesh = SceneryViewer.getSelectedMesh();
		
		if ( mesh != null )
		{
			try
			{
				SceneryViewer.writeSCMesh( stlFile.getAbsolutePath(), mesh );
				//ThreeDViewer.writeSCMesh( stlFilename, mesh );
			}
			catch ( final Exception e )
			{
				throw new RuntimeException( e );
			}
		}		
	}

}
