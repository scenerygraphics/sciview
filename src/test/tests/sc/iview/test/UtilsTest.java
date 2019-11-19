package sc.iview.test;

import cleargl.GLVector;
import graphics.scenery.Node;
import graphics.scenery.Sphere;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.Assert;
import org.junit.Test;

import static sc.iview.Utils.convertGLMatrixToAffineTransform3D;
import static sc.iview.Utils.createAffineTransform3DFromNode;

public class UtilsTest {

    @Test
    public void testTransforms() {
        float[] translation = new float[]{13, 17, 2};

        // Test translation
        AffineTransform3D affine = new AffineTransform3D();
        Node n = new Sphere(1,2);

        affine.translate(translation[0], translation[1], translation[2]);
        n.setPosition(new GLVector(translation));

        AffineTransform3D fromN = createAffineTransform3DFromNode(n);
        assertTransformsEqual(affine, fromN);

        // Test rotation
        double angle = Math.PI * 0.27;
        affine.rotate(0,angle);

        n.getRotation().rotateByAngleX((float) angle);
        n.updateWorld(true,true);

        fromN = createAffineTransform3DFromNode(n);
        assertTransformsEqual(affine, fromN);
    }

    private void assertTransformsEqual(AffineTransform3D affine, AffineTransform3D fromN) {
        double[] affineArray = affine.getRowPackedCopy();
        double[] nArray= fromN.getRowPackedCopy();
        for(int k = 0; k < affineArray.length; k++) {
            Assert.assertEquals(affineArray[k], nArray[k], 0.001);
        }
    }


}
