/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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
import graphics.scenery.Camera;
import org.joml.Quaternionf;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;

public class SceneRollControl implements ClickBehaviour, DragBehaviour
{
    protected final SciView sciView;

    public SceneRollControl(final SciView sciView, final float byFixedAngInRad) {
        this.sciView = sciView;
        rotQ_CW  = new Quaternionf().rotateAxis(+byFixedAngInRad,0,0,-1);
        rotQ_CCW = new Quaternionf().rotateAxis(-byFixedAngInRad,0,0,-1);
    }

    final Quaternionf rotQ_CW, rotQ_CCW;

    @Override
    public void click(int x, int y) {
        final Camera cam = sciView.getCamera();
        rotQ_CW.mul(cam.getRotation(),cam.getRotation()).normalize();
    }

    private final int minMouseMovementDelta = 2;
    private int lastX;

    @Override
    public void init(int x, int y) {
        lastX = x;
    }

    @Override
    public void drag(int x, int y) {
        final Camera cam = sciView.getCamera();
        if (x > lastX+minMouseMovementDelta)
            rotQ_CW.mul(cam.getRotation(),cam.getRotation()).normalize();
        else if(x < lastX-minMouseMovementDelta)
            rotQ_CCW.mul(cam.getRotation(),cam.getRotation()).normalize();
        lastX = x;
    }

    @Override
    public void end(int x, int y) {
    }
}
