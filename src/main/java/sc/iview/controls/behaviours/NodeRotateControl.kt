package sc.iview.controls.behaviours

import org.joml.Quaternionf
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Control behavior for rotating a Node
 *
 * @author Vladimir Ulman
 */
class NodeRotateControl(protected val sciView: SciView) : DragBehaviour {
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
        val targetedNode = sciView.activeNode
        val cam = sciView.camera ?: return
        val renderable = targetedNode?.renderable() ?: return
        if (!(renderable.lock.tryLock())) return

        val frameYaw   = sciView.getMouseSpeed() * (x - lastX) * 0.0174533f // 0.017 = PI/180
        val framePitch = sciView.getMouseSpeed() * (y - lastY) * 0.0174533f

        Quaternionf().rotateAxis(frameYaw, cam.up)
                .mul(targetedNode.rotation, targetedNode.rotation)
                .normalize()
        Quaternionf().rotateAxis(framePitch, cam.right)
                .mul(targetedNode.rotation, targetedNode.rotation)
                .normalize()
        targetedNode.needsUpdate = true

        renderable.lock.unlock()

        lastX = x
        lastY = y
    }

    override fun end(x: Int, y: Int) {
    }
}
