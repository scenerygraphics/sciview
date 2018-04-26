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
package sc.iview.vec3;

import net.imglib2.RealLocalizable;

import org.joml.Vector3f;

import cleargl.GLVector;

/**
 * Created by kharrington on 1/18/18.
 */
public class ClearGLDVec3 implements DVec3 {

    private GLVector glVector;

    public ClearGLDVec3 set( GLVector v ) {
        glVector = v;
        return this;
    }

    public  GLVector get() {
        return glVector;
    }

    public ClearGLDVec3( final float x, final float y, final float z ) {
        glVector = new GLVector(x,y,z);
    }

    public ClearGLDVec3(Vector3f source ) {
        glVector = new GLVector(source.get(0),source.get(1),source.get(2));
    }

    @Override
    public float getFloatPosition(int d) {
        return glVector.get(d);
    }

    @Override
    public double getDoublePosition(int d) {
        return glVector.get(d);
    }

    @Override
    public void move(RealLocalizable localizable) {
        glVector = glVector.plus( new GLVector(localizable.getFloatPosition(0),
                                                localizable.getFloatPosition(1),
                                                localizable.getFloatPosition(2)) );
    }

    @Override
    public void setPosition(float position, int d) {
        float x, y, z;
        x = glVector.get(0); y = glVector.get(1); z = glVector.get(2);
        if( d == 0 ) x = position;
        else if( d == 1 ) y = position;
        else if( d == 2 ) z = position;
        glVector = new GLVector(x, y, z);
    }
}
