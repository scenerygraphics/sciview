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
import net.imagej.mesh.Mesh;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.scijava.Context;
import org.scijava.service.SciJavaService;
import org.scijava.ui.UIService;
import org.scijava.util.Colors;

import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.BufferUtils;
import graphics.scenery.GenericTexture;
import graphics.scenery.Material;
import graphics.scenery.Node;

/**
 * Entry point for testing SciView functionality.
 * 
 * @author Kyle Harrington
 */
public class Main {
    private static Context context;
    private static DatasetIOService io;
    private static OpService ops;
    private static UIService ui;

    public static void main( String... args ) throws IOException {
        context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class );
        io = context.service( DatasetIOService.class );
        ops = context.service( OpService.class );
        ui = context.service( UIService.class );

        if( !ui.isVisible() ) ui.showUI();

        SciViewService sciViewService = context.service( SciViewService.class );
        SciView sciView = sciViewService.getOrCreateActiveSciView();
        sciView.getCamera().setPosition( new GLVector( 0.0f, 0.0f, 5.0f ) );
        sciView.getCamera().setTargeted( true );
        sciView.getCamera().setTarget( new GLVector( 0, 0, 0 ) );
        sciView.getCamera().setDirty( true );
        sciView.getCamera().setNeedsUpdate( true );
        //sciView.getCamera().setNeedsUpdateWorld(true);

        //lineTest( sciView );
        //meshTest( sciView );
        //meshTextureTest( sciView );
        volumeRenderTest( sciView, false );
    }

    public static void lineTest( SciView sciView ) {
        int numPoints = 25;
        Vector3[] points = new Vector3[numPoints];

        for( int k = 0; k < numPoints; k++ ) {
            points[k] = new ClearGLVector3( ( float ) ( 10.0f * Math.random() - 5.0f ), //
                                            ( float ) ( 10.0f * Math.random() - 5.0f ), //
                                            ( float ) ( 10.0f * Math.random() - 5.0f ) );

        }

        double edgeWidth = 0.1;

        sciView.addLine( points, Colors.LIGHTSALMON, edgeWidth );
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

    public static void meshTextureTest( final SciView sciView ) {
        Node msh = sciView.addBox();
        msh.fitInto( 10.0f, true );

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

    public static void meshTest( SciView sciView ) throws IOException {

        Mesh m = ( new STLMeshIO() ).open( SciView.class.getResource("/WieseRobert_simplified_Cip1.stl" ).getFile());

        Node msh = sciView.addMesh( m );

        msh.fitInto( 15.0f, true );

        Material mat = new Material();
        mat.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        mat.setDiffuse( new GLVector( 0.8f, 0.5f, 0.4f ) );
        mat.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        mat.setDoubleSided( true );

        msh.setMaterial( mat );

        msh.setNeedsUpdate( true );
        msh.setDirty( true );

    }

    public static void volumeRenderTest( final SciView sciView, boolean iso ) throws IOException {

        Dataset cube = io.open( SciView.class.getResource( "/cored_cube_var2_8bit.tif" ).getFile() );

        System.out.println( cube.firstElement().getClass() );
        Node v = sciView.addVolume( cube, new float[] { 1, 1, 1 } );
        v.setScale( new GLVector( 2f, 2f, 2f ) );
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
