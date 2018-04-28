/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview;

import io.scif.SCIFIOService;
import io.scif.services.DatasetIOService;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.scijava.Context;
import org.scijava.service.SciJavaService;
import org.scijava.ui.UIService;

import sc.iview.vec3.ClearGLDVec3;
import sc.iview.vec3.DVec3;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.BufferUtils;
import graphics.scenery.GenericTexture;
import graphics.scenery.Node;

/**
 * Entry point for testing SciView functionality.
 * 
 * @author Kyle Harrington
 */
public class Main {
    private static Context context;
    private static SciViewService sciViewService;
    private static DatasetIOService io;
    private static OpService ops;
    private static UIService ui;

    public static void main( String... args ) throws IOException {
        context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class );
        sciViewService = context.getService( SciViewService.class );
        io = context.service( DatasetIOService.class );
        ops = context.service( OpService.class );
        ui = context.service( UIService.class );

        if( !ui.isVisible() ) ui.showUI();

        SciView sciView = sciViewService.getOrCreateActiveSciView();
        sciView.getCamera().setPosition( new GLVector( 0.0f, 0.0f, 5.0f ) );
        sciView.getCamera().setTargeted( true );
        sciView.getCamera().setTarget( new GLVector( 0, 0, 0 ) );
        sciView.getCamera().setDirty( true );
        sciView.getCamera().setNeedsUpdate( true );
        //sciView.getCamera().setNeedsUpdateWorld(true);

        lineTest( sciView );
        //meshTest();
        meshTextureTest();
        volumeRenderTest(false);
    }

    public static void lineTest( SciView sciView ) {
        int numPoints = 25;
        DVec3[] points = new DVec3[numPoints];

        for( int k = 0; k < numPoints; k++ ) {
            points[k] = new ClearGLDVec3( ( float ) ( 10.0f * Math.random() - 5.0f ), //
                                          ( float ) ( 10.0f * Math.random() - 5.0f ), //
                                          ( float ) ( 10.0f * Math.random() - 5.0f ) );

        }

        double edgeWidth = 0.1;
        DVec3 color = new ClearGLDVec3( 1f, 0.75f, 0.5f );

        sciView.addLine( points, color, edgeWidth );
    }

    public static GenericTexture generateGenericTexture() {
        int width = 64;
        int height = 128;

        GLVector dims = new GLVector( width, height, 1 );
        int nChannels = 1;

        ByteBuffer bb = BufferUtils.BufferUtils.allocateByte( width * height * nChannels );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                bb.put( ( byte ) ( Math.random() * 255 ) );
            }
        }
        bb.flip();

        return new GenericTexture( "neverUsed", dims, nChannels, GLTypeEnum.UnsignedByte, bb, true, true, false );
    }

    public static void meshTextureTest() {
        SciView sciView = ( ( SciViewService ) context.getService( "sc.iview.SciViewService" ) ).getOrCreateActiveSciView();
        Node msh = sciView.addBox();
        msh.fitInto( 10.0f );

        GenericTexture texture = generateGenericTexture();

        msh.getMaterial().getTransferTextures().put( "diffuse", texture );
        msh.getMaterial().getTextures().put( "diffuse", "fromBuffer:diffuse" );
        msh.getMaterial().setDoubleSided( true );
        msh.getMaterial().setNeedsTextureReload( true );

        msh.setNeedsUpdate( true );
        msh.setDirty( true );

    }

    public static GenericTexture convertToGenericTexture( Dataset d ) {
        long width = d.getWidth();
        long height = d.getHeight();

        GLVector dims = new GLVector( width, height, 1 );
        int nChannels = 3;

        ByteBuffer bb = BufferUtils.BufferUtils.allocateByte( ( int ) ( width * height * nChannels ) );

        System.out.println( "Size:" + width + " " + height + " " + nChannels );

        Cursor<?> cur = d.cursor();
        while( cur.hasNext() ) {
            cur.fwd();
            int val = ( ( UnsignedByteType ) cur.get() ).get();
            //System.out.println( (byte)val );
            bb.put( ( byte ) val );
            //bb.put((byte)(Math.random()*255));
        }
        bb.flip();

        return new GenericTexture( "neverUsed", dims, nChannels, GLTypeEnum.UnsignedByte, bb, true, true, false );
    }

    public static void meshTest() throws IOException {
        SciView sciView = sciViewService.getOrCreateActiveSciView();

        //Node msh = sciView.addSTL(SciView.class.getResource("/cored_cube_16bit.stl").getFile());
        //Node msh = sciView.addObj("/Users/kharrington/git/SciView/sphere.obj");

        Node msh = sciView.addBox();
        //Node msh = sciView.addObj("/Users/kharrington/git/SciView/bunny.obj");
        //Node msh = sciView.addObj("/Users/kharrington/git/SciView/goat/goat.obj");

        msh.fitInto( 15.0f );

        //Dataset img = io.open("/Users/kharrington/git/SciView/clown_uint8_small.tif");
        //ui.show(img);

        //Dataset img = io.open("/Users/kharrington/git/SciView/clown_uint8_small.tif");

        //Dataset img = io.open("/Users/kharrington/git/SciView/clown_uint8_small.tif");

        //Dataset img = io.open("/Users/kharrington/git/SciView/clown_uint8.tif");
        //Dataset img = io.open("/Users/kharrington/git/SciView/bigulrik.tif");

        Dataset img = io.open( "http://mirror.imagej.net/images/clown.jpg" );

        GenericTexture gt = convertToGenericTexture( img );

        //Img img = IO.openImgs("/Users/kharrington/git/SciView/clown_uint8_small.tif").get(0).getImg();

        //File file = new File( "/Users/kharrington/git/SciView/clown_uint8_small.tif" );
        //final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

        msh.getMaterial().getTransferTextures().put( "diffuse", gt );
        msh.getMaterial().getTextures().put( "diffuse", "fromBuffer:diffuse" );
        msh.getMaterial().setDoubleSided( true );
        msh.getMaterial().setNeedsTextureReload( true );

        msh.setNeedsUpdate( true );
        msh.setDirty( true );

    }

    public static void volumeRenderTest(boolean iso) throws IOException {
        SciView sciView = sciViewService.getOrCreateActiveSciView();
        Dataset cube = io.open( SciView.class.getResource( "/cored_cube_16bit.tif" ).getFile() );
        System.out.println( cube.firstElement().getClass() );
        Node v = sciView.addVolume( cube, new float[] { 1, 1, 1 } );
        v.setScale( new GLVector( 10f, 10f, 10f ) );
        sciView.displayNodeProperties( v );

        if (iso) {
            int isoLevel = 1;
            @SuppressWarnings("unchecked")
            Img<UnsignedShortType> cubeImg = ( Img<UnsignedShortType> ) cube.getImgPlus().getImg();
            Img<BitType> bitImg = ( Img<BitType> ) ops.threshold().apply( cubeImg, new UnsignedShortType( isoLevel ) );

            Mesh m = ops.geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator() );

            sciView.displayNodeProperties( sciView.addMesh( m ) );
        }
    }
}
