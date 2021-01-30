package sc.iview.controls.behaviours

import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import sc.iview.SciView

/**
 * Control behavior for moving a Node
 *
 * @author Kyle Harrington
 */
class NodeTranslateControl(protected val sciView: SciView) : DragBehaviour, ScrollBehaviour {
    private var lastX = 0
    private var lastY = 0

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    override fun init(x: Int, y: Int) {
        if (sciView.activeNode == null) return

        lastX = x
        lastY = y
    }

    override fun drag(x: Int, y: Int) {
        val targetedNode = sciView.activeNode;
        val cam = sciView.camera ?: return
        if (targetedNode == null || !targetedNode.lock.tryLock()) return

        cam.right.mul((x - lastX) * sciView.getFPSSpeedSlow() * sciView.getMouseSpeed(), dragPosUpdater )
        targetedNode.position.add( dragPosUpdater )
        cam.up.mul(   (lastY - y) * sciView.getFPSSpeedSlow() * sciView.getMouseSpeed(), dragPosUpdater )
        targetedNode.position.add( dragPosUpdater )
        targetedNode.needsUpdate = true

        targetedNode.lock.unlock()

        lastX = x
        lastY = y
    }

    override fun end(x: Int, y: Int) {
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
        val targetedNode = sciView.activeNode;
        val cam = sciView.camera ?: return
        if (targetedNode == null || !targetedNode.lock.tryLock()) return

        cam.forward.mul( wheelRotation.toFloat() * sciView.getFPSSpeedSlow() * sciView.getMouseSpeed(), scrollPosUpdater )
        targetedNode.position.add( scrollPosUpdater );
        targetedNode.needsUpdate = true

        targetedNode.lock.unlock()
    }

    //aux vars to prevent from re-creating them over and over
    private val dragPosUpdater: Vector3f = Vector3f()
    private val scrollPosUpdater: Vector3f = Vector3f()
}