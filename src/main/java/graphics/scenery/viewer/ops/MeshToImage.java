package graphics.scenery.viewer.ops;

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
		menuPath = "ThreeDViewer>Mesh>Mesh To Image")
public class MeshToImage implements Command {
	
	@Parameter
	private int width;
	
	@Parameter
	private int height;
	
	@Parameter
	private int depth;
	
	@Parameter
	private OpService ops;
		
	@Override
	public void run() {
		Mesh currentMesh = SceneryViewer.getSelectedMesh();
		DefaultMesh opsMesh = (DefaultMesh) MeshConverter.getOpsMesh( currentMesh );		
				
		//net.imagej.ops.geom.geom3d.mesh.Mesh img = ops.geom().voxelization( opsMesh, width, height, depth);
		RandomAccessibleInterval<BitType> img = ops.geom().voxelization( opsMesh, width, height, depth);		
		
		net.imglib2.img.display.imagej.ImageJFunctions.show( img );
		
		//ThreeDViewer.addMesh( smoothMesh );

	}

}
