package sc.iview.controls.behaviours

import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Behavior for translating a camera
 *
 * @author Kyle Harrington
 */
class CameraTranslateControl(protected var sciView: SciView, var dragSpeed: Float) : DragBehaviour {
    private var firstEntered = false
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
        if (firstEntered) {
            lastX = x
            lastY = y
            firstEntered = false
        }
    }

    override fun drag(x: Int, y: Int) {
        if (!sciView.camera.lock.tryLock()) {
            return
        }
        val translationVector = Vector3f((x - lastX) * dragSpeed, (y - lastY) * dragSpeed, 0.0f)
        Quaternionf(sciView.camera.rotation).conjugate().transform(translationVector)
        translationVector.y *= -1f
        sciView.camera.position = sciView.camera.position.add(Vector3f(translationVector.x(), translationVector.y(), translationVector.z()))
        sciView.camera.lock.unlock()
    }

    override fun end(x: Int, y: Int) {
        firstEntered = true
    }
}