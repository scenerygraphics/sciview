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
package sc.iview

import graphics.scenery.Camera
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
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
