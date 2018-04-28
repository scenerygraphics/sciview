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
package sc.iview;

import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

public class MeshUtils {
    /**
     * Find the center of a mesh using vertices
     * 
     * @return a RealPoint representing the mesh's center
     */
    public static RealPoint center( Mesh m ) {
        RealPoint p = new RealPoint( 0, 0, 0 );
        for( RealLocalizable v : m.getVertices() ) {
            p.move( v );
        }
        for( int d = 0; d < 3; d++ ) {
            p.setPosition( p.getDoublePosition( d ) / m.getVertices().size(), d );
        }
        return p;
    }

    /**
     * Translate the mesh such that it's center is at 0,0,0
     * 
     * This requires DefaultMesh to change
     * 
     */
//    public static void centerMesh(Mesh m) {
//        RealPoint center = center(m);
//        
//        for( Facet f : m.getFacets() ) {
//            for( Vertex v : ((TriangularFacet) f).getVertices() ) {
//                for( int d = 0; d < 3; d++ ) {
//                    //v.changeDoublePosition(d, v.getDoublePosition(d) - center.getDoublePosition(d) );
//                    v.setDoublePosition(d, v.getDoublePosition(d) - center.getDoublePosition(d) );
//                }
//            }
//        }        
//    }
}
