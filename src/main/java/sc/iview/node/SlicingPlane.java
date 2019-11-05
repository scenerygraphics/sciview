package sc.iview.node;

import cleargl.GLMatrix;
import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.*;
import graphics.scenery.volumes.Volume;
import net.imglib2.*;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static sc.iview.Utils.convertGLMatrixToAffineTransform3D;

/**
 * An ImgPlane is a plane that corresponds to a slice of an Img
 *
 * Some code derived from from: https://github.com/saalfeldlab/bigcat/blob/063ad42e75d99023491ebeafcd6b1a8c224b67e9/src/main/java/bdv/bigcat/viewer/viewer3d/OrthoSlice.java
 *
 * @author Kyle Harrington
 * @author Philipp Hanslovsky
 */
public class SlicingPlane<T extends GenericByteType> extends Node {
    private ByteBuffer bb;
    private Box imgPlane;
    private RandomAccessibleInterval<T> img;
    private Volume v;

    public SlicingPlane(Volume v, RandomAccessibleInterval<T> img) {
        this.setName("SlicingPlane");

        this.img = img;
        this.v = v;

        imgPlane = new Box( new GLVector( 10f, 10f, 0.001f ) );
        imgPlane.setPosition(v.getPosition());

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

        rotate(0);

        imgPlane.setMaterial(mat);
        imgPlane.setNeedsUpdate(true);

        this.addChild(imgPlane);
    }

    // TODO Only handles grayscale now
    private ByteBuffer imgToByteBuffer(RandomAccessible<T> img, Dimensions d) {

        int numBytes = (int) (d.dimension(0) * d.dimension(1));
        ByteBuffer bb = BufferUtils.allocateByte(numBytes);

        RandomAccess<T> ra = img.randomAccess();

        long[] pos = new long[2];

        for( int y = 0; y < d.dimension(1); y++ ) {
            for( int x = 0; x < d.dimension(0); x++ ) {
                pos[0] = x; pos[1] = d.dimension(1) - y - 1;
                ra.setPosition(pos);
                bb.put(ra.get().getByte());
            }
        }
        bb.flip();

        return bb;
    }

    public void rotate(float i) {
        imgPlane.setRotation(imgPlane.getRotation().rotateByAngleY(i));

        AffineTransform3D imgPlaneTform = convertGLMatrixToAffineTransform3D(imgPlane.getTransformation());
        AffineTransform3D volumeTform = convertGLMatrixToAffineTransform3D(v.getTransformation());

        AffineTransform3D tform = volumeTform.concatenate(imgPlaneTform);

        RealRandomAccessible<T> realImg = Views.interpolate(Views.extendZero(img), new NearestNeighborInterpolatorFactory<T>());
        RealTransformRandomAccessible transformedSlice = RealViews.transform(realImg, tform);

        long width = img.dimension(0);
        long height = img.dimension(1);

        MixedTransformView slice = Views.hyperSlice(Views.raster(transformedSlice), 0, 10);

        bb = imgToByteBuffer(slice, Intervals.createMinMax(0,0,width,height));

        //bb = BufferUtils.allocateByte((int) (width*height*3));


        GenericTexture tex = new GenericTexture("imgPlane", new GLVector(width, height,1),1, GLTypeEnum.UnsignedByte, bb);

        Material mat = imgPlane.getMaterial();

        mat.getTransferTextures().put("imgPlane",tex);
        mat.getTextures().put("diffuse","fromBuffer:imgPlane");
        mat.setNeedsTextureReload(true);


    }
}
