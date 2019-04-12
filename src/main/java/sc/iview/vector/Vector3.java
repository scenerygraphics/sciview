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

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

/**
 * Interface for 3D vectors.
 * 
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
public interface Vector3 extends RealLocalizable, RealPositionable {

    // -- Vector3 methods --

    float xf();
    float yf();
    float zf();

    default void moveX(float distance) { setX( xf() + distance ); }
    default void moveY(float distance) { setY( yf() + distance ); }
    default void moveZ(float distance) { setZ( zf() + distance ); }
    default void move( float xDist, float yDist, float zDist ) {
        moveX( xDist );
        moveY( yDist );
        moveZ( zDist );
    }

    void setX(float position);
    void setY(float position);
    void setZ(float position);

    default void setPosition( float x, float y, float z ) {
        setX( x );
        setY( y );
        setZ( z );
    }

    default double xd() { return xf(); }
    default double yd() { return yf(); }
    default double zd() { return zf(); }

    default void moveX(double distance) { setX( xd() + distance ); }
    default void moveY(double distance) { setY( yd() + distance ); }
    default void moveZ(double distance) { setZ( zd() + distance ); }
    default void move( double xDist, double yDist, double zDist ) {
        moveX( xDist );
        moveY( yDist );
        moveZ( zDist );
    }

    default void setX(double position) { setX((float) position); }
    default void setY(double position) { setY((float) position); }
    default void setZ(double position) { setZ((float) position); }

    default void setPosition( double x, double y, double z ) {
        setX( x );
        setY( y );
        setZ( z );
    }

    // -- RealLocalizable methods --

    @Override
    default void localize( float[] position ) {
        position[0] = xf();
        position[1] = yf();
        position[2] = zf();
    }

    @Override
    default void localize( double[] position ) {
        position[0] = xd();
        position[1] = yd();
        position[2] = zd();
    }

    @Override
	default float getFloatPosition( int d ) {
        if( d == 0 ) return xf();
        if( d == 1 ) return yf();
        if( d == 2 ) return zf();
        throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
	default double getDoublePosition( int d ) {
        if( d == 0 ) return xd();
        if( d == 1 ) return yd();
        if( d == 2 ) return zd();
        throw new IndexOutOfBoundsException( "" + d );
    }

    // -- RealPositionable methods --

    @Override
    default void move( float distance, int d ) {
        if( d == 0 ) moveX( distance );
        else if( d == 1 ) moveY( distance );
        else if( d == 2 ) moveZ( distance );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
    default void move( double distance, int d ) {
        if( d == 0 ) moveX( distance );
        else if( d == 1 ) moveY( distance );
        else if( d == 2 ) moveZ( distance );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
	default void move( RealLocalizable distance ) {
        moveX( distance.getDoublePosition( 0 ) );
        moveY( distance.getDoublePosition( 1 ) );
        moveZ( distance.getDoublePosition( 2 ) );
    }

    @Override
    default void move( float[] distance ) {
        moveX( distance[0] );
        moveY( distance[1] );
        moveZ( distance[2] );
    }

    @Override
    default void move( double[] distance ) {
        moveX( distance[0] );
        moveY( distance[1] );
        moveZ( distance[2] );
    }

    @Override
    default void setPosition( RealLocalizable localizable ) {
        setX( localizable.getDoublePosition( 0 ) );
        setY( localizable.getDoublePosition( 1 ) );
        setZ( localizable.getDoublePosition( 2 ) );
    }

    @Override
    default void setPosition( float[] position ) {
        setX( position[0] );
        setY( position[1] );
        setZ( position[2] );
    }

    @Override
    default void setPosition( double[] position ) {
        setX( position[0] );
        setY( position[1] );
        setZ( position[2] );
    }

    @Override
    default void setPosition( float position, int d ) {
        if( d == 0 ) setX( position );
        else if( d == 1 ) setY( position );
        else if( d == 2 ) setZ( position );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
    default void setPosition( double position, int d ) {
        if( d == 0 ) setX( position );
        else if( d == 1 ) setY( position );
        else if( d == 2 ) setZ( position );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    // -- Positionable methods --

    @Override
    default void fwd( int d ) { move( 1, d ); }
    @Override
    default void bck( int d ) { move( 1, d ); }

    @Override
    default void move( int distance, int d ) {
        move( ( double ) distance, d );
    }

    @Override
    default void move( long distance, int d ) {
        move( ( double ) distance, d );
    }

    @Override
    default void move( Localizable distance ) {
        moveX( distance.getDoublePosition( 0 ) );
        moveY( distance.getDoublePosition( 1 ) );
        moveZ( distance.getDoublePosition( 2 ) );
    }

    @Override
    default void move( int[] distance ) {
        moveX( (double) distance[0] );
        moveY( (double) distance[1] );
        moveZ( (double) distance[2] );
    }

    @Override
    default void move( long[] distance ) {
        moveX( (double) distance[0] );
        moveY( (double) distance[1] );
        moveZ( (double) distance[2] );
    }

    @Override
    default void setPosition( Localizable localizable ) {
        setX( localizable.getDoublePosition( 0 ) );
        setY( localizable.getDoublePosition( 1 ) );
        setZ( localizable.getDoublePosition( 2 ) );
    }

    @Override
    default void setPosition( int[] position ) {
        setX( (double) position[0] );
        setY( (double) position[1] );
        setZ( (double) position[2] );
    }

    @Override
    default void setPosition( long[] position ) {
        setX( (double) position[0] );
        setY( (double) position[1] );
        setZ( (double) position[2] );
    }

    @Override
    default void setPosition( int position, int d ) {
        setPosition( ( double ) position, d );
    }

    @Override
    default void setPosition( long position, int d ) {
        setPosition( ( double ) position, d );
    }

    // -- EuclideanSpace methods --

    @Override
    default int numDimensions() { return 3; }

    // Extra convenience methods
    default double getLength() {
        return Math.sqrt( getDoublePosition(0) * getDoublePosition(0) + getDoublePosition(1) * getDoublePosition(1) + getDoublePosition(2) * getDoublePosition(2) );
    }

    default Vector3 minus(Vector3 p2) {
        Vector3 result = this.copy();
        result.moveX(-p2.getDoublePosition(0));
        result.moveY(-p2.getDoublePosition(1));
        result.moveZ(-p2.getDoublePosition(2));
        return result;
    }

    Vector3 copy();
}
