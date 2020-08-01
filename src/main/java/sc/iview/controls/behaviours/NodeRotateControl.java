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
package sc.iview.controls.behaviours;

import sc.iview.SciView;
import graphics.scenery.Node;
import org.joml.Quaternionf;
import org.scijava.ui.behaviour.DragBehaviour;

/**
 * Control behavior for rotating a Node
 *
 * @author Vladimir Ulman
 *
 */
public class NodeRotateControl implements DragBehaviour {

    protected final SciView sciView;
    private boolean firstEntered = true;
    private int lastX;
    private int lastY;

    public NodeRotateControl( SciView sciView) {
        this.sciView = sciView;
    }

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    @Override public void init( int x, int y ) {
        if (sciView.getActiveNode() == null) return;
        if (firstEntered) {
            lastX = x;
            lastY = y;
            firstEntered = false;
        }
    }

    @Override public void drag( int x, int y ) {
        final Node targetedNode = sciView.getActiveNode();
        if (targetedNode == null || !targetedNode.getLock().tryLock()) return;

        float frameYaw   = sciView.getMouseSpeed() * (x - lastX) * 0.0174533f; // 0.017 = PI/180
        float framePitch = sciView.getMouseSpeed() * (y - lastY) * 0.0174533f;

        new Quaternionf().rotateAxis(frameYaw, sciView.getCamera().getUp())
                .mul(targetedNode.getRotation(),targetedNode.getRotation())
                .normalize();
        new Quaternionf().rotateAxis(framePitch, sciView.getCamera().getRight())
                .mul(targetedNode.getRotation(),targetedNode.getRotation())
                .normalize();
        targetedNode.setNeedsUpdate(true);

        targetedNode.getLock().unlock();

        lastX = x;
        lastY = y;
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
    }
}
