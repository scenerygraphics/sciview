package sc.iview.controls.behaviours;

import cleargl.GLVector;
import com.jogamp.opengl.math.Quaternion;
import org.scijava.ui.behaviour.DragBehaviour;
import sc.iview.SciView;

public class CameraTranslateControl implements DragBehaviour {
    protected SciView sciView;
    private boolean firstEntered;
    private int lastX;
    private int lastY;

    public float getDragSpeed() {
        return dragSpeed;
    }

    public void setDragSpeed( float dragSpeed ) {
        this.dragSpeed = dragSpeed;
    }

    protected float dragSpeed;

    public CameraTranslateControl( SciView sciView, float dragSpeed ) {
        this.sciView = sciView;
        this.dragSpeed = dragSpeed;
    }

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    @Override public void init( int x, int y ) {
        if (firstEntered) {
            lastX = x;
            lastY = y;
            firstEntered = false;
        }
    }

    @Override public void drag( int x, int y ) {

        if(!sciView.getCamera().getLock().tryLock()) {
            return;
        }

        float[] translationVector = new float[]{ ( x - lastX ) * getDragSpeed(), ( y - lastY ) * getDragSpeed(), 0};

        ( new Quaternion( sciView.getCamera().getRotation() ) ).conjugate().rotateVector( translationVector, 0, translationVector, 0 );
        translationVector[1] *= -1;
        sciView.getCamera().setPosition( sciView.getCamera().getPosition().plus( new GLVector( translationVector ) ) );

        sciView.getCamera().getLock().unlock();
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
    }
}
