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

import org.joml.Vector3f
import org.scijava.ui.behaviour.DragBehaviour
import org.scijava.ui.behaviour.ScrollBehaviour
import sc.iview.SciView

/**
 * Control behavior for moving a Node
 *
 * @author Kyle Harrington
 */
class NodeTranslateControl(protected val sciView: SciView) : DragBehaviour, ScrollBehaviour {
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
        val targetedNode = sciView.activeNode;
        val cam = sciView.camera ?: return
        if (targetedNode == null || !targetedNode.lock.tryLock()) return

        cam.right.mul((x - lastX) * sciView.getFPSSpeedSlow() * sciView.getMouseSpeed(), dragPosUpdater )
        targetedNode.position.add( dragPosUpdater )
        cam.up.mul(   (lastY - y) * sciView.getFPSSpeedSlow() * sciView.getMouseSpeed(), dragPosUpdater )
        targetedNode.position.add( dragPosUpdater )
        targetedNode.needsUpdate = true

        targetedNode.lock.unlock()

        lastX = x
        lastY = y
    }

    override fun end(x: Int, y: Int) {
    }

    override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
        val targetedNode = sciView.activeNode;
        val cam = sciView.camera ?: return
        if (targetedNode == null || !targetedNode.lock.tryLock()) return

        cam.forward.mul( wheelRotation.toFloat() * sciView.getFPSSpeedSlow() * sciView.getMouseSpeed(), scrollPosUpdater )
        targetedNode.position.add( scrollPosUpdater );
        targetedNode.needsUpdate = true

        targetedNode.lock.unlock()
    }

    //aux vars to prevent from re-creating them over and over
    private val dragPosUpdater: Vector3f = Vector3f()
    private val scrollPosUpdater: Vector3f = Vector3f()
}
