package sc.iview.controls.behaviours

import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Behavior for translating a camera
 *
 * @author Kyle Harrington
 */
class CameraTranslateControl(protected val sciView: SciView, var dragSpeed: Float) : DragBehaviour {
    private var lastX = 0
    private var lastY = 0

    //easy 1st threshold is "3", very hard to hit threshold is "30"
    //if dragSpeed == 1.0,  we essentially use slow and fast movements
    //if dragSpeed == 10.0, we essentially use fast and very-fast movements
    protected val dragX_SlowSpeedLimit = 3f
    protected val dragX_FastSpeedLimit = 30f
    protected val dragY_SlowSpeedLimit = 3f
    protected val dragY_FastSpeedLimit = 30f

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    override fun init(x: Int, y: Int) {
        lastX = x
        lastY = y

        //set up (the current) shortcuts to the FPS movement routines
        move_left_slow = sciView.sceneryInputHandler.getBehaviour("move_left") as ClickBehaviour
        move_left_fast = sciView.sceneryInputHandler.getBehaviour("move_left_fast") as ClickBehaviour
        move_left_veryfast = sciView.sceneryInputHandler.getBehaviour("move_left_veryfast") as ClickBehaviour
        move_right_slow = sciView.sceneryInputHandler.getBehaviour("move_right") as ClickBehaviour
        move_right_fast = sciView.sceneryInputHandler.getBehaviour("move_right_fast") as ClickBehaviour
        move_right_veryfast = sciView.sceneryInputHandler.getBehaviour("move_right_veryfast") as ClickBehaviour
        move_forward_slow = sciView.sceneryInputHandler.getBehaviour("move_forward") as ClickBehaviour
        move_forward_fast = sciView.sceneryInputHandler.getBehaviour("move_forward_fast") as ClickBehaviour
        move_forward_veryfast = sciView.sceneryInputHandler.getBehaviour("move_forward_veryfast") as ClickBehaviour
        move_backward_slow = sciView.sceneryInputHandler.getBehaviour("move_back") as ClickBehaviour
        move_backward_fast = sciView.sceneryInputHandler.getBehaviour("move_back_fast") as ClickBehaviour
        move_backward_veryfast = sciView.sceneryInputHandler.getBehaviour("move_back_veryfast") as ClickBehaviour

        //make sure all moves are always defined
        replaceNullBehavioursWith(noMovement)
    }

    private val noMovement = ClickBehaviour { _, _ -> /* intentionally empty */ }

    private var move_left_slow: ClickBehaviour? = null
    private var move_left_fast: ClickBehaviour? = null
    private var move_left_veryfast: ClickBehaviour? = null

    private var move_right_slow: ClickBehaviour? = null
    private var move_right_fast: ClickBehaviour? = null
    private var move_right_veryfast: ClickBehaviour? = null

    private var move_forward_slow: ClickBehaviour? = null
    private var move_forward_fast: ClickBehaviour? = null
    private var move_forward_veryfast: ClickBehaviour? = null

    private var move_backward_slow: ClickBehaviour? = null
    private var move_backward_fast: ClickBehaviour? = null
    private var move_backward_veryfast: ClickBehaviour? = null

    override fun drag(x: Int, y: Int) {
        val dx: Float = dragSpeed * (x - lastX)
        val dy: Float = dragSpeed * (lastY - y)
        //System.out.println(dx+",\t"+dy);
        if (dx > 0) {
            if (dx <= dragX_SlowSpeedLimit) move_right_slow!!.click(x, y)
            else if (dx <= dragX_FastSpeedLimit) move_right_fast!!.click(x, y)
            else move_right_veryfast!!.click(x, y)
        }
        if (dx < 0) {
            if (dx >= -dragX_SlowSpeedLimit) move_left_slow!!.click(x, y)
            else if (dx >= -dragX_FastSpeedLimit) move_left_fast!!.click(x, y)
            else move_left_veryfast!!.click(x, y)
        }
        if (dy > 0) {
            if (dy <= dragY_SlowSpeedLimit) move_forward_slow!!.click(x, y)
            else if (dy <= dragY_FastSpeedLimit) move_forward_fast!!.click(x, y)
            else move_forward_veryfast!!.click(x, y)
        }
        if (dy < 0) {
            if (dy >= -dragY_SlowSpeedLimit) move_backward_slow!!.click(x, y)
            else if (dy >= -dragY_FastSpeedLimit) move_backward_fast!!.click(x, y)
            else move_backward_veryfast!!.click(x, y)
        }
        lastX = x
        lastY = y
    }

    override fun end(x: Int, y: Int) {
    }

    private fun replaceNullBehavioursWith(defaultBehaviour: ClickBehaviour) {
        if (move_left_slow == null)         move_left_slow = defaultBehaviour
        if (move_left_fast == null)         move_left_fast = defaultBehaviour
        if (move_left_veryfast == null)     move_left_veryfast = defaultBehaviour
        if (move_right_slow == null)        move_right_slow = defaultBehaviour
        if (move_right_fast == null)        move_right_fast = defaultBehaviour
        if (move_right_veryfast == null)    move_right_veryfast = defaultBehaviour
        if (move_forward_slow == null)      move_forward_slow = defaultBehaviour
        if (move_forward_fast == null)      move_forward_fast = defaultBehaviour
        if (move_forward_veryfast == null)  move_forward_veryfast = defaultBehaviour
        if (move_backward_slow == null)     move_backward_slow = defaultBehaviour
        if (move_backward_fast == null)     move_backward_fast = defaultBehaviour
        if (move_backward_veryfast == null) move_backward_veryfast = defaultBehaviour
    }
}
