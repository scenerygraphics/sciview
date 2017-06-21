package graphics.scenery.viewer.ops;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cleargl.GLVector;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;

import org.scijava.command.Command;

import graphics.scenery.viewer.process.MeshConverter;
import graphics.scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Mesh>Convex Hull")
public class ConvexHull implements Command {
	
	@Parameter
	private OpService ops;

	@Parameter
	private SceneryService sceneryService;
		
	@Override
	public void run() {
		if( sceneryService.getActiveSceneryViewer().getActiveNode() instanceof  Mesh ) {
			Mesh currentMesh = (Mesh)sceneryService.getActiveSceneryViewer().getActiveNode();
			DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh(currentMesh);

			currentMesh.getMaterial().setDiffuse(new GLVector(1.0f, 0.0f, 0.0f));

			net.imagej.ops.geom.geom3d.mesh.Mesh smoothMesh = ops.geom().convexHull((net.imagej.ops.geom.geom3d.mesh.Mesh) opsMesh);

			sceneryService.getActiveSceneryViewer().addMesh(smoothMesh);
		}

	}

}
