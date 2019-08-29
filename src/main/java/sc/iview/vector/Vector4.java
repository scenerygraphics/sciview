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
 * Interface for 4D vectors.
 * 
 * @author Kyle Harrington
 */
public interface Vector4 extends RealLocalizable, RealPositionable {

    // -- Vector3 methods --

    float xf();
    float yf();
    float zf();
    float wf();

    default void moveX(float distance) { setX( xf() + distance ); }
    default void moveY(float distance) { setY( yf() + distance ); }
    default void moveZ(float distance) { setZ( zf() + distance ); }
    default void moveW(float distance) { setZ( wf() + distance ); }
    default void move(float xDist, float yDist, float zDist, float wDist) {
        moveX( xDist );
        moveY( yDist );
        moveZ( zDist );
        moveW( wDist );
    }

    void setX(float position);
    void setY(float position);
    void setZ(float position);
    void setW(float position);

    default void setPosition(float x, float y, float z, float w) {
        setX( x );
        setY( y );
        setZ( z );
        setW( w );
    }

    default double xd() { return xf(); }
    default double yd() { return yf(); }
    default double zd() { return zf(); }
    default double wd() { return wf(); }

    default void moveX(double distance) { setX( xd() + distance ); }
    default void moveY(double distance) { setY( yd() + distance ); }
    default void moveZ(double distance) { setZ( zd() + distance ); }
    default void moveW(double distance) { setW( wd() + distance ); }
    default void move(double xDist, double yDist, double zDist, double wDist) {
        moveX( xDist );
        moveY( yDist );
        moveZ( zDist );
        moveW( wDist );
    }

    default void setX(double position) { setX((float) position); }
    default void setY(double position) { setY((float) position); }
    default void setZ(double position) { setZ((float) position); }
    default void setW(double position) { setW((float) position); }

    default void setPosition(double x, double y, double z, double w) {
        setX( x );
        setY( y );
        setZ( z );
        setW( w );
    }

    // -- RealLocalizable methods --

    @Override
    default void localize(float[] position) {
        position[0] = xf();
        position[1] = yf();
        position[2] = zf();
        position[3] = wf();
    }

    @Override
    default void localize(double[] position) {
        position[0] = xd();
        position[1] = yd();
        position[2] = zd();
        position[3] = wd();
    }

