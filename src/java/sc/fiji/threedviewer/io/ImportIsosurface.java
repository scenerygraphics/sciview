package sc.fiji.threedviewer.io;

import java.io.File;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import sc.fiji.display.process.MeshConverter;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>Import>Isosurface")
public class ImportIsosurface  implements Command {
	
	@Parameter
	private OpService ops;
	
	@Parameter
	private int isoLevel;
	
	@Parameter
	private ImgPlus<UnsignedByteType> image;
	//private Img<UnsignedByteType> image;

	@Override
	public void run() {
		
		//Calibration cal = imp.getCalibration();
		//AbstractScale tform = new Scale3D( cal.pixelWidth, cal.pixelHeight, cal.pixelDepth );
		
		Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply( image,
				new UnsignedByteType( isoLevel ) );
		
		Mesh m = ops.geom().marchingCubes( bitImg, isoLevel );		
				
		System.out.println( "Mesh: Num verts = " + m.getVertices().size() + " Num facets = "
				+ "" + m.getFacets().size() );
		
		DefaultMesh dm = (DefaultMesh) m;
		//for( RealLocalizable v : dm.getVertices() ) {
		//	System.out.println( "(" + v.getDoublePosition(0) + ", " + v.getDoublePosition(1) + ", " + v.getDoublePosition(2) + ")" );
		//}
		
		//RealLocalizable v = dm.getVertices().iterator().next();
		
		//System.out.println( "(" + v.getDoublePosition(0) + ", " + v.getDoublePosition(1) + ", " + v.getDoublePosition(2) + ")" );
		
		ThreeDViewer.addMesh( m );
		
	}

}
