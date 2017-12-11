package sc.iview.io;

import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
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
import sc.iview.SciView;
import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "SciView>Import>Isosurface")
public class ImportIsosurface  implements Command {
	
	@Parameter
	private OpService ops;
	
	@Parameter
	private int isoLevel;
	
	@Parameter
	private ImgPlus<UnsignedByteType> image;

	@Parameter
	DisplayService displayService;

	@Parameter
	SciView sciView;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		
		Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply( image,
				new UnsignedByteType( isoLevel ) );
		
		Mesh m = ops.geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator());
		
		DefaultMesh dm = (DefaultMesh) m;

		sciView.addMesh( m, logService );
		
	}

}
