package sc.iview;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.BufferUtils;
import graphics.scenery.GenericTexture;
import graphics.scenery.Material;
import graphics.scenery.Node;
import io.scif.img.IO;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by kharrington on 6/20/17.
 */
public class Main {
    static ImageJ ij = new ImageJ();
    static GenericTexture gt;// = generateGenericTexture();//convertToGenericTexture(img);

    public static void main(String... args) throws IOException, InterruptedException {
        ij = new ImageJ();

        //ij.launch(args);
        if (!ij.ui().isVisible())
            ij.ui().showUI();



        meshTest();
        //meshTextureTest();
        //volumeRenderTest();
    }

    public static GenericTexture generateGenericTexture() {
        int width = 64;
        int height = 128;

        GLVector dims = new GLVector(width, height, 1 );
        int nChannels = 1;

        ByteBuffer bb = BufferUtils.BufferUtils.allocateByte((int) (width * height * nChannels));

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                bb.put((byte)(Math.random()*255));
            }
        }
        bb.flip();

        return new GenericTexture("neverUsed", dims, nChannels, GLTypeEnum.UnsignedByte, bb, true, true, false);
    }

    public static void meshTextureTest() throws IOException, InterruptedException {
        SciView sciView = ((SciViewService) ij.getContext().getService( "sc.iview.SciViewService" )).getOrCreateActiveSciView();
        Node msh = sciView.addBox();
        msh.fitInto( 10.0f );

        GenericTexture gt = generateGenericTexture();

        msh.getMaterial().getTransferTextures().put("diffuse", gt);
        msh.getMaterial().getTextures().put("diffuse", "fromBuffer:diffuse");
        msh.getMaterial().setDoubleSided(true);
        msh.getMaterial().setNeedsTextureReload(true);


        msh.setNeedsUpdate(true);
        msh.setDirty(true);

    }



    public static GenericTexture convertToGenericTexture( Dataset d ) {
        long width = d.getWidth();
        long height = d.getHeight();

        GLVector dims = new GLVector( width, height, 1 );
        int nChannels = 3;

        ByteBuffer bb = BufferUtils.BufferUtils.allocateByte((int) (width * height * nChannels));

        System.out.println("Size:" + width + " " +  height + " " + nChannels);

        Cursor cur = d.cursor();
        while( cur.hasNext() ) {
            cur.fwd();
            int val = ((UnsignedByteType) cur.get()).get();
            //System.out.println( (byte)val );
            bb.put( (byte) val );
            //bb.put((byte)(Math.random()*255));
        }
        bb.flip();

        return new GenericTexture("neverUsed", dims, nChannels, GLTypeEnum.UnsignedByte, bb, true, true, false);
    }

    public static void meshTest() throws IOException, InterruptedException {
        SciView sciView = ((SciViewService) ij.getContext().getService( "sc.iview.SciViewService" )).getOrCreateActiveSciView();

        //Node msh = sciView.addSTL(SciView.class.getResource("/cored_cube_16bit.stl").getFile());
        //Node msh = sciView.addObj("/Users/kharrington/git/SciView/sphere.obj");

        Node msh = sciView.addBox();
        //Node msh = sciView.addObj("/Users/kharrington/git/SciView/bunny.obj");
        //Node msh = sciView.addObj("/Users/kharrington/git/SciView/goat/goat.obj");

        msh.fitInto( 15.0f );

        //Dataset img = (Dataset) ij.io().open("/Users/kharrington/git/SciView/clown_uint8_small.tif");
        //ij.ui().show(img);

        //Dataset img = ij.scifio().datasetIO().open("/Users/kharrington/git/SciView/clown_uint8_small.tif");

        //Dataset img = (Dataset) ij.io().open("/Users/kharrington/git/SciView/clown_uint8_small.tif");

        //Dataset img = (Dataset) ij.io().open("/Users/kharrington/git/SciView/clown_uint8.tif");
        //Dataset img = (Dataset) ij.io().open("/Users/kharrington/git/SciView/bigulrik.tif");

        Dataset img = (Dataset) ij.io().open("http://mirror.imagej.net/images/clown.jpg");

        GenericTexture gt = convertToGenericTexture(img);

        //Img img = IO.openImgs("/Users/kharrington/git/SciView/clown_uint8_small.tif").get(0).getImg();

        //File file = new File( "/Users/kharrington/git/SciView/clown_uint8_small.tif" );
        //final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

        msh.getMaterial().getTransferTextures().put("diffuse", gt);
        msh.getMaterial().getTextures().put("diffuse", "fromBuffer:diffuse");
        msh.getMaterial().setDoubleSided(true);
        msh.getMaterial().setNeedsTextureReload(true);



        msh.setNeedsUpdate(true);
        msh.setDirty(true);

    }

    public static void volumeRenderTest() throws IOException {

//      Volume render test
        SciView sciView = ((SciViewService) ij.getContext().getService( "sc.iview.SciViewService" )).getOrCreateActiveSciView();
        Dataset testImg = (Dataset) ij.io().open(  SciView.class.getResource("/cored_cube_16bit.tif").getFile() );
        System.out.println( testImg.firstElement().getClass() );
        Node v = sciView.addVolume( testImg, new float[]{1,1,1}, ij.log() );
        v.setScale(new GLVector(10f, 10f, 10f));
        sciView.displayNodeProperties( v, ij.log() );

        int isoLevel = 1;
        Img<UnsignedShortType> testImgImg = (Img<UnsignedShortType>) testImg.getImgPlus().getImg();
        Img<BitType> bitImg = (Img<BitType>) ij.op().threshold().apply(  testImgImg,
                new UnsignedShortType( isoLevel ) );

        Mesh m = ij.op().geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator());

        DefaultMesh dm = (DefaultMesh) m;

        //sciView.displayNodeProperties( sciView.addMesh( m ) );

    }
}
