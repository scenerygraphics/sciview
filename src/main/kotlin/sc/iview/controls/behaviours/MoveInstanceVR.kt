package sc.iview.controls.behaviours

import graphics.scenery.Scene
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour

class MoveInstanceVR(
    val buttonmanager: MultiButtonManager,
    val button: OpenVRHMD.OpenVRButton,
    val trackerRole: TrackerRole,
    val getTipPosition: () -> Vector3f,
    val spotMoveInitCallback: ((Vector3f) -> Unit)? = null,
    val spotMoveDragCallback: ((Vector3f) -> Unit)? = null,
    val spotMoveEndCallback: ((Vector3f) -> Unit)? = null,
): DragBehaviour {

    override fun init(x: Int, y: Int) {
        buttonmanager.pressButton(button, trackerRole)
        if (!buttonmanager.isTwoHandedActive()) {
            spotMoveInitCallback?.invoke(getTipPosition())
        }
    }

    override fun drag(x: Int, y: Int) {
        // Only perform the single hand behavior when no other grab button is currently active
        // to prevent simultaneous execution of behaviors
        if (!buttonmanager.isTwoHandedActive()) {
            spotMoveDragCallback?.invoke(getTipPosition())
        }
    }

    override fun end(x: Int, y: Int) {
        if (!buttonmanager.isTwoHandedActive()) {
            spotMoveEndCallback?.invoke(getTipPosition())
        }
        buttonmanager.releaseButton(button, trackerRole)
    }

    companion object {

        /**
         * Convenience method for adding grab behaviour
         */
        fun createAndSet(
            scene: Scene,
            hmd: OpenVRHMD,
            buttons: List<OpenVRHMD.OpenVRButton>,
            controllerSide: List<TrackerRole>,
            buttonmanager: MultiButtonManager,
            getTipPosition: () -> Vector3f,
            spotMoveInitCallback: ((Vector3f) -> Unit)? = null,
            spotMoveDragCallback: ((Vector3f) -> Unit)? = null,
            spotMoveEndCallback: ((Vector3f) -> Unit)? = null,
        ) {
            hmd.events.onDeviceConnect.add { _, device, _ ->
                if (device.type == TrackedDeviceType.Controller) {
                    device.model?.let { controller ->
                        if (controllerSide.contains(device.role)) {
                            buttons.forEach { button ->
                                val name = "VRDrag:${hmd.trackingSystemName}:${device.role}:$button"
                                val grabBehaviour = MoveInstanceVR(
                                    buttonmanager,
                                    button,
                                    device.role,
                                    getTipPosition,
                                    spotMoveInitCallback,
                                    spotMoveDragCallback,
                                    spotMoveEndCallback
                                )
                                buttonmanager.registerButtonConfig(button, device.role)
                                hmd.addBehaviour(name, grabBehaviour)
                                hmd.addKeyBinding(name, device.role, button)
                            }
                        }
                    }
                }
            }
        }
    }
}