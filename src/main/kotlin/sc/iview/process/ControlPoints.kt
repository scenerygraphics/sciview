package sc.iview.process

import graphics.scenery.Material
import graphics.scenery.Node
import graphics.scenery.Sphere
import org.scijava.ui.behaviour.Behaviour
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import org.scijava.util.ColorRGB
import sc.iview.SciView
import sc.iview.Utils
import sc.iview.vector.JOMLVector3
import sc.iview.vector.Vector3
import java.util.ArrayList

/**
 * ControlPoints developed for interactive mesh creation
 *
 * @author Kyle Harrington
 */
class ControlPoints {
    protected var nodes: MutableList<Node>
    private var targetPoint: Node? = null
    private var controlPointDistance = 0f
    fun clearPoints() {
        nodes.clear()
    }

    val vertices: List<Vector3>
        get() {
            val points: MutableList<Vector3> = ArrayList()
            for (k in nodes.indices) {
                points.add(JOMLVector3(nodes[k].position))
            }
            return points
        }

    fun setPoints(newPoints: Array<Vector3?>) {
        nodes.clear()
        nodes = ArrayList()
        for (k in newPoints.indices) {
            val cp = Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS)
            cp.position = JOMLVector3.convert(newPoints[k])
            nodes.add(cp)
        }
    }

    fun addPoint(controlPoint: Node) {
        nodes.add(controlPoint)
    }

    @JvmName("getNodes1")
    fun getNodes(): List<Node> {
        return nodes
    }

    fun initializeSciView(sciView: SciView, controlPointDistance: Float) {
        // This is where the command should change the current inputs setup
        sciView.stashControls()
        sciView.sceneryInputHandler.addBehaviour("place_control_point",
                placeControlPointBehaviour(sciView))
        sciView.sceneryInputHandler.addKeyBinding("place_control_point", "double-click button1")

        // Setup the scrolling behavior to adjust the control point distance
        sciView.sceneryInputHandler.addBehaviour("change_control_point_distance",
                distanceControlPointBehaviour(sciView))
        sciView.sceneryInputHandler.addKeyBinding("change_control_point_distance", "scroll")

        // Create target point
        targetPoint = Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS)
        val mat = Material()
        mat.ambient = Utils.convertToVector3f(TARGET_COLOR)
        mat.diffuse = Utils.convertToVector3f(TARGET_COLOR)
        (targetPoint as Sphere).material = mat
        (targetPoint as Sphere).position = sciView.camera.position.add(sciView.camera.forward.mul(controlPointDistance))
        sciView.addNode(targetPoint, false)
        //sciView.getCamera().addChild(targetPoint);
        (targetPoint as Sphere).update.add {

            //targetPoint.getRotation().set(sciView.getCamera().getRotation().conjugate().rotateByAngleY((float) Math.PI));
            // Set rotation before setting position
            (targetPoint as Sphere).position = sciView.camera.position.add(sciView.camera.forward.mul(controlPointDistance))
        }
    }

    private fun placeControlPointBehaviour(sciView: SciView): Behaviour {
        return ClickBehaviour { _, _ -> placeControlPoint(sciView) }
    }

    private fun distanceControlPointBehaviour(sciView: SciView): Behaviour {
        return ScrollBehaviour { wheelRotation, _, _, _ ->
            controlPointDistance += wheelRotation.toFloat()
            targetPoint!!.position = sciView.camera.position.add(sciView.camera.forward.mul(controlPointDistance))
        }
    }

    private fun placeControlPoint(sciView: SciView) {
        val controlPoint = Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS)
        val mat = Material()
        mat.ambient = Utils.convertToVector3f(DEFAULT_COLOR)
        mat.diffuse = Utils.convertToVector3f(DEFAULT_COLOR)
        controlPoint.material = mat

        //controlPoint.setPosition( sciView.getCamera().getTransformation().mult(targetPoint.getPosition().xyzw()) );
        controlPoint.position = targetPoint!!.position
        addPoint(controlPoint)
        sciView.addNode(controlPoint, false)
    }

    fun cleanup(sciView: SciView) {
        sciView.restoreControls()
        // Remove all control points
        for (n in getNodes()) {
            sciView.deleteNode(n, false)
        }
        sciView.deleteNode(targetPoint, false)
    }

    companion object {
        var DEFAULT_RADIUS = 0.5f
        var DEFAULT_SEGMENTS = 18
        var DEFAULT_COLOR = ColorRGB.fromHSVColor(0.5, 0.5, 0.25)
        var TARGET_COLOR = ColorRGB.fromHSVColor(0.5, 0.75, 0.75)
    }

    init {
        nodes = ArrayList()
    }
}