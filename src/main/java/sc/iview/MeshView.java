package sc.iview;

import cleargl.GLVector;
import graphics.scenery.BufferUtils;
import graphics.scenery.Mesh;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.nio.FloatBuffer;
import java.util.List;


/**
 * Created by kharrington on 6/21/17.
 */
public class MeshView extends Mesh {

    private RealLocalizable center;

    MeshView( net.imagej.ops.geom.geom3d.mesh.Mesh mesh ) {

        int numDimension = 3;

        List<Facet> facets = mesh.getFacets();

        float[] scVertices = new float[facets.size() * 3 * numDimension];
        float[] scNormals = new float[facets.size() * 3 * numDimension];

        float[] boundingBox = new float[]{Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};

        int count = 0;
        List<Vertex> vertices;
        for( Facet facet : facets ) {
            TriangularFacet tri = (TriangularFacet) facet;
            vertices = tri.getVertices();
            Vector3D normal = tri.getNormal();
            for( Vertex v : vertices ) {
                for( int d = 0; d < numDimension; d++ ) {
                    scVertices[count] = (float) v.getDoublePosition(d);

                    if( scVertices[count] < boundingBox[d] )// min
                        boundingBox[d] = scVertices[count];

                    if( scVertices[count] > boundingBox[d+3] )// min
                        boundingBox[d+3] = scVertices[count];

                    if( d == 0 ) scNormals[count] = (float) normal.getX();
                    else if( d == 1 ) scNormals[count] = (float) normal.getY();
                    else if( d == 2 ) scNormals[count] = (float) normal.getZ();
                    count++;
                }
            }
        }

        updateCenter();

        setVertices(BufferUtils.BufferUtils.allocateFloatAndPut(scVertices) );
        setNormals( BufferUtils.BufferUtils.allocateFloatAndPut(scNormals) );
        setTexcoords(BufferUtils.BufferUtils.allocateFloat(0));
        setIndices(BufferUtils.BufferUtils.allocateInt(0));

        setBoundingBoxCoords(boundingBox);
        setDirty(true);
        setScale(new GLVector(0.1f, 0.1f, 0.1f));
    }

    public net.imagej.ops.geom.geom3d.mesh.Mesh getOpsMesh(Mesh scMesh ) {

        DefaultMesh mesh = new DefaultMesh();

        FloatBuffer verts = getVertices();

        // Flip if it looks like we're on the wrong side
        if( ! verts.hasRemaining() )
            verts.flip();

        while( verts.hasRemaining() ) {

            Vertex[] triVerts = new Vertex[3];
            for( int vIdx = 0; vIdx < 3; vIdx++ )
                triVerts[vIdx] = new Vertex( verts.get(), verts.get(), verts.get() );
            TriangularFacet tri = new TriangularFacet( triVerts[0], triVerts[1], triVerts[2] );
            mesh.addFace(tri);
        }

        verts.flip();

        return mesh;
    }

    public RealLocalizable updateCenter() {
        RealPoint p = new RealPoint( 0, 0, 0 );

        FloatBuffer verts = getVertices();

        // Flip if it looks like we're on the wrong side
        if( ! verts.hasRemaining() )
            verts.flip();

        int vertCount = 0;
        while( verts.hasRemaining() ) {
            for( int vIdx = 0; vIdx < 3; vIdx++ )
                p.move(verts.get(),vIdx);
            vertCount++;
        }

        verts.flip();

        for( int d = 0; d < 3; d++ ) {
            p.setPosition( p.getDoublePosition(d)/vertCount, d );
        }

        center = p;

        return center;
    }

    public RealLocalizable getCenter() {
        return center;
    }

    public void setCenter(RealLocalizable center) {
        this.center = center;
    }

}
