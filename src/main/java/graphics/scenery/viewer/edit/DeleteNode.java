package graphics.scenery.viewer.edit;

import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Edit>Delete Node")
public class DeleteNode implements Command {

	//Consider taking an object as a parameter? Like the way IJ2 menus work for selecting an object
	//@Parameter
	//private int objectId;
	
	@Override
	public void run() {
		if( SceneryViewer.getSelectedMesh() != null ) {
			SceneryViewer.deleteSelectedMesh();
		}
	}

}

