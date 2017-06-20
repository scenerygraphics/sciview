package graphics.scenery.viewer.io;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "Scenery>Import>Isosurface")
public class ImportIsosurface  implements Command {
	
	@Parameter
	private OpService ops;
	
	@Parameter
	private int isoLevel;
	
	@Parameter
	private ImgPlus<UnsignedByteType> image;

	@Parameter
	private SceneryService sceneryService;

	@Override
	public void run() {
		
		Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply( image,
				new UnsignedByteType( isoLevel ) );
		
		Mesh m = ops.geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator());
		
		DefaultMesh dm = (DefaultMesh) m;

		sceneryService.getActiveSceneryViewer().addMesh( m );
		
	}

}
