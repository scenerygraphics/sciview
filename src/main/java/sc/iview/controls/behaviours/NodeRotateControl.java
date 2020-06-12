package sc.iview.controls.behaviours;

import graphics.scenery.Camera;
import graphics.scenery.Node;
import org.joml.Quaternionf;
import org.scijava.ui.behaviour.DragBehaviour;
import sc.iview.SciView;

/**
 * Control behavior for rotating a Node
 *
 * @author Vladimir Ulman
 *
 */
public class NodeRotateControl implements DragBehaviour {

    protected SciView sciView;
    private boolean firstEntered = true;
    private int lastX;
    private int lastY;

    private float mouseSpeedMultiplier = 0.25f;

    public NodeRotateControl( SciView sciView) {
        this.sciView = sciView;
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
        if (sciView.getActiveNode() == null) return;
        if (firstEntered) {
            lastX = x;
            lastY = y;
            firstEntered = false;
        }
    }

    @Override public void drag( int x, int y ) {
        final Node targetedNode = sciView.getActiveNode();
        if (targetedNode == null || !targetedNode.getLock().tryLock()) return;

        float frameYaw   = mouseSpeedMultiplier*(x - lastX) * 0.0174533f;
        float framePitch = mouseSpeedMultiplier*(y - lastY) * 0.0174533f;

        new Quaternionf().rotateAxis(frameYaw, getCamera().getUp())
                .mul(targetedNode.getRotation(),targetedNode.getRotation())
                .normalize();
        new Quaternionf().rotateAxis(framePitch, getCamera().getRight())
                .mul(targetedNode.getRotation(),targetedNode.getRotation())
                .normalize();
        targetedNode.setNeedsUpdate(true);
        //nothing works...
        //we need to have "targetedNode.getMaximumBoundingBox().getBoundingSphere().getOrigin()" updated
        //so that shift+mouse rotates around the fresh coord...
        //
        //targetedNode.updateWorld(false,false);
        //targetedNode.setBoundingBox( targetedNode.generateBoundingBox() );

        targetedNode.getLock().unlock();

        lastX = x;
        lastY = y;
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
    }
}
