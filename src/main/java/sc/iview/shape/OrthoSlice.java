package sc.iview.shape;

import java.awt.image.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import bdv.cache.CacheControl;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import cleargl.GLMatrix;
import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.*;
import net.imagej.Dataset;
import net.imglib2.Point;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformListener;
import sc.iview.SciView;

/**
 * An orthoslice
 *
 * Taken from: https://github.com/saalfeldlab/bigcat/blob/063ad42e75d99023491ebeafcd6b1a8c224b67e9/src/main/java/bdv/bigcat/viewer/viewer3d/OrthoSlice.java
 *
 * @author Philipp Hanslovsky
 * @author Kyle Harrington
 */
public class OrthoSlice
{

	private static final String DIFFUSE = "diffuse";

	private final SciView sciView;

	private final RenderTransformListener renderTransformListener = new RenderTransformListener();

	private final List< Node > planes = new ArrayList<>();

	private final OrthoSliceMesh mesh = new OrthoSliceMesh( new Point( 0, 0 ), new Point( 1, 0 ), new Point( 1, 1 ), new Point( 0, 1 ), new AffineTransform3D() );

	private BdvHandle bdvHandle;
	int width = 100;
	int height = 100;
	int numChannels = 1;

	class MyTarget implements RenderTarget
	{
		BufferedImage bi;

		@Override
		public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
		{
			bi = bufferedImage;
			return null;
		}

		@Override
		public int getWidth()
		{
			return width;
		}

		@Override
		public int getHeight()
		{
			return height;
		}
	}
	private final MyTarget target;

	final MultiResolutionRenderer renderer;

	final PointLight[] lights = {
			new PointLight(),
			new PointLight(),
			new PointLight(),
			new PointLight()
	};

	ViewerState renderState;

	//public OrthoSlice(final SciView sciView, final Img<UnsignedByteType> img )
	public OrthoSlice(final SciView sciView, final Dataset img )
	{
		super();
		this.sciView = sciView;
		this.planes.add( mesh );
		this.sciView.addNode( mesh, false );

		final Bdv b = BdvFunctions.show( img, "OrthoSlice" );
				//new BdvOptions().numRenderingThreads( 2 ).is2D() );

		this.bdvHandle = b.getBdvHandle();

		target = new MyTarget();

		renderer = new MultiResolutionRenderer(
				target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false,
				b.getBdvHandle().getViewerPanel().getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );

		renderState = b.getBdvHandle().getViewerPanel().getState();
	}

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

	public void paint()
	{
		synchronized ( this.sciView )
		{

			GLMatrix tmat = sciView.getCamera().getTransformation();
			AffineTransform3D tform = new AffineTransform3D();
			for( int r=0; r<4; r++ )
				for( int c=0; c<4; c++ )
					tform.set(tmat.get(r,c),r,c);

			renderer.paint( renderState );


			final Material m = new Material();
			m.setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
			m.setDiffuse( new GLVector( 0.0f, 0.0f, 0.0f ) );
			m.setSpecular( new GLVector( 0.0f, 0.0f, 0.0f ) );
			//m.setDoubleSided( true );

			final ByteBuffer bb = convertImageData(target.bi);
			mesh.setMaterial( m );

			final String textureName = "texture";
			final String textureType = DIFFUSE;
			final GenericTexture texture = new GenericTexture( textureName, new GLVector( width, height, 1.0f ), numChannels, GLTypeEnum.Byte, bb, true, true );
			m.getTransferTextures().put( textureName, texture );
			m.getTextures().put( textureType, "fromBuffer:" + textureName );
			m.getTextures().put( "ambient", "fromBuffer:" + textureName );
			m.setNeedsTextureReload( true );
			mesh.update( new Point( 0, 0 ), new Point( width, 0 ), new Point( width, height ), new Point( 0, height ),
					tform );
			mesh.setNeedsUpdate( true );
			mesh.setDirty( true );
			final float[] arr = new float[ mesh.getVertices().capacity() ];
			mesh.getVertices().get( arr );
			System.out.println( "SET MESH AT " + Arrays.toString( arr ) );

		}
	}

//	private final class ViewerTransformlistener implements TransformListener< AffineTransform3D >
//	{
//
//		@Override
//		public void transformChanged( final AffineTransform3D transform )
//		{
//			synchronized ( viewerTransform )
//			{
//				viewerTransform.set( transform );
//				synchronized ( viewer )
//				{
//					final int w = viewer.getWidth();
//					final int h = viewer.getHeight();
//					if ( w > 0 && h > 0 )
//						synchronized ( mesh )
//						{
//							mesh.update( new Point( 0, 0 ), new Point( w, 0 ), new Point( w, h ), new Point( 0, h ), viewerTransform.inverse() );
//							updateLights( mesh.getVertices() );
//						}
//				}
//			}
//		}
//	}

	private final class RenderTransformListener implements TransformListener< AffineTransform3D >
	{

		@Override
		public void transformChanged( final AffineTransform3D transform )
		{
			paint();
		}

	}

}
