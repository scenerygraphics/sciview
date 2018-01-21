package sc.iview;

import cleargl.GLVector;

/**
 * Created by kharrington on 1/19/18.
 */
public class DVec3s {

    public static GLVector convert(DVec3 v) {
        if(v.getClass()==ClearGLDVec3.class) {
            return ((ClearGLDVec3)v).get();
        } else {
            return new GLVector(v.getFloatPosition(0), v.getFloatPosition(1), v.getFloatPosition(2));
        }
    }
}
