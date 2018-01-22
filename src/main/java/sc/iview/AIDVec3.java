package sc.iview;

import net.imglib2.RealLocalizable;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIVector3D;

import java.nio.ByteBuffer;

/**
 * Created by kharrington on 1/21/18.
 */
public class AIDVec3 implements DVec3 {

    private AIVector3D aiVector3D;

    public static AIDVec3 convert( AIVector3D v ) {
        AIDVec3 aidVec3 = new AIDVec3(v.x(),v.y(),v.z());
        return aidVec3;
    }

    public AIDVec3 set( AIVector3D v ) {
        aiVector3D = v;
        return this;
    }

    public  AIVector3D get() {
        return aiVector3D;
    }

    public AIDVec3( final float x, final float y, final float z ) {
        aiVector3D = AIVector3D.create();
        aiVector3D.x(x);
        aiVector3D.y(y);
        aiVector3D.z(z);
    }

    public AIDVec3(Vector3f source ) {
        //aiVector3D = new AIVector3D(source.get(0),source.get(1),source.get(2));
        aiVector3D = AIVector3D.create();
        setPosition( source.x(), source.y(), source.z() );
    }

    @Override
    public float getFloatPosition(int d) {
        if( d == 0 ) return aiVector3D.x();
        else if( d == 1 ) return aiVector3D.y();
        else if( d == 2 ) return aiVector3D.z();
        return Float.NaN;
    }

    @Override
    public double getDoublePosition(int d) {
        return getFloatPosition(d);
    }

    @Override
    public void move(RealLocalizable localizable) {
        aiVector3D.x( aiVector3D.x() + localizable.getFloatPosition(0) );
        aiVector3D.x( aiVector3D.y() + localizable.getFloatPosition(1) );
        aiVector3D.x( aiVector3D.z() + localizable.getFloatPosition(2) );
    }

    @Override
    public void setPosition(float position, int d) {
        if( d == 0 ) aiVector3D.x( position );
        else if( d == 1 ) aiVector3D.y( position );
        else if( d == 2 ) aiVector3D.z( position );
    }
}