    @Override
	default float getFloatPosition(int d) {
        if( d == 0 ) return xf();
        if( d == 1 ) return yf();
        if( d == 2 ) return zf();
        if( d == 3 ) return wf();
        throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
	default double getDoublePosition(int d) {
        if( d == 0 ) return xd();
        if( d == 1 ) return yd();
        if( d == 2 ) return zd();
        if( d == 3 ) return wd();
        throw new IndexOutOfBoundsException( "" + d );
    }

    // -- RealPositionable methods --

    @Override
    default void move(float distance, int d) {
        if( d == 0 ) moveX( distance );
        else if( d == 1 ) moveY( distance );
        else if( d == 2 ) moveZ( distance );
        else if( d == 3 ) moveW( distance );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
    default void move(double distance, int d) {
        if( d == 0 ) moveX( distance );
        else if( d == 1 ) moveY( distance );
        else if( d == 2 ) moveZ( distance );
        else if( d == 3 ) moveW( distance );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
	default void move(RealLocalizable distance) {
        moveX( distance.getDoublePosition( 0 ) );
        moveY( distance.getDoublePosition( 1 ) );
        moveZ( distance.getDoublePosition( 2 ) );
        moveW( distance.getDoublePosition( 3 ) );
    }

    @Override
    default void move(float[] distance) {
        moveX( distance[0] );
        moveY( distance[1] );
        moveZ( distance[2] );
        moveW( distance[3] );
    }

    @Override
    default void move(double[] distance) {
        moveX( distance[0] );
        moveY( distance[1] );
        moveZ( distance[2] );
        moveW( distance[3] );
    }

    @Override
    default void setPosition(RealLocalizable localizable) {
        setX( localizable.getDoublePosition( 0 ) );
        setY( localizable.getDoublePosition( 1 ) );
        setZ( localizable.getDoublePosition( 2 ) );
        setW( localizable.getDoublePosition( 3 ) );
    }

    @Override
    default void setPosition(float[] position) {
        setX( position[0] );
        setY( position[1] );
        setZ( position[2] );
        setW( position[3] );
    }

    @Override
    default void setPosition(double[] position) {
        setX( position[0] );
        setY( position[1] );
        setZ( position[2] );
        setW( position[3] );
    }

    @Override
    default void setPosition(float position, int d) {
        if( d == 0 ) setX( position );
        else if( d == 1 ) setY( position );
        else if( d == 2 ) setZ( position );
        else if( d == 3 ) setW( position );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    @Override
    default void setPosition(double position, int d) {
        if( d == 0 ) setX( position );
        else if( d == 1 ) setY( position );
        else if( d == 2 ) setZ( position );
        else if( d == 3 ) setW( position );
        else throw new IndexOutOfBoundsException( "" + d );
    }

    // -- Positionable methods --

    @Override
    default void fwd(int d) { move( 1, d ); }
    @Override
    default void bck(int d) { move( 1, d ); }

    @Override
    default void move(int distance, int d) {
        move( ( double ) distance, d );
    }

    @Override
    default void move(long distance, int d) {
        move( ( double ) distance, d );
    }

    @Override
    default void move(Localizable distance) {
        moveX( distance.getDoublePosition( 0 ) );
        moveY( distance.getDoublePosition( 1 ) );
        moveZ( distance.getDoublePosition( 2 ) );
        moveW( distance.getDoublePosition( 3 ) );
    }

    @Override
    default void move(int[] distance) {
        moveX( (double) distance[0] );
        moveY( (double) distance[1] );
        moveZ( (double) distance[2] );
        moveW( (double) distance[3] );
    }

    @Override
    default void move(long[] distance) {
        moveX( (double) distance[0] );
        moveY( (double) distance[1] );
        moveZ( (double) distance[2] );
        moveW( (double) distance[3] );
    }

    @Override
    default void setPosition(Localizable localizable) {
        setX( localizable.getDoublePosition( 0 ) );
        setY( localizable.getDoublePosition( 1 ) );
        setZ( localizable.getDoublePosition( 2 ) );
        setW( localizable.getDoublePosition( 3 ) );
    }

    @Override
    default void setPosition(int[] position) {
        setX( (double) position[0] );
        setY( (double) position[1] );
        setZ( (double) position[2] );
        setW( (double) position[3] );
    }

    @Override
    default void setPosition(long[] position) {
        setX( (double) position[0] );
        setY( (double) position[1] );
        setZ( (double) position[2] );
        setW( (double) position[3] );
    }

    @Override
    default void setPosition(int position, int d) {
        setPosition( ( double ) position, d );
    }

    @Override
    default void setPosition(long position, int d) {
        setPosition( ( double ) position, d );
    }

    // -- EuclideanSpace methods --

    @Override
    default int numDimensions() { return 3; }

    // Extra convenience methods
    default double getLength() {
        return Math.sqrt( getDoublePosition(0) * getDoublePosition(0) + //
                          getDoublePosition(1) * getDoublePosition(1) + //
                          getDoublePosition(2) * getDoublePosition(2) + //
                          getDoublePosition(3) * getDoublePosition(3) );
    }

    default Vector4 add(Vector4 p2) {
        Vector4 result = this.copy();
        result.moveX(p2.getDoublePosition(0));
        result.moveY(p2.getDoublePosition(1));
        result.moveZ(p2.getDoublePosition(2));
        result.moveW(p2.getDoublePosition(3));
        return result;
    }

    default Vector4 minus(Vector4 p2) {
        Vector4 result = this.copy();
        result.moveX(-p2.getDoublePosition(0));
        result.moveY(-p2.getDoublePosition(1));
        result.moveZ(-p2.getDoublePosition(2));
        result.moveW(-p2.getDoublePosition(3));
        return result;
    }

    default Vector4 multiply(float s) {
        Vector4 result = this.copy();
        result.setPosition( result.getDoublePosition(0) * s, 0 );
        result.setPosition( result.getDoublePosition(1) * s, 1 );
        result.setPosition( result.getDoublePosition(2) * s, 2 );
        result.setPosition( result.getDoublePosition(3) * s, 3 );
        return result;
    }

    default float[] asFloatArray() {
        float[] a = new float[4];
        a[0] = xf();
        a[1] = yf();
        a[2] = zf();
        a[3] = wf();
        return a;
    }

    default double[] asDoubleArray() {
        double[] a = new double[4];
        a[0] = xd();
        a[1] = yd();
        a[2] = zd();
        a[3] = wd();
        return a;
    }

    default Vector4 cross(Vector4 v2) {
        JOMLVector4 v = new JOMLVector4(JOMLVector4.convert(this));
        return v.cross(new JOMLVector4(JOMLVector4.convert(v2)));
    }

    default Vector4 elmul(Vector4 v2) {
        Vector4 r = this.copy();
        r.setX( r.xf() * v2.xf() );
        r.setY( r.yf() * v2.yf() );
        r.setZ( r.zf() * v2.zf() );
        r.setW( r.wf() * v2.wf() );
        return r;
    }

    default float dot(Vector4 v2) {
        return ( this.xf() * v2.xf() + this.yf() * v2.yf() + this.zf() * v2.zf() + this.wf() * v2.wf() );
    }

    default Vector4 normalize() {
        Vector4 r = this.copy();
        double f = 1 / this.getLength();
        r.setX(r.xf() * f);
        r.setY(r.yf() * f);
        r.setZ(r.zf() * f);
        r.setW(r.wf() * f);
        return r;
    }

    Vector4 copy();
}
