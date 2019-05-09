package sc.iview.controls.behaviours;

import cleargl.GLVector;
import com.jogamp.opengl.math.Quaternion;
import graphics.scenery.Camera;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;
import graphics.scenery.Node;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.List;

public class NodeTranslateControl implements DragBehaviour {

    protected SciView sciView;
    private boolean firstEntered = true;
    private int lastX;
    private int lastY;

    protected List<Node> axes;// Tracking the rendered axes

    public float getDragSpeed() {
        return dragSpeed;
    }

    public void setDragSpeed( float dragSpeed ) {
        this.dragSpeed = dragSpeed;
    }

    protected float dragSpeed;

    public NodeTranslateControl( SciView sciView, float dragSpeed ) {
        this.sciView = sciView;
        this.dragSpeed = dragSpeed;
    }

    public Camera getCamera() {
        return sciView.getCamera();
    }

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    @Override public void init( int x, int y ) {
        getCamera().setTargeted(true);
        getCamera().setTarget(sciView.getActiveNode().getPosition());

        if (firstEntered) {
            lastX = x;
            lastY = y;
            firstEntered = false;
            axes = new ArrayList<>();

            // Draw a line along the axis
            int lineLengths = 50;// Should be proportional to something about the view?

            // Axis orthogonal to camera lookAt (along viewplane)
            GLVector lrAxis = getCamera().getForward().cross(getCamera().getUp()).normalize();
            GLVector leftPoint = getCamera().getTarget().plus(lrAxis.times(lineLengths));
            GLVector rightPoint = getCamera().getTarget().plus(lrAxis.times(-1 * lineLengths));
            Cylinder cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(leftPoint,rightPoint);
            cylinder.setName("L-R axis");
            Material mat = new Material();
            mat.setDiffuse(new GLVector(1,0,0));
            mat.setAmbient(new GLVector(1,0,0));
            cylinder.setMaterial(mat);
            GLVector cylCenter = cylinder.getPosition().plus(lrAxis.times(lineLengths)).plus(getCamera().getForward().times(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getRadius()*0.5f));
            cylinder.setPosition(cylCenter);
            sciView.addNode(cylinder, false);
            axes.add(cylinder);

            // Axis orthogonal to camera lookAt (along viewplane)
            GLVector udAxis = getCamera().getUp();
            GLVector upPoint = getCamera().getTarget().plus(udAxis.times(lineLengths));
            GLVector downPoint = getCamera().getTarget().plus(udAxis.times(-1 * lineLengths));
            cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(upPoint,downPoint);
            cylinder.setName("U-D axis");
            mat = new Material();
            mat.setDiffuse(new GLVector(0.25f,1f,0.25f));
            mat.setAmbient(new GLVector(0.25f,1f,0.25f));
            cylinder.setMaterial(mat);
            cylCenter = cylinder.getPosition().plus(udAxis.times(lineLengths)).plus(getCamera().getForward().times(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getRadius()*0.5f));
            cylinder.setPosition(cylCenter);
            sciView.addNode(cylinder, false);
            axes.add(cylinder);

            // Axis orthogonal to camera lookAt (along viewplane)
            GLVector fbAxis = getCamera().getForward();
            GLVector frontPoint = getCamera().getTarget().plus(fbAxis.times(lineLengths));
            GLVector backPoint = getCamera().getTarget().plus(fbAxis.times(-1 * lineLengths));
            cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(frontPoint,backPoint);
            cylinder.setName("F-B axis");
            mat = new Material();
            mat.setDiffuse(new GLVector(0f,0.5f,1f));
            mat.setAmbient(new GLVector(0f,0.5f,1f));
            cylinder.setMaterial(mat);
            cylCenter = cylinder.getPosition().plus(fbAxis.times(lineLengths)).plus(getCamera().getForward().times(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getRadius()*0.5f));
            cylinder.setPosition(cylCenter);
            sciView.addNode(cylinder, false);
            axes.add(cylinder);
        }
    }

    @Override public void drag( int x, int y ) {

        if( sciView.getActiveNode() == null || sciView.getActiveNode().getLock().tryLock() != true) {
            return;
        }

        float[] translationVector = new float[]{ ( x - lastX ) * getDragSpeed(), ( y - lastY ) * getDragSpeed(), 0};

        ( new Quaternion( sciView.getCamera().getRotation() ) ).conjugate().rotateVector( translationVector, 0, translationVector, 0 );
        translationVector[1] *= -1;
        sciView.getActiveNode().setPosition( sciView.getActiveNode().getPosition().plus( new GLVector( translationVector ) ) );

        sciView.getActiveNode().getLock().unlock();
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
        // Clean up axes
        for( Node n : axes ) {
            sciView.deleteNode( n, false );
        }
    }
}
