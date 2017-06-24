package sc.iview.edit;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Edit>Delete Node")
public class DeleteNode implements Command {

	//Consider taking an object as a parameter? Like the way IJ2 menus work for selecting an object
	//@Parameter
	//private int objectId;

	@Parameter
	private SciViewService sceneryService;
	
	@Override
	public void run() {
		if( sceneryService.getActiveSceneryViewer().getActiveNode() != null ) {
			sceneryService.getActiveSceneryViewer().deleteSelectedMesh();
		}
	}

}

