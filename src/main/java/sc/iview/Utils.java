package sc.iview;

import cleargl.GLVector;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;

public class Utils {
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
}
