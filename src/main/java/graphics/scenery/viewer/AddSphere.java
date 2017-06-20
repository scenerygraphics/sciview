package graphics.scenery.viewer;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cleargl.GLVector;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import graphics.scenery.viewer.process.MeshConverter;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Add>Sphere")
public class AddSphere  implements Command {
		
	@Parameter
	private int radius;

	@Parameter
	private SceneryService sceneryService;

	@Override
	public void run() {
		//GLVector pos = ThreeDViewer.getSelectedMesh().getPosition();
		RealLocalizable center;
		if( sceneryService.getActiveSceneryViewer().getSelectedMesh() != null ) {
			Mesh mesh = MeshConverter.getOpsMesh( sceneryService.getActiveSceneryViewer().getSelectedMesh() );
			center = MeshUtils.getCenter( mesh );
		} else {
			center = new RealPoint( 0, 0, 0 );
		}
		//RealLocalizable center = ((DefaultMesh) mesh).getCenter();
		GLVector pos = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
		sceneryService.getActiveSceneryViewer().addSphere( pos, radius );
	}

}
