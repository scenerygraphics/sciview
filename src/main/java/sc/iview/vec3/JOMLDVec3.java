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

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;

import org.joml.Vector3f;

/**
 * Created by kharrington on 1/18/18.
 */
public class JOMLDVec3 extends Vector3f implements DVec3 {

    public JOMLDVec3(Vector3f source ) {
        setPosition(source.get(0),0);
        setPosition(source.get(1),1);
        setPosition(source.get(2),2);
    }

    @Override
    public void localize(float[] position) {
        position[0] = get(0);
        position[1] = get(1);
        position[2] = get(2);
    }

    @Override
    public void localize(double[] position) {
        position[0] = get(0);
        position[1] = get(1);
        position[2] = get(2);
    }

    @Override
    public float getFloatPosition(int d) {
        return get(d);
    }

    @Override
    public double getDoublePosition(int d) {
        return get(d);
    }

    @Override
    public void move(float distance, int d) {
        if( d == 0 ) set(this.add(distance,0,0));
        else if( d == 1 ) set(this.add(0, distance,0));
        else if( d == 2 ) set(this.add(0, 0, distance));
    }

    @Override
    public void move(double distance, int d) {
        move((float)distance, d);
    }

    @Override
    public void move(RealLocalizable localizable) {
        move(localizable.getFloatPosition(0),0);
        move(localizable.getFloatPosition(1),1);
        move(localizable.getFloatPosition(2),2);
    }

    @Override
    public void move(float[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public void move(double[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public void setPosition(RealLocalizable localizable) {
        set(localizable.getFloatPosition(0), localizable.getFloatPosition(1), localizable.getFloatPosition(2));
    }

    @Override
    public void setPosition(float[] position) {
        set(position[0], position[1], position[2]);
    }

    @Override
    public void setPosition(double[] position) {
        set(position[0], position[1], position[2]);
    }

    private void set(double v, double v1, double v2) {
        set(v, v1, v2);
    }

    @Override
    public void setPosition(float position, int d) {
        this.setComponent(d,position);
    }

    @Override
    public void setPosition(double position, int d) {
        setPosition(position, d);
    }

    @Override
    public void fwd(int d) {
        setPosition(getFloatPosition(d)+1,d);
    }

    @Override
    public void bck(int d) {
        setPosition(getFloatPosition(d)-1,d);
    }

    @Override
    public void move(int distance, int d) {
        setPosition(getFloatPosition(d)+distance,d);
    }

    @Override
    public void move(long distance, int d) {
        setPosition(getFloatPosition(d)+distance,d);
    }

    @Override
    public void move(Localizable localizable) {
        setPosition(localizable.getFloatPosition(0),0);
        setPosition(localizable.getFloatPosition(1),1);
        setPosition(localizable.getFloatPosition(2),2);
    }

    @Override
    public void move(int[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public void move(long[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public void setPosition(Localizable localizable) {
        setPosition(localizable.getFloatPosition(0),0);
        setPosition(localizable.getFloatPosition(1),1);
        setPosition(localizable.getFloatPosition(2),2);
    }

    @Override
    public void setPosition(int[] position) {
        setPosition(position[0],0);
        setPosition(position[1],1);
        setPosition(position[2],2);
    }

    @Override
    public void setPosition(long[] position) {
        setPosition(position[0],0);
        setPosition(position[1],1);
        setPosition(position[2],2);
    }

    @Override
    public void setPosition(int position, int d) {
        setPosition(position,d);
    }

    @Override
    public void setPosition(long position, int d) {
        setPosition(position,d);
    }

    @Override
    public int numDimensions() {
        return 3;
    }
}
