package sc.fiji;

import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

public class MeshUtils {
	/**
	 * Find the center of a mesh using vertices
	 * 
	 * @return a RealLocalizable representing the mesh's center
	 */
	public static RealLocalizable getCenter(Mesh m) {
		RealPoint p = new RealPoint( 0, 0, 0 );
		for( RealLocalizable v : m.getVertices() ) {
			p.move( v );
		}
		for( int d = 0; d < 3; d++ ) {
			p.setPosition( p.getDoublePosition(d)/m.getVertices().size(), d );
		}
		return p;
	}
	
	/**
	 * Translate the mesh such that it's center is at 0,0,0
	 */
	public static void centerMesh(Mesh m) {
		RealPoint center = (RealPoint) getCenter(m);
		
		for( Facet f : m.getFacets() ) {
			for( Vertex v : ((TriangularFacet) f).getVertices() ) {
				for( int d = 0; d < 3; d++ ) {
					//v.changeDoublePosition(d, v.getDoublePosition(d) - center.getDoublePosition(d) );
					v.setDoublePosition(d, v.getDoublePosition(d) - center.getDoublePosition(d) );
				}
			}
		}		
	}
}
