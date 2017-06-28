package sc.iview.ops;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.Mesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;
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
	private SciViewService sceneryService;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval<BitType> img;
		
	@Override
	public void run() {
		if( sceneryService.getActiveSciView().getActiveNode() instanceof Mesh ) {
			Mesh currentMesh = (Mesh) sceneryService.getActiveSciView().getActiveNode();
			DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh(currentMesh);

			//net.imagej.ops.geom.geom3d.mesh.Mesh img = ops.geom().voxelization( opsMesh, width, height, depth);
			RandomAccessibleInterval<BitType> img = ops.geom().voxelization(opsMesh, width, height, depth);
		}

	}

}
