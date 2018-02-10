package sc.iview.create;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cleargl.GLVector;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import sc.iview.SciView;
import sc.iview.SciViewService;
import sc.iview.process.MeshConverter;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "SciView>Add>Sphere")
public class AddSphere  implements Command {
		
	@Parameter
	private int radius;

	@Parameter
	private SciViewService sceneryService;

	@Parameter
	private SciView sciView;
	
	@Override
	public void run() {
		RealLocalizable center;

		center = new RealPoint( 0, 0, 0 );

		GLVector pos = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
		//sceneryService.getActiveSceneryViewer().addSphere( pos, radius );
		sciView.addSphere( pos, radius );
	}

}
