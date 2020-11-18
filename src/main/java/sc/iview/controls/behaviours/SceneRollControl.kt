package sc.iview.controls.behaviours

import org.joml.Quaternionf
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Control behavior for rolling a scene (along camera's forward axis)
 *
 * @author Vladimir Ulman
 */
class SceneRollControl(protected val sciView: SciView, protected val byFixedAngInRad: Float) : ClickBehaviour, DragBehaviour {
    private val rotQ_CW:  Quaternionf = Quaternionf().rotateAxis(+byFixedAngInRad, 0f, 0f, -1f);
    private val rotQ_CCW: Quaternionf = Quaternionf().rotateAxis(-byFixedAngInRad, 0f, 0f, -1f);

    override fun click(x: Int, y: Int) {
        val cam = sciView.camera ?: return
        rotQ_CW.mul(cam.rotation, cam.rotation).normalize()
    }

    private val minMouseMovementDelta = 2
    private var lastX = 0

    override fun init(x: Int, y: Int) {
        lastX = x
    }

    override fun drag(x: Int, y: Int) {
        val cam = sciView.camera ?: return
        if (x > lastX + minMouseMovementDelta) rotQ_CW.mul(cam.rotation, cam.rotation).normalize()
        else if (x < lastX - minMouseMovementDelta) rotQ_CCW.mul(cam.rotation, cam.rotation).normalize()
        lastX = x
    }

    override fun end(x: Int, y: Int) {
    }
}