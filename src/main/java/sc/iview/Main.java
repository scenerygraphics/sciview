package sc.iview;

import cleargl.GLVector;
import graphics.scenery.Node;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.io.IOException;

/**
 * Created by kharrington on 6/20/17.
 */
public class Main {
    public static void main(String... args) throws IOException {
        ImageJ ij = new ImageJ();

        //ij.launch(args);
        if( !ij.ui().isVisible() )
            ij.ui().showUI();

//      Volume render test
        SciView sciView = ((SciViewService) ij.getContext().getService( "sc.iview.SciViewService" )).getOrCreateActiveSciView();
        Dataset testImg = (Dataset) ij.io().open( "/Users/kharrington/git/SciView/resources/cored_cube_16bit.tif" );
        System.out.println( testImg.firstElement().getClass() );
        Node v = sciView.addVolume( testImg, new float[]{1,1,1} );
        v.setScale(new GLVector(10f, 10f, 10f));
        sciView.displayNodeProperties( v );

        int isoLevel = 1;
        Img<UnsignedShortType> testImgImg = (Img<UnsignedShortType>) testImg.getImgPlus().getImg();
        Img<BitType> bitImg = (Img<BitType>) ij.op().threshold().apply(  testImgImg,
                new UnsignedShortType( isoLevel ) );

        Mesh m = ij.op().geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator());

        DefaultMesh dm = (DefaultMesh) m;

        sciView.displayNodeProperties( sciView.addMesh( m ) );

    }
}
