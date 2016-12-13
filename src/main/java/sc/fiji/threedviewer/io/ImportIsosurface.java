package sc.fiji.threedviewer.io;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultMarchingCubes;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import sc.fiji.threed.ThreeDViewer;

import org.scijava.command.Command;

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
		
		//Mesh m = ops.geom().marchingCubes( (RandomAccessibleInterval<BitType>)bitImg, 0.5, new BitTypeVertexInterpolator() );
		Mesh m = ops.geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator());
		
		//DefaultMarchingCubes<BitType> mCubes = new DefaultMarchingCubes<BitType>();
		//mCubes.setInput( bitImg );
		//Mesh m = mCubes.compute1( bitImg );
		
		//Mesh m = ops.geom().marchingCubes( (RandomAccessibleInterval<BitType>)bitImg, 0.5, new BitTypeVertexInterpolator() );
		//final DefaultMesh m = (DefaultMesh) ops.run(DefaultMarchingCubes.class, bitImg );
				
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
