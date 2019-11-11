package sc.iview.controls.behaviours;

import cleargl.GLVector;
import graphics.scenery.Camera;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;
import graphics.scenery.Node;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.List;

public class NodeTranslateControl implements DragBehaviour, ScrollBehaviour {

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

    public GLVector getLRAxis() {
        // Object mode
        return getCamera().getForward().cross(getCamera().getUp()).normalize();
    }

    public GLVector getUDAxis() {
        // Object mode
        return getCamera().getUp();
    }

    public GLVector getFBAxis() {
        // Object mode
        return getCamera().getForward();
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
            GLVector leftPoint = getCamera().getTarget().plus(getLRAxis().times(lineLengths));
            GLVector rightPoint = getCamera().getTarget().plus(getLRAxis().times(-1 * lineLengths));
            Cylinder cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(leftPoint,rightPoint);
            cylinder.setName("L-R axis");
            Material mat = new Material();
            mat.setDiffuse(new GLVector(1,0,0));
            mat.setAmbient(new GLVector(1,0,0));
            cylinder.setMaterial(mat);
            cylinder.setPosition(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getOrigin().plus(getLRAxis().times(lineLengths)));
            sciView.addNode(cylinder, false);
            axes.add(cylinder);

            // Axis orthogonal to camera lookAt (along viewplane)
            GLVector upPoint = getCamera().getTarget().plus(getUDAxis().times(lineLengths));
            GLVector downPoint = getCamera().getTarget().plus(getUDAxis().times(-1 * lineLengths));
            cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(upPoint,downPoint);
            cylinder.setName("U-D axis");
            mat = new Material();
            mat.setDiffuse(new GLVector(0.25f,1f,0.25f));
            mat.setAmbient(new GLVector(0.25f,1f,0.25f));
            cylinder.setMaterial(mat);
            cylinder.setPosition(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getOrigin().plus(getUDAxis().times(lineLengths)));
            sciView.addNode(cylinder, false);
            axes.add(cylinder);

            // Axis orthogonal to camera lookAt (along viewplane)
            GLVector frontPoint = getCamera().getTarget().plus(getFBAxis().times(lineLengths));
            GLVector backPoint = getCamera().getTarget().plus(getFBAxis().times(-1 * lineLengths));
            cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(frontPoint,backPoint);
            cylinder.setName("F-B axis");
            mat = new Material();
            mat.setDiffuse(new GLVector(0f,0.5f,1f));
            mat.setAmbient(new GLVector(0f,0.5f,1f));
            cylinder.setMaterial(mat);
            cylinder.setPosition(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getOrigin().plus(getFBAxis().times(lineLengths)));
            sciView.addNode(cylinder, false);
            axes.add(cylinder);
        }
    }

    @Override public void drag( int x, int y ) {
        if( sciView.getActiveNode() == null || !sciView.getActiveNode().getLock().tryLock()) {
            return;
        }

        sciView.getActiveNode().setPosition(sciView.getActiveNode().getPosition().plus(
                getLRAxis().times(( x - lastX ) * getDragSpeed())).plus(
                        getUDAxis().times(-1 * ( y - lastY ) * getDragSpeed())));

        sciView.getActiveNode().getLock().unlock();
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
        // Clean up axes
        for( Node n : axes ) {
            sciView.deleteNode( n, false );
        }
    }

    @Override
    public void scroll(double wheelRotation, boolean isHorizontal, int x, int y) {
        if( sciView.getActiveNode() == null || !sciView.getActiveNode().getLock().tryLock()) {
            return;
        }

        sciView.getActiveNode().setPosition(sciView.getActiveNode().getPosition().plus(
                getFBAxis().times((float) wheelRotation * getDragSpeed())));

        sciView.getActiveNode().getLock().unlock();
    }
}
