package sc.fiji.threed.ops;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;

import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import sc.fiji.display.process.MeshConverter;
import scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Mesh>Smooth")
public class MeshSmoothing implements Command {
	
	@Parameter
	private int meshSmoothingSteps;
	
	@Parameter
	private OpService ops;
		
	@Override
	public void run() {
		Mesh currentMesh = ThreeDViewer.getSelectedMesh();
		DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh( currentMesh );	
		
		net.imagej.ops.geom.geom3d.mesh.Mesh smoothMesh = ops.geom().meshSmoothing( (net.imagej.ops.geom.geom3d.mesh.Mesh) opsMesh, meshSmoothingSteps ); 				
		
		ThreeDViewer.removeMesh( currentMesh );
		ThreeDViewer.addMesh( smoothMesh );

	}

}
