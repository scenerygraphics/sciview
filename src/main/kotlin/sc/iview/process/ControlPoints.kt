/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.process

import graphics.scenery.Node
import graphics.scenery.Sphere
import graphics.scenery.attribute.material.Material
import org.joml.Vector3f
import org.scijava.ui.behaviour.Behaviour
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import org.scijava.util.ColorRGB
import sc.iview.SciView
import sc.iview.Utils
import java.util.ArrayList

/**
 * ControlPoints developed for interactive mesh creation
 *
 * @author Kyle Harrington
 */
class ControlPoints {
    protected var nodes: MutableList<Node>
    private lateinit var targetPoint: Node
    private var controlPointDistance = 0f
    fun clearPoints() {
        nodes.clear()
    }

    val vertices: List<Vector3f>
        get() {
            val points: MutableList<Vector3f> = ArrayList()
            for (k in nodes.indices) {
                points.add(Vector3f(nodes[k].position))
            }
            return points
        }

    fun setPoints(newPoints: Array<Vector3f>) {
        nodes.clear()
        nodes = ArrayList()
        for (k in newPoints.indices) {
            val cp = Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS)
            cp.spatialOrNull()?.position = newPoints[k]
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
        val cam= sciView.camera ?: return
        // This is where the command should change the current inputs setup
        sciView.stashControls()
        val inputHandler = sciView.sceneryInputHandler!!
        inputHandler.addBehaviour("place_control_point",
                placeControlPointBehaviour(sciView))
        inputHandler.addKeyBinding("place_control_point", "double-click button1")

        // Setup the scrolling behavior to adjust the control point distance
        inputHandler.addBehaviour("change_control_point_distance",
                distanceControlPointBehaviour(sciView))
        inputHandler.addKeyBinding("change_control_point_distance", "scroll")

        // Create target point
        targetPoint = Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS)
        (targetPoint as Sphere).ifMaterial {
            ambient = Utils.convertToVector3f(TARGET_COLOR)
            diffuse = Utils.convertToVector3f(TARGET_COLOR)
        }
        (targetPoint as Sphere).spatialOrNull()?.position = cam.spatialOrNull()?.position!!.add(cam.forward.mul(controlPointDistance))
        sciView.addNode(targetPoint, false)
        //sciView.getCamera().addChild(targetPoint);
        (targetPoint as Sphere).update.add {

            //targetPoint.getRotation().set(sciView.getCamera().getRotation().conjugate().rotateByAngleY((float) Math.PI));
            // Set rotation before setting position
            (targetPoint as Sphere).spatialOrNull()?.position = cam.spatialOrNull()?.position!!.add(cam.forward.mul(controlPointDistance))
        }
    }

    private fun placeControlPointBehaviour(sciView: SciView): Behaviour {
        return ClickBehaviour { _, _ -> placeControlPoint(sciView) }
    }

    private fun distanceControlPointBehaviour(sciView: SciView): Behaviour {
        return ScrollBehaviour { wheelRotation, _, _, _ ->
            val cam = sciView.camera!!
            controlPointDistance += wheelRotation.toFloat()
            targetPoint.spatialOrNull()?.position = cam.spatialOrNull()?.position!!.add(cam.forward.mul(controlPointDistance))
        }
    }

    private fun placeControlPoint(sciView: SciView) {
        val controlPoint = Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS)
        controlPoint.material {
            ambient = Utils.convertToVector3f(DEFAULT_COLOR)
            diffuse = Utils.convertToVector3f(DEFAULT_COLOR)
        }

        //controlPoint.setPosition( sciView.getCamera().getTransformation().mult(targetPoint.getPosition().xyzw()) );
        controlPoint.spatial().position = targetPoint.spatialOrNull()!!.position
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
