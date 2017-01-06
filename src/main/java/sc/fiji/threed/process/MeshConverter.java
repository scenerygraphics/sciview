package sc.fiji.threed.process;

import net.imagej.ops.geom.geom3d.mesh.*;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import scenery.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * ImageJ OPs Mesh to Scenery Mesh converter
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * 		   Kyle Harrington, University of Idaho
 */
public class MeshConverter {
	public static scenery.Mesh getSceneryMesh(Mesh mesh)
	{
		scenery.Mesh scMesh;
		if (mesh != null) {
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

			scMesh.setVertices(BufferUtils.BufferUtils.allocateFloatAndPut(scVertices) );
			scMesh.setNormals( BufferUtils.BufferUtils.allocateFloatAndPut(scNormals) );
			return scMesh;
		}
		return null;
	}
	
	public static Mesh getOpsMesh( scenery.Mesh scMesh ) {		
		
		if( scMesh != null ) {
			DefaultMesh mesh = new DefaultMesh();
			
			//float[] scVertices = scMesh.getVertices().array();
			
			FloatBuffer verts = scMesh.getVertices();
			
			System.out.println( "Converting mesh a: initial has remaining: " + verts.hasRemaining() );
			
			// Flip if it looks like we're on the wrong side
			if( ! verts.hasRemaining() )
				verts.flip();
						
			//float[] scNormals = scMesh.getNormals().array();
			
			// rewrite to use scMesh.getVertices().get(index)									
			//for( int facetIdx = 0; facetIdx < scVertices.length/9; facetIdx++ ) {
			
			//System.out.println( "Converting mesh b: initial has remaining: " + verts.hasRemaining() );
			
			while( verts.hasRemaining() ) {

				Vertex[] triVerts = new Vertex[3];
				for( int vIdx = 0; vIdx < 3; vIdx++ ) 
					triVerts[vIdx] = new Vertex( verts.get(), verts.get(), verts.get() );
				TriangularFacet tri = new TriangularFacet( triVerts[0], triVerts[1], triVerts[2] );
				//tri.setNormal( new Vector3D( scNormals[offset], scNormals[offset+1], scNormals[offset+2] ) );
				//tri.getNormal();// Just do this to trigger a computeNormal and hope that it makes the right normal (^.-)				
				mesh.addFace(tri);
			}
			
			verts.flip();
						
			return mesh;
		}
		
		return null;		
	}

}
