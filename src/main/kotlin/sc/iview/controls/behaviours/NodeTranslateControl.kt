/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
package sc.iview.controls.behaviours

import graphics.scenery.Camera
import graphics.scenery.Cylinder
import graphics.scenery.Material
import graphics.scenery.Node
import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import sc.iview.SciView
import java.util.*

/**
 * Control behavior for moving a Node
 *
 * @author Kyle Harrington
 */
class NodeTranslateControl(protected var sciView: SciView, var dragSpeed: Float) : DragBehaviour, ScrollBehaviour {
    private var firstEntered = true
    private var lastX = 0
    private var lastY = 0
    protected var axes // Tracking the rendered axes
            : MutableList<Node>? = null
    val camera: Camera
        get() = sciView.camera!!

    // Object mode
    val lRAxis: Vector3f
        get() =// Object mode
            camera.forward.cross(camera.up).normalize()

    fun getUDAxis(): Vector3f {
        // Object mode
        return camera.up
    }

    fun getFBAxis(): Vector3f {
        // Object mode
        return camera.forward
    }

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    override fun init(x: Int, y: Int) {
        val activeNode = sciView.activeNode ?: return
        camera.targeted = true
        camera.target = activeNode.position
        if (firstEntered) {
            lastX = x
            lastY = y
            firstEntered = false
            axes = ArrayList()

            // Draw a line along the axis
            val lineLengths = 50 // Should be proportional to something about the view?

            // Axis orthogonal to camera lookAt (along viewplane)
            val leftPoint = camera.target.add(lRAxis.mul(lineLengths.toFloat()))
            val rightPoint = camera.target.add(lRAxis.mul(-1 * lineLengths.toFloat()))
            var cylinder = Cylinder(1.0f, (lineLengths * 2).toFloat(), 20)
            cylinder.orientBetweenPoints(leftPoint, rightPoint)
            cylinder.name = "L-R axis"
            var mat = Material()
            mat.diffuse = Vector3f(1.0f, 0.0f, 0.0f)
            mat.ambient = Vector3f(1.0f, 0.0f, 0.0f)
            cylinder.material = mat
            cylinder.position = activeNode.getMaximumBoundingBox().getBoundingSphere().origin.add(lRAxis.mul(lineLengths.toFloat()))
            sciView.addNode(cylinder, false)
            (axes as ArrayList<Node>).add(cylinder)

            // Axis orthogonal to camera lookAt (along viewplane)
            val upPoint = camera.target.add(getUDAxis().mul(lineLengths.toFloat()))
            val downPoint = camera.target.add(getUDAxis().mul(-1 * lineLengths.toFloat()))
            cylinder = Cylinder(1.0f, (lineLengths * 2).toFloat(), 20)
            cylinder.orientBetweenPoints(upPoint, downPoint)
            cylinder.name = "U-D axis"
            mat = Material()
            mat.diffuse = Vector3f(0.25f, 1f, 0.25f)
            mat.ambient = Vector3f(0.25f, 1f, 0.25f)
            cylinder.material = mat
            cylinder.position = activeNode.getMaximumBoundingBox().getBoundingSphere().origin.add(getUDAxis().mul(lineLengths.toFloat()))
            sciView.addNode(cylinder, false)
            (axes as ArrayList<Node>).add(cylinder)

            // Axis orthogonal to camera lookAt (along viewplane)
            val frontPoint = camera.target.add(getFBAxis().mul(lineLengths.toFloat()))
            val backPoint = camera.target.add(getFBAxis().mul(-1 * lineLengths.toFloat()))
            cylinder = Cylinder(1.0f, (lineLengths * 2).toFloat(), 20)
            cylinder.orientBetweenPoints(frontPoint, backPoint)
            cylinder.name = "F-B axis"
            mat = Material()
            mat.diffuse = Vector3f(0f, 0.5f, 1f)
            mat.ambient = Vector3f(0f, 0.5f, 1f)
            cylinder.material = mat
            cylinder.position = activeNode.getMaximumBoundingBox().getBoundingSphere().origin.add(getFBAxis().mul(lineLengths.toFloat()))
            sciView.addNode(cylinder, false)
            (axes as ArrayList<Node>).add(cylinder)
        }
    }

    override fun drag(x: Int, y: Int) {
        val activeNode = sciView.activeNode ?: return

        if (sciView.activeNode == null || !activeNode.lock.tryLock()) {
            return
        }
        activeNode.position = activeNode.position.add(
                lRAxis.mul((x - lastX) * dragSpeed)).add(
                getUDAxis().mul(-1 * (y - lastY) * dragSpeed))
        activeNode.lock.unlock()
    }

    override fun end(x: Int, y: Int) {
        firstEntered = true
        // Clean up axes
        for (n in axes!!) {
            sciView.deleteNode(n, false)
        }
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
        val activeNode = sciView.activeNode ?: return

        if (sciView.activeNode == null || !activeNode.lock.tryLock()) {
            return
        }
        activeNode.position = activeNode.position.add(
                getFBAxis().mul(wheelRotation.toFloat() * dragSpeed))
        activeNode.lock.unlock()
    }
}