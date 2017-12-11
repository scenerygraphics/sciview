package sc.iview.io;

import java.io.File;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import org.scijava.command.Command;

import graphics.scenery.Mesh;
import sc.iview.SciViewService;

@Plugin(type = Command.class, 
		menuPath = "SciView>Export>STL_", label = "Export STL")
public class ExportSTL  implements Command {
	
	//@Parameter
	//private Mesh scMesh;
	
	@Parameter(style=FileWidget.SAVE_STYLE)
	private File stlFile = new File("");

	@Parameter
	private SciViewService sceneryService;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		if( sceneryService.getActiveSciView().getActiveNode() instanceof Mesh ) {
			Mesh mesh = (Mesh)sceneryService.getActiveSciView().getActiveNode();

			if (mesh != null) {
				try {
					sceneryService.getActiveSciView().writeSCMesh(stlFile.getAbsolutePath(), mesh);
					//ThreeDViewer.writeSCMesh( stlFilename, mesh );
				} catch (final Exception e) {
					logService.trace(e);
				}
			}
		}
	}

}
