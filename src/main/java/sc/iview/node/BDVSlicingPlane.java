package sc.iview.node;

import bdv.SpimSource;
import bdv.cache.CacheControl;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
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
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static sc.iview.Utils.createAffineTransform3DFromNode;

/**
 * An ImgPlane is a plane that corresponds to a slice of an Img
 *
 * Some code derived from from: https://github.com/saalfeldlab/bigcat/blob/063ad42e75d99023491ebeafcd6b1a8c224b67e9/src/main/java/bdv/bigcat/viewer/viewer3d/OrthoSlice.java
 *
 * @author Kyle Harrington
 * @author Philipp Hanslovsky
 */
public class BDVSlicingPlane<T extends GenericByteType> extends Node {
    private final Bdv b;
    private final BdvHandle bdvHandle;
    private final SliceTarget target;
    private final MultiResolutionRenderer bdvRenderer;
    private final ViewerState renderState;
    private ByteBuffer bb;
    private Box imgPlane;
    private RandomAccessibleInterval<T> img;
    private Volume v;
    private long width;
    private long height;
    //private Spim

    //https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/registration/ViewRegistration.java

    public BDVSlicingPlane(Volume v, RandomAccessibleInterval<T> img) {
        this.setName("SlicingPlane");

        this.img = img;
        this.v = v;

        //SpimSource source = new SpimSource();

        this.b = BdvFunctions.show(img, "Slice");
        this.bdvHandle = b.getBdvHandle();
        this.target = new SliceTarget();

        this.width = img.dimension(0);
        this.height = img.dimension(1);

        this.bdvRenderer = new MultiResolutionRenderer(target, new PainterThread(null), new double[] {1}, 0,
                false, 1, null, false,
                b.getBdvHandle().getViewerPanel().getOptionValues().getAccumulateProjectorFactory(),
                new CacheControl.Dummy());

        this.renderState = b.getBdvHandle().getViewerPanel().getState();

        //imgPlane = new Box( new GLVector( 2f, 2f, 0.001f ) );
        imgPlane = new Box( new GLVector( v.getMaximumBoundingBox().getMax().x()-v.getMaximumBoundingBox().getMin().x(),
                v.getMaximumBoundingBox().getMax().y()-v.getMaximumBoundingBox().getMin().y(),
                0.001f ) );
        imgPlane.setPosition(v.getPosition());
        //imgPlane.setPosition(v.getPosition().minus(new GLVector(v.getMaximumBoundingBox().getMax().times(0.5f))));

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
                pos[0] = x; pos[1] = y;
                ra.setPosition(pos);
                bb.put(ra.get().getByte());
            }
        }
        bb.flip();

        return bb;
    }

