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
package sc.iview.commands.demo;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.*;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.Colors;
import sc.iview.SciView;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_LINES;

/**
 * A demo of inserting a 2D image as a plane into a scene
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Image Plane Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Image Plane", weight = DEMO_LINES) })
public class ImagePlaneDemo implements Command {

    @Parameter
    private SciView sciView;

    @Override
    public void run() {

        // Load the 2D image
        Img<UnsignedByteType> img = sciView.getScreenshot();

        ByteBuffer bb = imgToByteBuffer(img);

//        ImagePlus imp = ImageJFunctions.wrap(img, "screenshot");
//        imp.show();
//        BufferedImage bi = imp.getBufferedImage();
//
//        byte[] data = ((DataBufferByte) bi.getData().getDataBuffer()).getData();
//
//        ByteBuffer bb = BufferUtils.allocateByteAndPut(data);

        //System.out.println("bi: " + bi.getType() + " dl: " + data.length + " " + imp);

        Box imgPlane = new Box( new GLVector( 10f, 10f, 0.01f ) );
        imgPlane.setPosition(new GLVector(0,10,0));

        FloatBuffer tc = BufferUtils.allocateFloatAndPut(new float[]{
                // front
                0.0f, 0.0f,//--+
                1.0f, 0.0f,//+-+
                1.0f, 1.0f,//+++
                0.0f, 1.0f,//-++
                // right
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                // back
                0.0f, 0.0f,//---
                0.0f, 1.0f,//-+-
                1.0f, 1.0f,//++-
                1.0f, 0.0f,//+--
                // left
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                // bottom
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                // up
                0.0f, 0.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f
        });
        imgPlane.setTexcoords(tc);

        Material mat = new Material();
        mat.setSpecular(new GLVector(1,1,1));
        mat.setDiffuse(new GLVector(1,1,1));
        mat.setAmbient(new GLVector(1,1,1));

        GenericTexture tex = new GenericTexture("imgPlane", new GLVector(img.dimension(0), img.dimension(1),1),3, GLTypeEnum.UnsignedByte,bb);
        tex.setChannels(3);
        tex.setType(GLTypeEnum.UnsignedByte);

        mat.getTransferTextures().put("imgPlane",tex);
        mat.getTextures().put("diffuse","fromBuffer:imgPlane");
        mat.setNeedsTextureReload(true);

        imgPlane.setMaterial(mat);
        imgPlane.setNeedsUpdate(true);

        sciView.addNode(imgPlane);
        sciView.centerOnNode(imgPlane);


    }

    // This should interleave channels, but the coloring doesnt seem to happen
//    private static ByteBuffer imgToByteBuffer(Img<UnsignedByteType> img) {
//        int numBytes = (int) (img.dimension(0) * img.dimension(1) * 3);
//        ByteBuffer bb = BufferUtils.allocateByte(numBytes);
//        //byte[] rgba = new byte[]{0, 0, 0, (byte) 255};
//        byte[] rgb = new byte[]{0, 0, 0};
//
//        RandomAccess<UnsignedByteType> ra = img.randomAccess();
//
//        long[] pos = new long[3];
//
//        System.out.println(img.dimension(0) + " " + img.dimension(1) + " " + img.dimension(2));
//
//        for( int x = 0; x < img.dimension(0); x++ ) {
//            for( int y = 0; y < img.dimension(1); y++ ) {
//                for( int c = 0; c < img.dimension(2) - 1; c++ ) {// hard coded dropping of alpha
//                    pos[0] = x; pos[1] = y; pos[2] = c;
//                    ra.setPosition(pos);
//                    //rgba[c] = ra.get().getByte();
//                    rgb[c] = ra.get().getByte();
//                }
//                bb.put(rgb);
//            }
//        }
//        bb.flip();
//
//        return bb;
//    }

// This stacks channels along y-axis
    private static ByteBuffer imgToByteBuffer(Img<UnsignedByteType> img) {
        int numBytes = (int) (img.dimension(0) * img.dimension(1) * img.dimension(2));
        ByteBuffer bb = BufferUtils.allocateByte(numBytes);

        Cursor<UnsignedByteType> cur = img.cursor();
        while( cur.hasNext() ) {
            UnsignedByteType v = cur.next();
            bb.put(v.getByte());
        }
        bb.flip();

        return bb;
    }
}
