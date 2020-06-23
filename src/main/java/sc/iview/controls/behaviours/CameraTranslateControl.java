package sc.iview.controls.behaviours;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import sc.iview.SciView;

/**
 * Behavior for translating a camera
 *
 * @author Kyle Harrington
 *
 */
public class CameraTranslateControl implements DragBehaviour {
    protected final SciView sciView;
    private boolean firstEntered = true;
    private int lastX;
    private int lastY;

    public float getDragSpeed() {
        return dragSpeed;
    }

    public void setDragSpeed( float dragSpeed ) {
        this.dragSpeed = dragSpeed;
    }

    protected float dragSpeed;

    //easy 1st threshold is "3", very hard to hit threshold is "30"
    //if dragSpeed == 1.0,  we essentially use slow and fast movements
    //if dragSpeed == 10.0, we essentially use fast and very-fast movements
    protected float dragX_SlowSpeedLimit = 3f;
    protected float dragX_FastSpeedLimit = 30f;
    protected float dragY_SlowSpeedLimit = 3f;
    protected float dragY_FastSpeedLimit = 30f;

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

            //set up (the current) shortcuts to the FPS movement routines
            move_left_slow     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_left");
            move_left_fast     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_left_fast");
            move_left_veryfast = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_left_veryfast");

            move_right_slow     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_right");
            move_right_fast     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_right_fast");
            move_right_veryfast = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_right_veryfast");

            move_forward_slow     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_forward");
            move_forward_fast     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_forward_fast");
            move_forward_veryfast = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_forward_veryfast");

            move_backward_slow     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_back");
            move_backward_fast     = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_back_fast");
            move_backward_veryfast = (ClickBehaviour)sciView.getSceneryInputHandler().getBehaviour("move_back_veryfast");

            //make sure all moves are always defined
            replaceNullBehavioursWith( noMovement );
        }
    }

    private ClickBehaviour move_left_slow,     move_left_fast,     move_left_veryfast;
    private ClickBehaviour move_right_slow,    move_right_fast,    move_right_veryfast;
    private ClickBehaviour move_forward_slow,  move_forward_fast,  move_forward_veryfast;
    private ClickBehaviour move_backward_slow, move_backward_fast, move_backward_veryfast;

    private static ClickBehaviour noMovement = new ClickBehaviour() {
        @Override
        public void click(int x, int y) { /* intentionally empty */ }
    };

    @Override public void drag( int x, int y ) {
        final float dx = dragSpeed*(x-lastX);
        final float dy = dragSpeed*(lastY-y);
        //System.out.println(dx+",\t"+dy);

        if (dx > 0) {
            if (dx <= dragX_SlowSpeedLimit) move_right_slow.click(x,y);
            else if (dx <= dragX_FastSpeedLimit) move_right_fast.click(x,y);
            else move_right_veryfast.click(x,y);
        }
        if (dx < 0) {
            if (dx >= -dragX_SlowSpeedLimit) move_left_slow.click(x,y);
            else if (dx >= -dragX_FastSpeedLimit) move_left_fast.click(x,y);
            else move_left_veryfast.click(x,y);
        }

        if (dy > 0) {
            if (dy <= dragY_SlowSpeedLimit) move_forward_slow.click(x,y);
            else if (dy <= dragY_FastSpeedLimit) move_forward_fast.click(x,y);
            else move_forward_veryfast.click(x,y);
        }
        if (dy < 0) {
            if (dy >= -dragY_SlowSpeedLimit) move_backward_slow.click(x,y);
            else if (dy >= -dragY_FastSpeedLimit) move_backward_fast.click(x,y);
            else move_backward_veryfast.click(x,y);
        }

        lastX = x;
        lastY = y;
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
    }

    private void replaceNullBehavioursWith(final ClickBehaviour defaultBehaviour)
    {
        if (move_left_slow == null) move_left_slow = defaultBehaviour;
        if (move_left_fast == null) move_left_fast = defaultBehaviour;
        if (move_left_veryfast == null) move_left_veryfast = defaultBehaviour;

        if (move_right_slow == null) move_right_slow = defaultBehaviour;
        if (move_right_fast == null) move_right_fast = defaultBehaviour;
        if (move_right_veryfast == null) move_right_veryfast = defaultBehaviour;

        if (move_forward_slow == null) move_forward_slow = defaultBehaviour;
        if (move_forward_fast == null) move_forward_fast = defaultBehaviour;
        if (move_forward_veryfast == null) move_forward_veryfast = defaultBehaviour;

        if (move_backward_slow == null) move_backward_slow = defaultBehaviour;
        if (move_backward_fast == null) move_backward_fast = defaultBehaviour;
        if (move_backward_veryfast == null) move_backward_veryfast = defaultBehaviour;
    }
}
