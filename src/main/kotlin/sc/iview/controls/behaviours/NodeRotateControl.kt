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
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Control behavior for rotating a Node
 *
 * @author Vladimir Ulman
 */
class NodeRotateControl(protected val sciView: SciView) : DragBehaviour {
    private var lastX = 0
    private var lastY = 0

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    override fun init(x: Int, y: Int) {
        if (sciView.activeNode == null) return

        lastX = x
        lastY = y
    }

    override fun drag(x: Int, y: Int) {
        val targetedNode = sciView.activeNode
        val cam = sciView.camera ?: return
        if (targetedNode == null || !targetedNode.lock.tryLock()) return

        val frameYaw   = sciView.getMouseSpeed() * (x - lastX) * 0.0174533f // 0.017 = PI/180
        val framePitch = sciView.getMouseSpeed() * (y - lastY) * 0.0174533f

        Quaternionf().rotateAxis(frameYaw, cam.up)
                .mul(targetedNode.spatialOrNull()?.rotation, targetedNode.spatialOrNull()?.rotation)
                .normalize()
        Quaternionf().rotateAxis(framePitch, cam.right)
                .mul(targetedNode.spatialOrNull()?.rotation, targetedNode.spatialOrNull()?.rotation)
                .normalize()
        targetedNode.spatialOrNull()?.needsUpdate = true

        targetedNode.lock.unlock()

        lastX = x
        lastY = y
    }

    override fun end(x: Int, y: Int) {
    }
}
