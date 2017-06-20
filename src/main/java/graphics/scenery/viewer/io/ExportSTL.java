package graphics.scenery.viewer.io;

import java.io.File;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

import graphics.scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Export>STL_", label = "Export STL")
public class ExportSTL  implements Command {
	
	//@Parameter
	//private Mesh scMesh;
	
	@Parameter(style=FileWidget.SAVE_STYLE)
	private File stlFile = new File("");

	@Parameter
	private SceneryService sceneryService;

	@Override
	public void run() {
		Mesh mesh = sceneryService.getActiveSceneryViewer().getSelectedMesh();
		
		if ( mesh != null )
		{
			try
			{
				sceneryService.getActiveSceneryViewer().writeSCMesh( stlFile.getAbsolutePath(), mesh );
				//ThreeDViewer.writeSCMesh( stlFilename, mesh );
			}
			catch ( final Exception e )
			{
				throw new RuntimeException( e );
			}
		}		
	}

}
