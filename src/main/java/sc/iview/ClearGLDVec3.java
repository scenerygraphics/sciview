package sc.iview;

import cleargl.GLVector;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import org.joml.Vector3f;

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
    public void localize(float[] position) {
        position[0] = glVector.get(0);
        position[1] = glVector.get(1);
        position[2] = glVector.get(2);
    }

    @Override
    public void localize(double[] position) {
        position[0] = glVector.get(0);
        position[1] = glVector.get(1);
        position[2] = glVector.get(2);
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
    public void move(float distance, int d) {
        this.setPosition(distance,d);
    }

    @Override
    public void move(double distance, int d) {
        move((float)distance, d);
    }

    @Override
    public void move(RealLocalizable localizable) {
        glVector = glVector.plus( new GLVector(localizable.getFloatPosition(0),
                                                localizable.getFloatPosition(1),
                                                localizable.getFloatPosition(2)) );
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
        float x, y, z;
        x = glVector.get(0); y = glVector.get(1); z = glVector.get(2);
        if( d == 0 ) x = position;
        else if( d == 1 ) y = position;
        else if( d == 2 ) z = position;
        glVector = new GLVector(x, y, z);
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
