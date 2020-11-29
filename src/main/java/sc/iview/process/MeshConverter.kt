/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
package sc.iview.process

import graphics.scenery.BufferUtils.Companion.allocateFloat
import net.imagej.mesh.Mesh
import net.imagej.mesh.Meshes
import net.imagej.mesh.nio.BufferMesh
import java.nio.FloatBuffer

/**
 * Conversion routines between ImageJ and Scenery `Mesh` objects.
 *
 * @author Curtis Rueden
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
object MeshConverter {
    @JvmOverloads @JvmStatic
    fun toScenery(mesh: Mesh, center: Boolean = false, flipWindingOrder: Boolean = false): graphics.scenery.Mesh {
        val vCount = Int.MAX_VALUE.toLong().coerceAtMost(mesh.vertices().size()).toInt()
        val tCount = Int.MAX_VALUE.toLong().coerceAtMost(mesh.triangles().size()).toInt()

        // Convert the mesh to an NIO-backed one.
        val bufferMesh = BufferMesh(vCount, tCount)
        Meshes.calculateNormals(mesh, bufferMesh) // Force recalculation of normals because not all meshes are safe

//        if( mesh instanceof BufferMesh ) {
//            // TODO: Check that BufferMesh capacities & positions are compatible.
//            // Need to double check what Scenery assumes about the given buffers.
//            bufferMesh = ( BufferMesh ) mesh;
//        } else {
//            // Copy the mesh into a BufferMesh.
//            bufferMesh = new BufferMesh( vCount, tCount );
//            Meshes.copy( mesh, bufferMesh );
//        }

        // Extract buffers from the BufferMesh.
        val verts = bufferMesh.vertices().verts()
        val vNormals = bufferMesh.vertices().normals()
        val texCoords = bufferMesh.vertices().texCoords()
        val indices = bufferMesh.triangles().indices()

        // Prepare the buffers for Scenery to ingest them.
        // Sets capacity to equal position, then resets position to 0.
        verts.flip()
        if (center) { // Do 2 passes, 1 to find center, and the other to shift
            val v = floatArrayOf(0.0f, 0.0f, 0.0f) // used for tally of coords and mean
            var coord = 0 // coordinate index
            var n = 0 // num verts
            while (verts.hasRemaining()) {
                v[coord] += verts.get()
                if (coord == 0) n++
                coord = (coord + 1) % 3
            }
            verts.flip()
            // Take average
            for (k in 0..2) {
                v[k] /= n.toFloat()
            }
            // Center shift
            coord = 0
            var value: Float
            while (verts.hasRemaining()) {
                value = verts.get()
                // Write
                verts.put(verts.position() - 1, value - v[coord].toFloat())
                coord = (coord + 1) % 3
            }
            verts.flip()
        }
        vNormals.flip()
        texCoords.flip()
        indices.flip()
        if (flipWindingOrder) {
            flipVectorBuffer(verts, 3)
            flipVectorBuffer(vNormals, 3)
            flipVectorBuffer(texCoords, 2)
        }

        // Create and populate the Scenery mesh.
        val scMesh = graphics.scenery.Mesh()
        scMesh.vertices = verts
        scMesh.normals = vNormals
        scMesh.texcoords = texCoords
        scMesh.indices = indices
        scMesh.boundingBox = scMesh.generateBoundingBox()
        scMesh.dirty = true
        return scMesh
    }

    private fun flipVectorBuffer(buffer: FloatBuffer, vectorSize: Int) {
        val stepSize = vectorSize * vectorSize
        var index = 0
        val tmp = FloatArray(stepSize)
        while (buffer.hasRemaining()) {
            buffer[tmp, 0, stepSize]
            for (i in vectorSize - 1 downTo 0) {
                for (j in 0 until vectorSize) {
                    buffer.put(index + vectorSize * i + j, tmp[vectorSize * (vectorSize - (i + 1)) + j])
                }
            }
            index += stepSize
        }
        buffer.flip()
    }

    @JvmStatic
    fun toImageJ(scMesh: graphics.scenery.Mesh): Mesh {
        // Extract buffers from Scenery mesh.
        val verts = scMesh.vertices
        val vNormals = scMesh.normals
        val texCoords = scMesh.texcoords
        val indices = scMesh.indices

        // Compute the triangle normals.
        val tNormals =  //
                allocateFloat(indices.capacity())
        var i = 0
        while (i < indices.position()) {
            val v0 = indices[i]
            val v1 = indices[i + 1]
            val v2 = indices[i + 2]
            val v0x = verts[v0]
            val v0y = verts[v0 + 1]
            val v0z = verts[v0 + 2]
            val v1x = verts[v1]
            val v1y = verts[v1 + 1]
            val v1z = verts[v1 + 2]
            val v2x = verts[v2]
            val v2y = verts[v2 + 1]
            val v2z = verts[v2 + 2]
            val v10x = v1x - v0x
            val v10y = v1y - v0y
            val v10z = v1z - v0z
            val v20x = v2x - v0x
            val v20y = v2y - v0y
            val v20z = v2z - v0z
            val nx = v10y * v20z - v10z * v20y
            val ny = v10z * v20x - v10x * v20z
            val nz = v10x * v20y - v10y * v20x
            tNormals.put(nx)
            tNormals.put(ny)
            tNormals.put(nz)
            i += 3
        }
        return BufferMesh(verts, vNormals, texCoords, indices, tNormals)
    }
}