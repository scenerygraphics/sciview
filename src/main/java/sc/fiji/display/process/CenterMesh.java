package sc.fiji.display.process;

import org.scijava.plugin.Plugin;

import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import sc.fiji.display.process.MeshConverter;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Mesh>Re-center")
public class CenterMesh  implements Command {
			
	@Override
	public void run() {
		scenery.Mesh scMesh = ThreeDViewer.getSelectedMesh();
		DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh( scMesh );	
		
		((DefaultMesh) opsMesh).centerMesh();
		
		ThreeDViewer.removeMesh( scMesh );				
				
		ThreeDViewer.addMesh( opsMesh );
	}

}
