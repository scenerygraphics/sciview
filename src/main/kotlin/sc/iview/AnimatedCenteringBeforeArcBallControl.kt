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
    protected var lastX = -1
    protected var lastY = -1

    override fun init(x: Int, y: Int) {
        initAction.invoke(x, y)

        lastX = x
        lastY = y

        cam?.targeted = true
        cam?.target = target.invoke()
    }

    override fun drag(x: Int, y: Int) {
        cam?.let { node ->
            if (!node.lock.tryLock()) {
                return
            }

            val xoffset: Float = (x - lastX).toFloat() * mouseSpeedMultiplier
            val yoffset: Float = (lastY - y).toFloat() * mouseSpeedMultiplier

            lastX = x
            lastY = y

            val frameYaw = (xoffset) / 180.0f * Math.PI.toFloat()
            val framePitch = yoffset / 180.0f * Math.PI.toFloat() * -1f

            // first calculate the total rotation quaternion to be applied to the camera
            val yawQ = Quaternionf().rotateXYZ(0.0f, frameYaw, 0.0f).normalize()
            val pitchQ = Quaternionf().rotateXYZ(framePitch, 0.0f, 0.0f).normalize()

            node.ifSpatial {
                distance = (target.invoke() - position).length()
                node.target = target.invoke()
                val currentRotation = rotation

                // Rotate pitch first, then yaw to ensure proper axis alignment
                rotation = pitchQ.mul(currentRotation).normalize()
                rotation = yawQ.mul(rotation).normalize()

                // Update position based on new rotation
                position = target.invoke() + node.forward * distance * (-1.0f)
            }

            node.lock.unlock()
        }
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
        scrollAction.invoke(wheelRotation, isHorizontal, x, y)

        if (isHorizontal || cam == null) {
            return
        }

        val sign = if (wheelRotation.toFloat() > 0) 1 else -1

        distance = (target.invoke() - cam!!.spatial().position).length()
        // This is the difference from scenery's scroll: we use a quadratic speed
        distance += sign * wheelRotation.toFloat() * wheelRotation.toFloat() * scrollSpeedMultiplier

        if (distance >= maximumDistance) distance = maximumDistance
        if (distance <= minimumDistance) distance = minimumDistance

        cam?.let { node -> node.spatialOrNull()?.position = target.invoke() + node.forward * distance * (-1.0f) }
    }
}
