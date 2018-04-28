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

import java.util.HashMap;
import java.util.Map;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.Triangle;
import net.imagej.mesh.Vertex;
import net.imglib2.RealPoint;

/**
 * Utility methods for working with {@link Mesh} objects.
 *
 * @author Curtis Rueden
 */
public class MeshUtils {

    /**
     * Find the center of a mesh using vertices
     * 
     * @return a RealPoint representing the mesh's center
     */
    public static RealPoint center( final Mesh m ) {
        RealPoint p = new RealPoint( 0, 0, 0 );
        for( final Vertex v : m.vertices() ) {
            p.move( v );
        }
        for( int d = 0; d < 3; d++ ) {
            p.setPosition( p.getDoublePosition( d ) / m.vertices().size(), d );
        }
        return p;
    }

    public static float[] boundingBox( final net.imagej.mesh.Mesh mesh ) {
        final float[] boundingBox = new float[] { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                                                  Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                                                  Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        for( final Vertex v : mesh.vertices() ) {
            final float x = v.xf(), y = v.yf(), z = v.zf();
            if( x < boundingBox[0] ) boundingBox[0] = x;
            if( y < boundingBox[1] ) boundingBox[1] = y;
            if( z < boundingBox[2] ) boundingBox[2] = z;
            if( x > boundingBox[3] ) boundingBox[3] = x;
            if( y > boundingBox[4] ) boundingBox[4] = y;
            if( z > boundingBox[5] ) boundingBox[5] = z;
        }
        return boundingBox;
    }

    /**
     * Copies a mesh into another mesh.
     * 
     * @param src
     *            Source mesh, from which data will be copied.
     * @param dest
     *            Destination mesh, into which source will be copied.
     */
    public static void copy( final net.imagej.mesh.Mesh src, final net.imagej.mesh.Mesh dest ) {
        final Map<Long, Long> vIndexMap = new HashMap<>();
        // Copy the vertices, keeping track when indices change.
        for( final Vertex v : src.vertices() ) {
            long srcIndex = v.index();
            long destIndex = dest.vertices().add( v.x(), v.y(), v.z(), //
                                                  v.nx(), v.ny(), v.nz(), //
                                                  v.u(), v.v() );
            if( srcIndex != destIndex ) {
                // NB: If the destination vertex index matches the source, we skip
                // recording the entry, to save space in the map. Later, we leave
                // indexes unchanged which are absent from the map.
                //
                // This scenario is actually quite common, because vertices are often
                // numbered in natural order, with the first vertex having index 0,
                // the second having index 1, etc., although it is not guaranteed.
                vIndexMap.put( srcIndex, destIndex );
            }
        }
        // Copy the triangles, taking care to use destination indices.
        for( final Triangle tri : src.triangles() ) {
            final long v0src = tri.vertex0();
            final long v1src = tri.vertex1();
            final long v2src = tri.vertex2();
            final long v0 = vIndexMap.getOrDefault( v0src, v0src );
            final long v1 = vIndexMap.getOrDefault( v1src, v1src );
            final long v2 = vIndexMap.getOrDefault( v2src, v2src );
            dest.triangles().add( v0, v1, v2, tri.nx(), tri.ny(), tri.nz() );
        }
    }
}
