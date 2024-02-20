package sc.iview.controls.behaviours
import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.controls.behaviours.MovementCommand
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import org.joml.Vector3f
import sc.iview.SciView

class ArcballResetMovementCommand(
        direction: String,
        n: () -> Node?,
        speed: Float,
        private val sciview: SciView
) : MovementCommand(direction, n, speed) {

    @Synchronized override fun click(x: Int, y: Int) {
        // First, call the superclass method to handle the movement
        super.click(x, y)

        // Now, handle the arcball reset
        // Since you can't directly access the 'node', use 'n()' to get the current Node, which should be the Camera
        val cameraNode = sciview.camera

        // Assuming SciView has a method to get the camera and its position
        // And assuming 'targetArcball' is a feature of SciView you want to update
        val currentDistance = sciview.targetArcball.distance // Example to access current distance
        val newTargetPosition = cameraNode!!.spatial().position + cameraNode.forward.mul(currentDistance, Vector3f())

        // Update the targetArcball's target to the new position
        // This assumes that 'targetArcball.target' is a setter method or lambda you can update
        sciview.targetArcball.target = { newTargetPosition }
    }
}
