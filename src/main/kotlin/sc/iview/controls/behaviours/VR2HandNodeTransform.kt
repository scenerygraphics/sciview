package sc.iview.controls.behaviours

import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.attribute.spatial.Spatial
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.behaviours.VRScale
import graphics.scenery.controls.behaviours.VRTwoHandDragBehavior
import graphics.scenery.controls.behaviours.VRTwoHandDragOffhand
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import sc.iview.controls.behaviours.VRGrabTheWorld.Companion.createAndSet
import java.util.concurrent.CompletableFuture


/** Transform a target node [target] by pressing the same buttons defined in [createAndSet] on both VR controllers.
 * The fastest way to attach the behavior is by using [createAndSet].
 * [onEndCallback] is an optional lambda that is executed once the behavior ends.
 * @author Jan Tiemann
 * @author Samuel Pantze */
class VR2HandNodeTransform(
    name: String,
    controller: Spatial,
    offhand: VRTwoHandDragOffhand,
    val scene: Scene,
    val scaleLocked: Boolean = false,
    val rotationLocked: Boolean = false,
    val positionLocked: Boolean = false,
    val lockYaxis: Boolean = true,
    val target: Node,
    val onEndCallback: (() -> Unit)? = null
) : VRTwoHandDragBehavior(name, controller, offhand) {

    override fun dragDelta(
        currentPositionMain: Vector3f,
        currentPositionOff: Vector3f,
        lastPositionMain: Vector3f,
        lastPositionOff: Vector3f
    ) {

        val scaleDelta =
            VRScale.getScaleDelta(currentPositionMain, currentPositionOff, lastPositionMain, lastPositionOff)

        val newRein = (currentPositionMain - currentPositionOff).normalize()
        val oldRein = (lastPositionMain - lastPositionOff).normalize()
        if (lockYaxis) {
            oldRein.y = 0f
            newRein.y = 0f
        }

        val newReinRotation = Quaternionf().lookAlong(newRein, Vector3f(0f, 1f, 0f))
        val oldReinRotation = Quaternionf().lookAlong(oldRein, Vector3f(0f, 1f, 0f))
        val diffRotation = oldReinRotation.mul(newReinRotation.invert())

        target.let {
            if (!rotationLocked) {
                it.ifSpatial {
                    rotation.mul(diffRotation)
                }
            }
            if (!scaleLocked) {
                target.ifSpatial {
                    scale *= scaleDelta
                    needsUpdate = true
                }
            }
            if (!positionLocked) {
                val positionDelta =
                    (currentPositionMain + currentPositionOff) / 2f - (lastPositionMain + lastPositionOff) / 2f
                target.ifSpatial {
                    position.add(positionDelta)
                }
            }
        }
    }

    override fun end(x: Int, y: Int) {
        super.end(x, y)
        onEndCallback?.invoke()
    }

    companion object {
        /**
         * Convenience method for adding scale behaviour
         */
        fun createAndSet(
            hmd: OpenVRHMD,
            button: OpenVRHMD.OpenVRButton,
            scene: Scene,
            scaleLocked: Boolean = false,
            rotationLocked: Boolean = false,
            positionLocked: Boolean = false,
            lockYaxis: Boolean = true,
            target: Node,
            onEndCallback: (() -> Unit)? = null
        ): CompletableFuture<VR2HandNodeTransform> {
            @Suppress("UNCHECKED_CAST") return createAndSet(
                hmd, button
            ) { controller: Spatial, offhand: VRTwoHandDragOffhand ->
                VR2HandNodeTransform(
                    "Scaling",
                    controller,
                    offhand,
                    scene,
                    scaleLocked,
                    rotationLocked,
                    positionLocked,
                    lockYaxis,
                    target,
                    onEndCallback
                )
            } as CompletableFuture<VR2HandNodeTransform>
        }
    }
}