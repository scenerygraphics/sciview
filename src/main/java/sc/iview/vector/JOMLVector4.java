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

import org.joml.Vector4f;

/**
 * {@link Vector4} backed by a JOML {@link Vector4f}.
 * 
 * @author Kyle Harrington
 */
public class JOMLVector4 implements Vector4 {

    private Vector4f source;

    public JOMLVector4(float x, float y, float z, float w ) {
        this( new Vector4f( x, y, z, w ) );
    }

    public JOMLVector4(Vector4f source ) {
        this.source = source;
    }

    public Vector4f source() { return source; }

    @Override public float xf() { return source.x(); }
    @Override public float yf() { return source.y(); }
    @Override public float zf() { return source.z(); }
    @Override public float wf() { return source.w(); }

    @Override public void setX( float position ) { source.set( position, yf(), zf(), wf() ); }
    @Override public void setY( float position ) { source.set( xf(), position, zf(), wf() ); }
    @Override public void setZ( float position ) { source.set( xf(), yf(), position, wf() ); }
    @Override public void setW( float position ) { source.set( xf(), yf(), zf(), position ); }

    @Override
    public Vector4 copy() {
        return new JOMLVector4(xf(),yf(),zf(),wf());
    }

    @Override
    public String toString() {
        return "[" + xf() + "; " + yf() + "; " + zf() + "; " + wf() + "]";
    }

    public static Vector4f convert( Vector4 v ) {
        if( v instanceof JOMLVector4) return ((JOMLVector4) v).source();
        return new Vector4f( v.xf(), v.yf(), v.zf(), v.wf() );
    }
}
