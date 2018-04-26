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
import net.imglib2.RealPositionable;
import org.joml.Vector3f;

/**
 * Created by kharrington on 1/18/18.
 */
public interface DVec3 extends RealLocalizable, RealPositionable {

//    default DVec3() {
//        this();
//    }
//
//    default DVec3(Vector3f source) {
//        System.out.println("constructor");
////        setPosition(source.get(0),0);
////        setPosition(source.get(1),1);
////        setPosition(source.get(2),2);
//    }

    @Override
    public default void localize(float[] position) {
        position[0] = getFloatPosition(0);
        position[1] = getFloatPosition(1);
        position[2] = getFloatPosition(2);
    }

    @Override
    public default void localize(double[] position) {
        position[0] = getDoublePosition(0);
        position[1] = getDoublePosition(1);
        position[2] = getDoublePosition(2);
    }

    @Override
    public default int numDimensions() {
        return 3;
    }

    @Override
    public default void setPosition(int position, int d) {
        setPosition(position,d);
    }

    @Override
    public default void setPosition(long position, int d) {
        setPosition(position,d);
    }

    @Override
    public default void setPosition(int[] position) {
        setPosition(position[0],0);
        setPosition(position[1],1);
        setPosition(position[2],2);
    }

    @Override
    public default void setPosition(long[] position) {
        setPosition(position[0],0);
        setPosition(position[1],1);
        setPosition(position[2],2);
    }

    @Override
    public default void move(Localizable localizable) {
        setPosition(localizable.getFloatPosition(0),0);
        setPosition(localizable.getFloatPosition(1),1);
        setPosition(localizable.getFloatPosition(2),2);
    }

    @Override
    public default void move(int[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public default void move(long[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public default void setPosition(Localizable localizable) {
        setPosition(localizable.getFloatPosition(0),0);
        setPosition(localizable.getFloatPosition(1),1);
        setPosition(localizable.getFloatPosition(2),2);
    }

    @Override
    public default void setPosition(double position, int d) {
        setPosition(position, d);
    }

    @Override
    public default void fwd(int d) {
        setPosition(getFloatPosition(d)+1,d);
    }

    @Override
    public default void bck(int d) {
        setPosition(getFloatPosition(d)-1,d);
    }

    @Override
    public default void move(int distance, int d) {
        setPosition(getFloatPosition(d)+distance,d);
    }

    @Override
    public default void move(long distance, int d) {
        setPosition(getFloatPosition(d)+distance,d);
    }

    @Override
    public default void move(float[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public default void move(double[] distance) {
        move(distance[0],0);
        move(distance[1],1);
        move(distance[2],2);
    }

    @Override
    public default void move(float distance, int d) {
        this.setPosition(distance,d);
    }

    @Override
    public default void move(double distance, int d) {
        move((float)distance, d);
    }

    @Override
    public default void setPosition(float[] position) {
        setPosition(position[0],0);
        setPosition(position[1],1);
        setPosition(position[2],2);
    }

    @Override
    public default void setPosition(double[] position) {
        setPosition(position[0],0);
        setPosition(position[1],1);
        setPosition(position[2],2);
    }

    @Override
    public default void setPosition(RealLocalizable localizable) {
        setPosition(localizable.getFloatPosition(0),0);
        setPosition(localizable.getFloatPosition(1),1);
        setPosition(localizable.getFloatPosition(2),2);
    }

    public default void setPosition(float x, float y, float z) {
        setPosition(x,0);
        setPosition(y,1);
        setPosition(z,2);
    }

}