//    public RandomAccessible<T> randomAccessible() {
//        //AffineTransform3D planeTform = convertGLMatrixToAffineTransform3D(imgPlane.getWorld());
//        AffineTransform3D planeRotation = createAffineTransform3DFromNode(imgPlane, false);
//        AffineTransform3D planeTranslation = new AffineTransform3D();
//        planeTranslation.translate(imgPlane.getPosition().x(), imgPlane.getPosition().y(), imgPlane.getPosition().z());
////        planeTform.translate(v.getMaximumBoundingBox().getMax().x(),
////                v.getMaximumBoundingBox().getMax().y(),
////                v.getMaximumBoundingBox().getMax().z());
//        //AffineTransform3D volTform = convertGLMatrixToAffineTransform3D(v.getWorld());
//        //AffineTransform3D tform = planeTform.concatenate(volTform.inverse());
//        AffineTransform3D volRotation = createAffineTransform3DFromNode(v, false);
//        AffineTransform3D volTranslation = new AffineTransform3D();
//        volTranslation.translate(v.getPosition().x(), v.getPosition().y(), v.getPosition().z());
//
//        AffineTransform3D dTranslation = new AffineTransform3D();
//        GLVector dV = imgPlane.getPosition().minus(v.getPosition()).plus(v.getMaximumBoundingBox().getMax());
//        dTranslation.translate(dV.x(), dV.y(), dV.z());
//        AffineTransform3D tform = planeRotation.concatenate(volRotation.inverse());
//
////        RealPoint p = new RealPoint(0,0,0);
////        RealPoint outp = new RealPoint(0,0,0);
////
////        tform.apply(p,outp);
////        System.out.println("Tform: " + outp);
//
////        RealPoint p = new RealPoint(10, 10, 10);
////        RealPoint outp = new RealPoint(10, 10, 10);
////        planeRotation.apply(p,outp);
////        System.out.println("Plane rotation: " + outp);
////        planeRotation.concatenate(planeTranslation).apply(p,outp);
////        System.out.println("Plane rotation + translation: " + outp);
////        planeRotation.concatenate(planeTranslation).concatenate(volRotation).apply(p,outp);
////        System.out.println("Plane rotation + translation + vol rot: " + outp);
////        planeRotation.concatenate(planeTranslation).concatenate(volRotation).concatenate(volTranslation).apply(p,outp);
////        System.out.println("Plane rotation + translation + vol rot + vol trans: " + outp);
//
//        RealRandomAccessible<T> realImg = Views.interpolate(Views.extendZero(img), new NearestNeighborInterpolatorFactory<T>());
//        RealTransformRandomAccessible transformedSlice = RealViews.transform(realImg, tform);
//
//        // TODO the slice position is not correct
//        //return Views.hyperSlice(Views.raster(transformedSlice), 2, img.dimension(0)/2);
//        return Views.hyperSlice(Views.raster(transformedSlice), 2, 0   );
//    }

	//from https://stackoverflow.com/questions/29301838/converting-bufferedimage-to-bytebuffer
	public static ByteBuffer convertImageData(BufferedImage bi)
	{
		ByteBuffer byteBuffer;
		DataBuffer dataBuffer = bi.getRaster().getDataBuffer();

		if (dataBuffer instanceof DataBufferByte) {
			byte[] pixelData = ((DataBufferByte) dataBuffer).getData();
			byteBuffer = ByteBuffer.wrap(pixelData);
		}
		else if (dataBuffer instanceof DataBufferUShort) {
			short[] pixelData = ((DataBufferUShort) dataBuffer).getData();
			byteBuffer = ByteBuffer.allocate(pixelData.length * 2);
			byteBuffer.asShortBuffer().put(ShortBuffer.wrap(pixelData));
		}
		else if (dataBuffer instanceof DataBufferShort) {
			short[] pixelData = ((DataBufferShort) dataBuffer).getData();
			byteBuffer = ByteBuffer.allocate(pixelData.length * 2);
			byteBuffer.asShortBuffer().put(ShortBuffer.wrap(pixelData));
		}
		else if (dataBuffer instanceof DataBufferInt) {
			int[] pixelData = ((DataBufferInt) dataBuffer).getData();
			byteBuffer = ByteBuffer.allocate(pixelData.length * 4);
			byteBuffer.asIntBuffer().put(IntBuffer.wrap(pixelData));
		}
		else {
			throw new IllegalArgumentException("Not implemented for data buffer type: " + dataBuffer.getClass());
		}
		return byteBuffer;
	}

    public void rotate(float i) {
        imgPlane.setRotation(imgPlane.getRotation().rotateByAngleY(i));

//        RandomAccessible<T> slice = randomAccessible();
//        long width = img.dimension(0);
//        long height = img.dimension(1);

        // TODO update the spimsource

        bdvRenderer.paint(renderState);

        bb = convertImageData(target.bi);

        //bb = BufferUtils.allocateByte((int) (width*height*3));

        GenericTexture tex = new GenericTexture("imgPlane", new GLVector(width, height,1),1, GLTypeEnum.UnsignedByte, bb);

        Material mat = imgPlane.getMaterial();

        mat.getTransferTextures().put("imgPlane",tex);
        mat.getTextures().put("diffuse","fromBuffer:imgPlane");
        mat.setNeedsTextureReload(true);


    }


    private class SliceTarget implements RenderTarget {
        BufferedImage bi;

        @Override
        public BufferedImage setBufferedImage(BufferedImage bufferedImage) {
            bi = bufferedImage;
            return bi;
        }

        @Override
        public int getWidth() {
            return (int) width;
        }

        @Override
        public int getHeight() {
            return (int) height;
        }
    }
}
