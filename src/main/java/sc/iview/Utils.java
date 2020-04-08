package sc.iview;

import cleargl.GLVector;
import org.joml.Vector3f;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;

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
}
