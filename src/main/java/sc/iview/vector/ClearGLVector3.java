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
 * @author Curtis Rueden
 */
public class ClearGLVector3 implements Vector3 {

    private GLVector source;

    public ClearGLVector3( float x, float y, float z ) {
        this( new GLVector( x, y, z ) );
    }

    public ClearGLVector3( GLVector source ) {
        this.source = source;
    }

    public GLVector source() { return source; }

    @Override public float xf() { return source.get( 0 ); }
    @Override public float yf() { return source.get( 1 ); }
    @Override public float zf() { return source.get( 2 ); }

    @Override public void setX( float position ) { source.set( 0, position ); }
    @Override public void setY( float position ) { source.set( 1, position ); }
    @Override public void setZ( float position ) { source.set( 2, position ); }

    @Override
    public String toString() {
        return "[" + xf() + "; " + yf() + "; " + zf() + "]";
    }

    public static GLVector convert( Vector3 v ) {
        if( v instanceof ClearGLVector3 ) return ( ( ClearGLVector3 ) v ).source();
        return new GLVector( v.xf(), v.yf(), v.zf() );
    }

    public static ClearGLVector3 parse( final String coords ) {
        String[] tokens = coords.split( ";" );
        if( tokens.length != 3 ) throw new IllegalArgumentException( "Three semicolon-separated numbers required." );
        try {
            return new ClearGLVector3( Float.parseFloat( tokens[0].trim() ), //
                                       Float.parseFloat( tokens[1].trim() ), //
                                       Float.parseFloat( tokens[2].trim() ) );
        } catch( NumberFormatException exc ) {
            throw new IllegalArgumentException( "Three comma-separated numbers required", exc );
        }
    }
}
