package sc.iview;

import graphics.scenery.Mesh;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.joml.Vector3f;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods.
 *
 * @author Kyle Harrington
 */
public class Utils {
    static public Vector3f convertToVector3f(ColorRGB color) {
        if( color instanceof ColorRGBA) {
            return new Vector3f( color.getRed() / 255f, //
                                 color.getGreen() / 255f, //
                                 color.getBlue() / 255f); //color.getAlpha() / 255f );
        }
        return new Vector3f( color.getRed() / 255f, //
                             color.getGreen() / 255f, //
                             color.getBlue() / 255f );
    }

    static public Img<ARGBType> convertToARGB(Img<UnsignedByteType> screenshot) {
        Img<ARGBType> out = ArrayImgs.argbs(screenshot.dimension(0), screenshot.dimension(1));
        long[] pos = new long[3];
        Cursor<ARGBType> outCur = Views.iterable(out).cursor();
        RandomAccess<UnsignedByteType> sRA = screenshot.randomAccess();
        while( outCur.hasNext() ) {
            outCur.fwd();
            outCur.localize(pos);

            pos[2] = 0;
            sRA.setPosition(pos);
            int r = sRA.get().get();
            pos[2] = 1;
            sRA.setPosition(pos);
            int g = sRA.get().get();
            pos[2] = 2;
            sRA.setPosition(pos);
            int b = sRA.get().get();

            int a = 255;// FIXME

            outCur.get().set(ARGBType.rgba(r, g, b, a));
        }
        return out;
    }

    static public List<Vector3f> getVertexList(Mesh m) {
        List<Vector3f> l = new ArrayList<>();

        FloatBuffer vb = m.getVertices();
        while( vb.hasRemaining() ) {
            float x = vb.get();
            float y = vb.get();
            float z = vb.get();
            l.add( new Vector3f(x, y, z) );
        }

        vb.flip();

        return l;
    }

    public static void writeXYZ(File xyzFile, Mesh mesh) throws IOException {
        BufferedWriter w = new BufferedWriter( new FileWriter(xyzFile) );

        List<Vector3f> verts = getVertexList(mesh);

        for( Vector3f v : verts ) {
            w.write(v.x() + ", " + v.y() + ", " + v.z() + "\n");
        }

        w.close();
    }

    public static void writeXYZ(File xyzFile, List<RealLocalizable> points) throws IOException {
        BufferedWriter w = new BufferedWriter( new FileWriter(xyzFile) );

        if( points.isEmpty() )
            return;

        int numDim = points.get(0).numDimensions();
        double[] pos = new double[numDim];
        for( RealLocalizable v : points ) {
            for( int d = 0; d < numDim; d++ ) {
                if( d > 0 )
                    w.write(", ");
                w.write("" + v.getDoublePosition(d));
            }
            w.write("\n");
        }

        w.close();
    }
}
