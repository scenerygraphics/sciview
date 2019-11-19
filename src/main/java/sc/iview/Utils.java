package sc.iview;

import cleargl.GLMatrix;
import cleargl.GLVector;
import graphics.scenery.Node;
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

    static public AffineTransform3D createAffineTransform3DFromNode(Node n, boolean setTranslation) {
        AffineTransform3D tform = new AffineTransform3D();
        double[] ds = new double[16];

        GLMatrix m = n.getWorld();

        tform.set(
                m.get(0,0),
                m.get(0,1),
                m.get(0,2),
                m.get(0,3),

                m.get(1,0),
                m.get(1,1),
                m.get(1,2),
                m.get(1,3),

                m.get(2,0),
                m.get(2,1),
                m.get(2,2),
                m.get(2,3),

                m.get(3,0),
                m.get(3,1),
                m.get(3,2),
                m.get(3,3));

//        float[] rot = n.getRotation().toMatrix(new float[16], 0);
//        for( int k = 0; k < ds.length; k++ )
//            ds[k] = rot[k];
//        tform.set(ds);

        GLVector p = n.getPosition();

        if( setTranslation )
            tform.setTranslation(p.x(), p.y(), p.z());
        
        return tform;
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
        //float[] fs = tmat.invert().getTransposedFloatArray();
        float[] fs = tmat.getFloatArray();
        for( k = 0; k < ds.length; k++ ) {
            ds[k] = fs[k];
        }
        tform.set(ds);
        tform.translate(tmat.get(3,0), tmat.get(3,1), tmat.get(3,2));
        return tform;
    }

//    static public AffineTransform3D convertGLMatrixToAffineTransform3D(GLMatrix tmat) {
//        AffineTransform3D tform = new AffineTransform3D();
//        double[] ds = new double[16];
//        int k=0;
//        for( int r=0; r<4; r++ ) {
//            for (int c = 0; c < 4; c++) {
//                ds[k] = tmat.get(r, c);
//                k++;
//            }
//        }
//        tform.set(ds);
//        return tform;
//    }

//    static public AffineTransform3D convertGLMatrixToAffineTransform3D(GLMatrix tmat) {
//        AffineTransform3D tform = new AffineTransform3D();
//        double[] ds = new double[16];
//
//        ds[0] = tmat.get(0,0);
//        ds[1] = tmat.get(0,1);
//        ds[2] = tmat.get(0,2);
//        ds[3] = tmat.get(0,3);
//        ds[4] = tmat.get(1,0);
//        ds[5] = tmat.get(1,1);
//        ds[6] = tmat.get(1,2);
//        ds[7] = tmat.get(1,3);
//        ds[8] = tmat.get(2,0);
//        ds[9] = tmat.get(2,1);
//        ds[10] = tmat.get(2,2);
//        ds[11] = tmat.get(2,3);
//        tform.set(ds);
//        System.out.println("GLMatrix: " + tmat);
//        System.out.println("ATform: " + tform);
//        return tform;
//    }

//    static public AffineTransform3D convertGLMatrixToAffineTransform3D(GLMatrix tmat) {
//        AffineTransform3D tform = new AffineTransform3D();
//        double[][] ds = new double[4][4];
//
//        for( int r = 0; r < 4; r++ ) {
//            for( int c = 0; c < 4; c++ ) {
//                ds[r][c] = tmat.get(r,c);
//            }
//        }
//        tform.set(ds);
//        System.out.println("GLMatrix: " + tmat);
//        System.out.println("ATform: " + tform);
//        return tform;
//    }

//    static public AffineTransform3D convertGLMatrixToAffineTransform3D(GLMatrix tmat) {
//        AffineTransform3D tform = new AffineTransform3D();
//        double[] ds = new double[16];
//        ds[0] = tmat.get(0,0);
//        ds[1] = tmat.get(0,1);
//        ds[2] = tmat.get(0,2);
//        ds[3] = 0;
//        ds[4] = tmat.get(1,0);
//        ds[5] = tmat.get(1,1);
//        ds[6] = tmat.get(1,2);
//        ds[7] = 0;
//        ds[8] = tmat.get(2,0);
//        ds[9] = tmat.get(2,1);
//        ds[10] = tmat.get(2,2);
//        ds[11] = 0;
//        ds[12] = tmat.get(3,0);
//        ds[13] = tmat.get(3,1);
//        ds[14] = tmat.get(3,2);
//        ds[15] = 1;
//        tform.set(ds);
//        return tform;
//    }

//    static public AffineTransform3D convertGLMatrixToAffineTransform3D(GLMatrix tmat) {
//        AffineTransform3D tform = new AffineTransform3D();
//        double[] ds = new double[16];
//        ds[0] = tmat.get(0,0);
//        ds[4] = tmat.get(0,1);
//        ds[8] = tmat.get(0,2);
//        ds[12] = 0;
//        ds[1] = tmat.get(1,0);
//        ds[5] = tmat.get(1,1);
//        ds[9] = tmat.get(1,2);
//        ds[13] = 0;
//        ds[2] = tmat.get(2,0);
//        ds[6] = tmat.get(2,1);
//        ds[10] = tmat.get(2,2);
//        ds[14] = 0;
//        ds[3] = tmat.get(3,0);
//        ds[7] = tmat.get(3,1);
//        ds[11] = tmat.get(3,2);
//        ds[15] = 1;
//        tform.set(ds);
//        return tform;
//    }


}
