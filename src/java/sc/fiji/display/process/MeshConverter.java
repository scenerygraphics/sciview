package sc.fiji.display.process;

import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccessibleRealInterval;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * ImageJ OPs Mesh to Scenery Mesh converter
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * 		   Kyle Harrington, University of Idaho
 */
public class MeshConverter {
    private Mesh mesh;
    scenery.Mesh scMesh;

    public MeshConverter(Mesh mesh)
    {
        this.mesh = mesh;
    }
    
    public scenery.Mesh getSceneryMesh()
    {

        if (scMesh == null && mesh != null) {
            int numDimension = 3;
            scMesh = new scenery.Mesh();

            List<Facet> facets = mesh.getFacets();
                        
            float[] scVertices = new float[facets.size() * 3 * numDimension];
            float[] scNormals = new float[facets.size() * 3 * numDimension];
            
            int count = 0;
            List<Vertex> vertices;
			for( Facet facet : facets ) {
            	TriangularFacet tri = (TriangularFacet) facet;
            	vertices = tri.getVertices();
            	Vector3D normal = tri.getNormal();
            	for( Vertex v : vertices ) {
	            	for( int d = 0; d < numDimension; d++ ) {
	            		scVertices[count] = (float) v.getDoublePosition(d);
	            		if( d == 0 ) scNormals[count] = (float) normal.getX();
	            		else if( d == 1 ) scNormals[count] = (float) normal.getY();
	            		else if( d == 2 ) scNormals[count] = (float) normal.getZ();
	            		count++;
	            	}
            	}
            }

			scMesh.setVertices( scVertices );
			scMesh.setNormals( scNormals );
        }
        return scMesh;
    }

}