/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
import graphics.scenery.textures.Texture;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Random;

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

    @Parameter
    private IOService ioService;

    @Override
    public void run() {

        // Load the 2D image
        Img<UnsignedByteType> img = sciView.getScreenshot();

        // Add noise
        Random rng = new Random(17);
        img.forEach(t -> t.add(new UnsignedByteType(rng.nextInt(25))));

        ByteBuffer bb = imgToByteBuffer(img);

        Box imgPlane = new Box( new Vector3f( 10f, 10f, 0.01f ) );
        imgPlane.setPosition(new Vector3f(0,10,0));

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
        mat.setSpecular(new Vector3f(1,1,1));
        mat.setDiffuse(new Vector3f(1,1,1));
        mat.setAmbient(new Vector3f(1,1,1));

        // FIXME
//        Texture tex = new Texture(new Vector3f(img.dimension(0), img.dimension(1),1),3, GLTypeEnum.UnsignedByte, bb);
//
//        mat.getTextures().put("diffuse",tex);
        //mat.setNeedsTextureReload(true);

        imgPlane.setMaterial(mat);
        imgPlane.setNeedsUpdate(true);

        sciView.addNode(imgPlane);
        sciView.centerOnNode(imgPlane);


    }

    // This should interleave channels, but the coloring doesnt seem to happen
    private static ByteBuffer imgToByteBuffer(Img<UnsignedByteType> img) {
        int numBytes = (int) (img.dimension(0) * img.dimension(1) * 3);
        ByteBuffer bb = BufferUtils.allocateByte(numBytes);
        byte[] rgb = new byte[]{0, 0, 0};

        RandomAccess<UnsignedByteType> ra = img.randomAccess();

        long[] pos = new long[3];

        for( int y = 0; y < img.dimension(1); y++ ) {
            for( int x = 0; x < img.dimension(0); x++ ) {
                for( int c = 0; c < img.dimension(2) - 1; c++ ) {// hard coded dropping of alpha
                    pos[0] = x; pos[1] = img.dimension(1) - y - 1; pos[2] = c;
                    ra.setPosition(pos);
                    rgb[c] = ra.get().getByte();
                }
                bb.put(rgb);
            }
        }
        bb.flip();

        return bb;
    }


    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(ImagePlaneDemo.class, true, argmap);
    }
}
