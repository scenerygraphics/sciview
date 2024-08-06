/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
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
package sc.iview

import graphics.scenery.Camera
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.function.Supplier
import org.joml.Matrix4f
import kotlin.math.max
import kotlin.math.min

/*
 * A wrapping class for the {@ArcballCameraControl} that calls {@link CenterOnPosition()}
 * before the actual Arcball camera movement takes place. This way, the targeted node is
 * first smoothly brought into the centre along which Arcball is revolving, preventing
 * from sudden changes of view (and lost of focus from the user.
 *
 * @author Vladimir Ulman
 * @author Ulrik Guenther
 */


class AnimatedCenteringBeforeArcBallControl(
        val initAction: (Int, Int) -> Any,
        val scrollAction: (Double, Boolean, Int, Int) -> Any,
        name: String,
        n: () -> Camera?,
        w: Int,
        h: Int,
        target: () -> Vector3f
) : ArcballCameraControl(name, n, w, h, target) {
    private var lastX = -1
    private var lastY = -1
    private val rotationSpeed = 1.0f
    private val dampingFactor = 0.8f
    private var spherical = Spherical()
    private var sphericalDelta = Spherical()
    private var zoomFactor = 0.0f
    private val zoomSpeed = 0.2f
    private var isInitialClick = true

    override fun init(x: Int, y: Int) {
        initAction.invoke(x, y)

        lastX = x
        lastY = y

        cam?.targeted = true
        cam?.target = target.invoke()

        if (isInitialClick) {
            updateSpherical()
            isInitialClick = false
        } else {
            // For subsequent clicks, we'll set a small delta to trigger a smooth update
            sphericalDelta.theta = 0.001f
            sphericalDelta.phi = 0.001f
        }

        update()
    }

    override fun drag(x: Int, y: Int) {
        cam?.let { node ->
            if (!node.lock.tryLock()) {
                return
            }

            val deltaX = (x - lastX) * rotationSpeed * mouseSpeedMultiplier
            val deltaY = (y - lastY) * rotationSpeed * mouseSpeedMultiplier

            sphericalDelta.theta -= (2 * Math.PI * deltaX / cam!!.width).toFloat()
            sphericalDelta.phi -= (2 * Math.PI * deltaY / cam!!.height).toFloat()

            lastX = x
            lastY = y

            update()

            node.lock.unlock()
        }
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
        scrollAction.invoke(wheelRotation, isHorizontal, x, y)

        if (isHorizontal || cam == null) {
            return
        }

        zoomFactor -= wheelRotation.toFloat() * zoomSpeed
        update()
    }

    private fun updateSpherical() {
        cam?.let { camera ->
            val offset = camera.spatial().position - target.invoke()
            spherical.setFromVector3f(offset)
        }
    }

    private fun update() {
        spherical.theta += sphericalDelta.theta * dampingFactor
        spherical.phi += sphericalDelta.phi * dampingFactor

        spherical.phi = max(0.000001f, min(Math.PI.toFloat() - 0.000001f, spherical.phi))
        spherical.makeSafe()

        // Apply zoom
        spherical.radius *= Math.pow(0.95, zoomFactor.toDouble()).toFloat()
        spherical.radius = max(minimumDistance, min(maximumDistance, spherical.radius))

        val offset = spherical.toVector3f()
        val position = target.invoke() + offset

        cam?.let { camera ->
            // Smooth transition for position
            camera.spatial().position = camera.spatial().position.lerp(position, dampingFactor)

            // Smooth transition for rotation
            val targetRotation = calculateRotation(offset)
            camera.spatial().rotation = camera.spatial().rotation.slerp(targetRotation, dampingFactor)
        }

        sphericalDelta.theta *= (1f - dampingFactor)
        sphericalDelta.phi *= (1f - dampingFactor)
        zoomFactor *= (1f - dampingFactor)

        cam?.spatialOrNull()?.needsUpdateWorld = true
    }

    private fun calculateRotation(offset: Vector3f): Quaternionf {
        val targetToCamera = offset.normalize()
        val up = Vector3f(0f, 1f, 0f)
        val right = up.cross(targetToCamera, Vector3f())
        up.set(targetToCamera).cross(right)

        val rotationMatrix = Matrix4f().setLookAt(targetToCamera, Vector3f(0f), up)
        return Quaternionf().setFromNormalized(rotationMatrix)
    }

    private class Spherical(var radius: Float = 1f, var phi: Float = 0f, var theta: Float = 0f) {
        fun setFromVector3f(v: Vector3f) {
            radius = v.length()
            phi = Math.acos(v.y.toDouble() / radius).toFloat()
            theta = Math.atan2(v.z.toDouble(), v.x.toDouble()).toFloat()
        }

        fun makeSafe() {
            theta = max(-Math.PI.toFloat(), min(Math.PI.toFloat(), theta))
        }

        fun toVector3f(): Vector3f {
            val sinPhiRadius = Math.sin(phi.toDouble()) * radius
            return Vector3f(
                    (sinPhiRadius * Math.sin(theta.toDouble())).toFloat(),
                    (Math.cos(phi.toDouble()) * radius).toFloat(),
                    (sinPhiRadius * Math.cos(theta.toDouble())).toFloat()
            )
        }
    }
}