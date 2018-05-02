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

package sc.iview.process;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.imagej.mesh.Meshes;
import net.imagej.mesh.nio.BufferMesh;

/**
 * Conversion routines between ImageJ and Scenery {@code Mesh} objects.
 *
 * @author Curtis Rueden
 * @author Kyle Harrington
 */
public class MeshConverter {

    public static graphics.scenery.Mesh toScenery( final net.imagej.mesh.Mesh mesh ) {
        final int vCount = //
                ( int ) Math.min( Integer.MAX_VALUE, mesh.vertices().size() );
        final int tCount = //
                ( int ) Math.min( Integer.MAX_VALUE, mesh.triangles().size() );

        // Convert the mesh to an NIO-backed one.
        BufferMesh bufferMesh;
        if( mesh instanceof BufferMesh ) {
            // TODO: Check that BufferMesh capacities & positions are compatible.
            // Need to double check what Scenery assumes about the given buffers.
            bufferMesh = ( BufferMesh ) mesh;
        } else {
            // Copy the mesh into a BufferMesh.
            bufferMesh = new BufferMesh( vCount, tCount );
            Meshes.copy( mesh, bufferMesh );
        }

        // Extract buffers from the BufferMesh.
        final FloatBuffer verts = bufferMesh.vertices().verts();
        final FloatBuffer normals = bufferMesh.vertices().normals();
        final FloatBuffer texCoords = bufferMesh.vertices().texCoords();
        final IntBuffer indices = bufferMesh.triangles().indices();

        // Prepare the buffers for Scenery to ingest them.
        // Sets capacity to equal position, then resets position to 0.
        verts.flip();
        normals.flip();
        texCoords.flip();
        indices.flip();

        // Create and populate the Scenery mesh.
        graphics.scenery.Mesh scMesh = new graphics.scenery.Mesh();
        scMesh.setVertices( verts );
        scMesh.setNormals( normals );
        scMesh.setTexcoords( texCoords );
        scMesh.setIndices( indices );

        scMesh.recalculateNormals();

        scMesh.setBoundingBox( scMesh.generateBoundingBox() );
        scMesh.setDirty( true );

        return scMesh;
    }

    public static net.imagej.mesh.Mesh toImageJ( final graphics.scenery.Mesh scMesh ) {
        // Extract buffers from Scenery mesh.
        final FloatBuffer verts = scMesh.getVertices();
        final FloatBuffer vNormals = scMesh.getNormals();
        final FloatBuffer texCoords = scMesh.getTexcoords();
        final IntBuffer indices = scMesh.getIndices();

        // Compute the triangle normals.
        final FloatBuffer tNormals = //
                ByteBuffer.allocateDirect( indices.capacity() ).asFloatBuffer();
        for( int i = 0; i < indices.position(); i += 3 ) {
            final int v0 = indices.get( i );
            final int v1 = indices.get( i + 1 );
            final int v2 = indices.get( i + 2 );

            final float v0x = verts.get( v0 );
            final float v0y = verts.get( v0 + 1 );
            final float v0z = verts.get( v0 + 2 );
            final float v1x = verts.get( v1 );
            final float v1y = verts.get( v1 + 1 );
            final float v1z = verts.get( v1 + 2 );
            final float v2x = verts.get( v2 );
            final float v2y = verts.get( v2 + 1 );
            final float v2z = verts.get( v2 + 2 );

            final float v10x = v1x - v0x;
            final float v10y = v1y - v0y;
            final float v10z = v1z - v0z;

            final float v20x = v2x - v0x;
            final float v20y = v2y - v0y;
            final float v20z = v2z - v0z;

            final float nx = v10y * v20z - v10z * v20y;
            final float ny = v10z * v20x - v10x * v20z;
            final float nz = v10x * v20y - v10y * v20x;

            tNormals.put( nx );
            tNormals.put( ny );
            tNormals.put( nz );
        }

        return new BufferMesh( verts, vNormals, texCoords, indices, tNormals );
    }
}
