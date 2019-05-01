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
package sc.iview.vector;

/**
 * {@link Vector3} backed by three {@code double}s.
 * 
 * @author Curtis Rueden
 */
public class DoubleVector3 implements Vector3 {

    private double x, y, z;

    public DoubleVector3( double x, double y, double z ) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override public float xf() { return (float) x; }
    @Override public float yf() { return (float) y; }
    @Override public float zf() { return (float) z; }

    @Override public void setX( float position ) { x = position; }
    @Override public void setY( float position ) { y = position; }
    @Override public void setZ( float position ) { z = position; }

    @Override public double xd() { return x; }
    @Override public double yd() { return y; }
    @Override public double zd() { return z; }

    @Override public void setX( double position ) { x = position; }
    @Override public void setY( double position ) { y = position; }
    @Override public void setZ( double position ) { z = position; }

    @Override
    public Vector3 copy() {
        return new DoubleVector3(xd(),yd(),zd());
    }

    @Override
    public String toString() {
        return "[" + xd() + "; " + yd() + "; " + zd() + "]";
    }
}
