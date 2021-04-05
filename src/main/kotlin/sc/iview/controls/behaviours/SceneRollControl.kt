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
package sc.iview.controls.behaviours

import org.joml.Quaternionf
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Control behavior for rolling a scene (along camera's forward axis)
 *
 * @author Vladimir Ulman
 */
class SceneRollControl(protected val sciView: SciView, protected val byFixedAngInRad: Float) : ClickBehaviour, DragBehaviour {
    private val rotQ_CW: Quaternionf = Quaternionf().rotateAxis(+byFixedAngInRad, 0f, 0f, -1f);
    private val rotQ_CCW: Quaternionf = Quaternionf().rotateAxis(-byFixedAngInRad, 0f, 0f, -1f);

    override fun click(x: Int, y: Int) {
        val cam = sciView.camera ?: return
        rotQ_CW.mul(cam.rotation, cam.rotation).normalize()
    }

    private val minMouseMovementDelta = 2
    private var lastX = 0

    override fun init(x: Int, y: Int) {
        lastX = x
    }

    override fun drag(x: Int, y: Int) {
        val cam = sciView.camera ?: return
        if (x > lastX + minMouseMovementDelta) rotQ_CW.mul(cam.rotation, cam.rotation).normalize()
        else if (x < lastX - minMouseMovementDelta) rotQ_CCW.mul(cam.rotation, cam.rotation).normalize()
        lastX = x
    }

    override fun end(x: Int, y: Int) {
    }
}
