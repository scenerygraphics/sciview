package sc.iview.ops;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.Mesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;
import org.scijava.ui.UIService;
import sc.iview.SciView;
import sc.iview.SciViewService;
import sc.iview.process.MeshConverter;

@Plugin(type = Command.class, 
		menuPath = "SciView>Mesh>Mesh To Image")
public class MeshToImage implements Command {
	
	@Parameter
	private int width;
	
	@Parameter
	private int height;
	
	@Parameter
	private int depth;
	
	@Parameter
	private OpService ops;

	@Parameter
	DisplayService displayService;

	@Parameter
	LogService logService;

	@Parameter
	SciView sciView;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval<BitType> img;

	@Parameter
	UIService uiService;

	@Override
	public void run() {
		if( sciView.getActiveNode() instanceof Mesh ) {
			Mesh currentMesh = (Mesh) sciView.getActiveNode();
			DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh(currentMesh,logService);

			//net.imagej.ops.geom.geom3d.mesh.Mesh img = ops.geom().voxelization( opsMesh, width, height, depth);
			RandomAccessibleInterval<BitType> img = ops.geom().voxelization(opsMesh, width, height, depth);

			uiService.show(img);

		} else {
			logService.warn( "No active node. Add a mesh to the scene and select it.");
		}

	}

}
