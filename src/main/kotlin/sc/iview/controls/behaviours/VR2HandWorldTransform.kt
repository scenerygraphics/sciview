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
import graphics.scenery.utils.extensions.plusAssign
import graphics.scenery.utils.extensions.times
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import sc.iview.controls.behaviours.VRGrabTheWorld.Companion.createAndSet
import java.util.concurrent.CompletableFuture


/** Transform a target node [target] by pressing the same buttons defined in [createAndSet] on both VR controllers.
 * The fastest way to attach the behavior is by using [createAndSet].
 * @author Jan Tiemann
 * @author Samuel Pantze */
class VR2HandWorldTransform(
    name: String,
    controller: Spatial,
    offhand: VRTwoHandDragOffhand,
    val scene: Scene,
    var scaleLocked: Boolean = false,
    var rotationLocked: Boolean = false,
    val target: Node
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
        newRein.y = 0f
        val oldRein = (lastPositionMain - lastPositionOff).normalize()
        oldRein.y = 0f

        val newReinRotation = Quaternionf().lookAlong(newRein, Vector3f(0f, 1f, 0f))
        val oldReinRotation = Quaternionf().lookAlong(oldRein, Vector3f(0f, 1f, 0f))
        val diffRotation = oldReinRotation.mul(newReinRotation.invert())

        // Use the center point between both controller positions as pivot
        val pivot = (currentPositionOff - currentPositionMain).div(2f) + currentPositionOff

        target.let {
            if (!rotationLocked) {
                val rot = Matrix4f().translate(pivot).rotate(diffRotation).translate(pivot.times(-1f))
                it.ifSpatial {
                    position.add(rot.getTranslation(Vector3f()))
                    rotation.mul(rot.getNormalizedRotation(Quaternionf()))
                }
            }
            if (!scaleLocked) {
                target.ifSpatial {
                    // pivot and target are in same space
                    for (i in 0..2) {
                        position.setComponent(i, (position[i] + pivot[i] * (scaleDelta - 1)))
                    }
                    scale *= scaleDelta
                    needsUpdate = true
                }
            }
        }
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
            target: Node
        ): CompletableFuture<VR2HandWorldTransform> {
            @Suppress("UNCHECKED_CAST") return createAndSet(
                hmd, button
            ) { controller: Spatial, offhand: VRTwoHandDragOffhand ->
                VR2HandWorldTransform(
                    "Scaling",
                    controller,
                    offhand,
                    scene,
                    scaleLocked,
                    rotationLocked,
                    target
                )
            } as CompletableFuture<VR2HandWorldTransform>
        }
    }
}