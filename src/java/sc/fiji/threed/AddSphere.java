package sc.fiji.threed;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cleargl.GLVector;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.RealLocalizable;

import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import sc.fiji.display.process.MeshConverter;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Add>Sphere")
public class AddSphere  implements Command {
		
	@Parameter
	private int radius;
	
	@Override
	public void run() {
		//GLVector pos = ThreeDViewer.getSelectedMesh().getPosition();
		Mesh mesh = MeshConverter.getOpsMesh( ThreeDViewer.getSelectedMesh() );
		RealLocalizable center = ((DefaultMesh) mesh).getCenter();
		GLVector pos = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
		ThreeDViewer.addSphere( pos, radius );
	}

}
