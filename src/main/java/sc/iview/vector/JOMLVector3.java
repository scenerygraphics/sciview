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
package sc.iview.vector;

import org.joml.Vector3f;

/**
 * {@link Vector3} backed by a JOML {@link Vector3f}.
 * 
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
public class JOMLVector3 implements Vector3 {

    private Vector3f source;

    public JOMLVector3( float x, float y, float z ) {
        this( new Vector3f( x, y, z ) );
    }

    public JOMLVector3( Vector3f source ) {
        this.source = source;
    }

    public Vector3f source() { return source; }

    @Override public float xf() { return source.x(); }
    @Override public float yf() { return source.y(); }
    @Override public float zf() { return source.z(); }

    @Override public void setX( float position ) { source.set( position, yf(), zf() ); }
    @Override public void setY( float position ) { source.set( xf(), position, zf() ); }
    @Override public void setZ( float position ) { source.set( xf(), yf(), position ); }

    @Override
    public Vector3 copy() {
        return new JOMLVector3(xf(),yf(),zf());
    }

    @Override
    public String toString() {
        return "[" + xf() + "; " + yf() + "; " + zf() + "]";
    }

    public static Vector3f convert( Vector3 v ) {
        if( v instanceof JOMLVector3 ) return (( JOMLVector3 ) v).source();
        return new Vector3f( v.xf(), v.yf(), v.zf() );
    }
}
