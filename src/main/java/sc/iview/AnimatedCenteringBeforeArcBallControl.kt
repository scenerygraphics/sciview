package sc.iview

import graphics.scenery.Camera
import graphics.scenery.controls.behaviours.ArcballCameraControl
import org.joml.Vector3f
import java.util.function.Supplier

/*
 * A wrapping class for the {@ArcballCameraControl} that calls {@link CenterOnPosition()}
 * before the actual Arcball camera movement takes place. This way, the targeted node is
 * first smoothly brought into the centre along which Arcball is revolving, preventing
 * from sudden changes of view (and lost of focus from the user.
 *
 * @author Vladimir Ulman
 * @author Ulrik Guenther
 */
class AnimatedCenteringBeforeArcBallControl(val initAction: (Int, Int) -> Any, val scrollAction: (Double, Boolean, Int, Int) -> Any, name: String, n: () -> Camera?, w: Int, h: Int, target: () -> Vector3f) : ArcballCameraControl(name, n, w, h, target) {

    override fun init(x: Int, y: Int) {
        initAction.invoke(x, y)
        super.init(x, y)
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
        scrollAction.invoke(wheelRotation, isHorizontal, x, y)
        super.scroll(wheelRotation, isHorizontal, x, y)
    }
}