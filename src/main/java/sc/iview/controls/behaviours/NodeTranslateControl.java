package sc.iview.controls.behaviours;

import cleargl.GLVector;
import graphics.scenery.Camera;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;
import graphics.scenery.Node;
import org.joml.Vector3f;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.List;

/**
 * Control behavior for moving a Node
 *
 * @author Kyle Harrington
 *
 */
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

    public Vector3f getLRAxis() {
        // Object mode
        return getCamera().getForward().cross(getCamera().getUp()).normalize();
    }

    public Vector3f getUDAxis() {
        // Object mode
        return getCamera().getUp();
    }

    public Vector3f getFBAxis() {
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
            Vector3f leftPoint = getCamera().getTarget().add(getLRAxis().mul(lineLengths));
            Vector3f rightPoint = getCamera().getTarget().add(getLRAxis().mul(-1 * lineLengths));
            Cylinder cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(leftPoint,rightPoint);
            cylinder.setName("L-R axis");
            Material mat = new Material();
            mat.setDiffuse(new Vector3f(1,0,0));
            mat.setAmbient(new Vector3f(1,0,0));
            cylinder.setMaterial(mat);
            cylinder.setPosition(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getOrigin().add(getLRAxis().mul(lineLengths)));
            sciView.addNode(cylinder, false);
            axes.add(cylinder);

            // Axis orthogonal to camera lookAt (along viewplane)
            Vector3f upPoint = getCamera().getTarget().add(getUDAxis().mul(lineLengths));
            Vector3f downPoint = getCamera().getTarget().add(getUDAxis().mul(-1 * lineLengths));
            cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(upPoint,downPoint);
            cylinder.setName("U-D axis");
            mat = new Material();
            mat.setDiffuse(new Vector3f(0.25f,1f,0.25f));
            mat.setAmbient(new Vector3f(0.25f,1f,0.25f));
            cylinder.setMaterial(mat);
            cylinder.setPosition(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getOrigin().add(getUDAxis().mul(lineLengths)));
            sciView.addNode(cylinder, false);
            axes.add(cylinder);

            // Axis orthogonal to camera lookAt (along viewplane)
            Vector3f frontPoint = getCamera().getTarget().add(getFBAxis().mul(lineLengths));
            Vector3f backPoint = getCamera().getTarget().add(getFBAxis().mul(-1 * lineLengths));
            cylinder = new Cylinder(1, lineLengths * 2, 20);
            cylinder.orientBetweenPoints(frontPoint,backPoint);
            cylinder.setName("F-B axis");
            mat = new Material();
            mat.setDiffuse(new Vector3f(0f,0.5f,1f));
            mat.setAmbient(new Vector3f(0f,0.5f,1f));
            cylinder.setMaterial(mat);
            cylinder.setPosition(sciView.getActiveNode().getMaximumBoundingBox().getBoundingSphere().getOrigin().add(getFBAxis().mul(lineLengths)));
            sciView.addNode(cylinder, false);
            axes.add(cylinder);
        }
    }

    @Override public void drag( int x, int y ) {
        if( sciView.getActiveNode() == null || !sciView.getActiveNode().getLock().tryLock()) {
            return;
        }

        sciView.getActiveNode().setPosition(sciView.getActiveNode().getPosition().add(
                getLRAxis().mul(( x - lastX ) * getDragSpeed())).add(
                        getUDAxis().mul(-1 * ( y - lastY ) * getDragSpeed())));

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

        sciView.getActiveNode().setPosition(sciView.getActiveNode().getPosition().add(
                getFBAxis().mul((float) wheelRotation * getDragSpeed())));

        sciView.getActiveNode().getLock().unlock();
    }
}
