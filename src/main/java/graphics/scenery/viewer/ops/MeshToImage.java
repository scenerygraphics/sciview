package graphics.scenery.viewer.ops;

import graphics.scenery.viewer.SceneryService;
import net.imagej.Dataset;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;

import org.scijava.command.Command;

import graphics.scenery.viewer.SceneryViewer;
import graphics.scenery.viewer.process.MeshConverter;
import graphics.scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Mesh>Mesh To Image")
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
	private SceneryService sceneryService;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval<BitType> img;
		
	@Override
	public void run() {
		Mesh currentMesh = sceneryService.getActiveSceneryViewer().getSelectedMesh();
		DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh( currentMesh );		
				
		//net.imagej.ops.geom.geom3d.mesh.Mesh img = ops.geom().voxelization( opsMesh, width, height, depth);
		RandomAccessibleInterval<BitType> img = ops.geom().voxelization( opsMesh, width, height, depth);

	}

}
