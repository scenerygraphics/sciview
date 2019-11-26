package sc.iview.node;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.*;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.GenericByteType;
import sc.iview.vector.DoubleVector3;
import sc.iview.vector.Vector3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * An ImgPlane is a plane that corresponds to a slice of an Img
 */
public class ImgPlane<T extends GenericByteType> extends Node {
    private RandomAccessibleInterval<T> img;

    public ImgPlane(RandomAccessibleInterval<T> img) {
        this.setName("ImgPlane");
        this.img = img;

        resize(new DoubleVector3(10, 10, 10));

    }

    public void resize(Vector3 newSize) {
        // Remove children, we assume an ImgPlane only has 1 child Box
        List<Node> ch = getChildrenByName("imgPlane");
        while( !ch.isEmpty() ) {
            removeChild(ch.get(0));
            ch = getChildrenByName("imgPlane");
        }

        Box imgPlane = new Box( new GLVector( 10f, 10f, 0.01f ) );
        imgPlane.setName("imgPlane");

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

        ByteBuffer bb = imgToByteBuffer(img);
        GenericTexture tex = new GenericTexture("imgPlane", new GLVector(img.dimension(0), img.dimension(1),1),3, GLTypeEnum.UnsignedByte, bb);

        mat.getTransferTextures().put("imgPlane",tex);
        mat.getTextures().put("diffuse","fromBuffer:imgPlane");
        mat.setNeedsTextureReload(true);

        imgPlane.setMaterial(mat);
        imgPlane.setNeedsUpdate(true);

        this.addChild(imgPlane);
    }

    // This should interleave channels, but the coloring doesnt seem to happen
    private ByteBuffer imgToByteBuffer(RandomAccessibleInterval<T> img) {
        int numBytes = (int) (img.dimension(0) * img.dimension(1) * 3);
        ByteBuffer bb = BufferUtils.allocateByte(numBytes);
        byte[] rgb = new byte[]{0, 0, 0};

        RandomAccess<T> ra = img.randomAccess();

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

}
