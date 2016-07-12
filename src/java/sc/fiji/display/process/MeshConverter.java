package sc.fiji.display.process;

import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccessibleRealInterval;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * ImageJ OPs Mesh to Scenery Mesh converter
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * 		   Kyle Harrington, University of Idaho
 */
public class MeshConverter {
	//private Mesh mesh;
	//scenery.Mesh scMesh;

	//public MeshConverter(Mesh mesh)
	//{
	//    this.mesh = mesh;
	//}

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
				Vertex normal = tri.getNormal();
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
			return scMesh;
		}
		return null;
	}
	
	public static Mesh getOpsMesh( scenery.Mesh scMesh ) {		
		
		if( scMesh != null ) {
			DefaultMesh mesh = new DefaultMesh();
			
			float[] scVertices = scMesh.getVertices();
			float[] scNormals = scMesh.getNormals();
									
			for( int facetIdx = 0; facetIdx < scVertices.length/9; facetIdx++ ) {
				int offset = facetIdx * 9;
				Vertex[] triVerts = new Vertex[3];
				for( int vIdx = 0; vIdx < 3; vIdx++ ) {
					int voffset = vIdx * 3;
					triVerts[vIdx] = new Vertex( scVertices[offset+voffset], scVertices[offset+voffset+1], scVertices[offset+voffset+2] );
				}				
				TriangularFacet tri = new TriangularFacet( triVerts[0], triVerts[1], triVerts[2] );
				tri.setNormal( new Vertex( scNormals[offset], scNormals[offset+1], scNormals[offset+2] ) );
				mesh.addFace(tri);
			}
						
			return mesh;
		}
		
		return null;		
	}

}