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

import cleargl.GLVector;

/**
 * {@link Vector3} backed by a ClearGL {@link GLVector}.
 * @author Kyle Harrington
 */
public class ClearGLVector4 implements Vector4 {

    private GLVector source;

    public ClearGLVector4(float x, float y, float z, float w ) {
        this( new GLVector( x, y, z, w ) );
    }

    public ClearGLVector4(GLVector source ) {
        this.source = source;
    }

    public GLVector source() { return source; }

    @Override public float xf() { return source.get( 0 ); }
    @Override public float yf() { return source.get( 1 ); }
    @Override public float zf() { return source.get( 2 ); }
    @Override public float wf() { return source.get( 3 ); }

    @Override public void setX( float position ) { source.set( 0, position ); }
    @Override public void setY( float position ) { source.set( 1, position ); }
    @Override public void setZ( float position ) { source.set( 2, position ); }
    @Override public void setW( float position ) { source.set( 3, position ); }

    @Override
    public Vector4 copy() {
        return new ClearGLVector4(xf(),yf(),zf(),wf());
    }

    @Override
    public String toString() {
        return "[" + xf() + "; " + yf() + "; " + zf() + "; " + wf() + "]";
    }

    public static GLVector convert( Vector4 v ) {
        if( v instanceof ClearGLVector4) return ( (ClearGLVector4) v ).source();

        return new GLVector( v.xf(), v.yf(), v.zf(), v.wf() );
    }

    public static ClearGLVector4 parse(final String coords ) {
        String[] tokens = coords.split( ";" );
        if( tokens.length != 4 ) throw new IllegalArgumentException( "Four semicolon-separated numbers required." );
        try {
            return new ClearGLVector4( Float.parseFloat( tokens[0].trim() ), //
                                       Float.parseFloat( tokens[1].trim() ), //
                                       Float.parseFloat( tokens[2].trim() ), //
                                       Float.parseFloat( tokens[3].trim()) );
        } catch( NumberFormatException exc ) {
            throw new IllegalArgumentException( "Four comma-separated numbers required", exc );
        }
    }
}
