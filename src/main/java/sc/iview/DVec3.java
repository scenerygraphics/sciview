package sc.iview;

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import org.joml.Vector3f;

/**
 * Created by kharrington on 1/18/18.
 */
public interface DVec3 extends RealLocalizable, RealPositionable {

//    default DVec3() {
//        this();
//    }
//
//    default DVec3(Vector3f source) {
//        System.out.println("constructor");
////        setPosition(source.get(0),0);
////        setPosition(source.get(1),1);
////        setPosition(source.get(2),2);
//    }

    @Override
    public default void localize(float[] position) {
        position[0] = getFloatPosition(0);
        position[1] = getFloatPosition(1);
        position[2] = getFloatPosition(2);
    }

    @Override
    public default void localize(double[] position) {
        position[0] = getDoublePosition(0);
        position[1] = getDoublePosition(1);
        position[2] = getDoublePosition(2);
    }

    @Override
    public default int numDimensions() {
        return 3;
    }
}
