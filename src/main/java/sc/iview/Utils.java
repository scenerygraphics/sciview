package sc.iview;

import cleargl.GLMatrix;
import cleargl.GLVector;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;

public class Utils {
    /**
     * Convert a SciJava color to a GLVector
     * @param color
     * @return GLVector version of this color (normalized to 0-1)
     */
    static public GLVector convertToGLVector(ColorRGB color) {
        if( color instanceof ColorRGBA) {
            return new GLVector( color.getRed() / 255f, //
                                 color.getGreen() / 255f, //
                                 color.getBlue() / 255f, //
                                 color.getAlpha() / 255f );
        }
        return new GLVector( color.getRed() / 255f, //
                             color.getGreen() / 255f, //
                             color.getBlue() / 255f );
    }

    static public AffineTransform3D convertGLMatrixToAffineTransform3D(GLMatrix tmat) {
        AffineTransform3D tform = new AffineTransform3D();
        double[] ds = new double[16];
        int k=0;
        for( int r=0; r<4; r++ ) {
            for (int c = 0; c < 4; c++) {
                ds[k] = tmat.get(r, c);
                k++;
            }
        }
        tform.set(ds);
        return tform;
    }
}
