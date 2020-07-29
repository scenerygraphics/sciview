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

import cleargl.GLVector;
import com.jogamp.opengl.math.Quaternion;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.scijava.ui.behaviour.DragBehaviour;
import sc.iview.SciView;

/**
 * Behavior for translating a camera
 *
 * @author Kyle Harrington
 *
 */
public class CameraTranslateControl implements DragBehaviour {
    protected SciView sciView;
    private boolean firstEntered;
    private int lastX;
    private int lastY;

    public float getDragSpeed() {
        return dragSpeed;
    }

    public void setDragSpeed( float dragSpeed ) {
        this.dragSpeed = dragSpeed;
    }

    protected float dragSpeed;

    public CameraTranslateControl( SciView sciView, float dragSpeed ) {
        this.sciView = sciView;
        this.dragSpeed = dragSpeed;
    }

    /**
     * This function is called upon mouse down and initializes the camera control
     * with the current window size.
     *
     * x position in window
     * y position in window
     */
    @Override public void init( int x, int y ) {
        if (firstEntered) {
            lastX = x;
            lastY = y;
            firstEntered = false;
        }
    }

    @Override public void drag( int x, int y ) {

        if(!sciView.getCamera().getLock().tryLock()) {
            return;
        }

        Vector3f translationVector = new Vector3f((x - lastX) * getDragSpeed(), (y - lastY) * getDragSpeed(), 0);

        ( new Quaternionf( sciView.getCamera().getRotation() ) ).conjugate().transform( translationVector );
        translationVector.y *= -1;
        sciView.getCamera().setPosition( sciView.getCamera().getPosition().add( new Vector3f( translationVector.x(), translationVector.y(), translationVector.z() ) ) );

        sciView.getCamera().getLock().unlock();
    }

    @Override public void end( int x, int y ) {
        firstEntered = true;
    }
}
