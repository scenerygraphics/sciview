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

import java.nio.FloatBuffer;
import java.util.List;

import net.imagej.mesh.Triangle;
import net.imagej.mesh.Vertex3;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.log.LogService;

import graphics.scenery.BufferUtils;

/**
 * ImageJ OPs Mesh to Scenery Mesh converter
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden,
 * rhaase@mpi-cbg.de Kyle Harrington, University of Idaho
 */
public class MeshConverter {
    public static graphics.scenery.Mesh getSceneryMesh( Mesh mesh, LogService logService ) {
        graphics.scenery.Mesh scMesh;
        if( mesh != null ) {
            int numDimension = 3;
            scMesh = new graphics.scenery.Mesh();

            List<Facet> facets = mesh.getFacets();

            float[] scVertices = new float[facets.size() * 3 * numDimension];
            float[] scNormals = new float[facets.size() * 3 * numDimension];

            float[] boundingBox = new float[] { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                                                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                                                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };

            int count = 0;
            List<Vertex> vertices;
            for( Facet facet : facets ) {
                TriangularFacet tri = ( TriangularFacet ) facet;
                vertices = tri.getVertices();
                Vector3D normal = tri.getNormal();
                for( Vertex v : vertices ) {
                    for( int d = 0; d < numDimension; d++ ) {
                        scVertices[count] = ( float ) v.getDoublePosition( d );

                        if( scVertices[count] < boundingBox[d] )// min
                            boundingBox[d] = scVertices[count];

                        if( scVertices[count] > boundingBox[d + 3] )// min
                            boundingBox[d + 3] = scVertices[count];

                        if( d == 0 ) scNormals[count] = ( float ) normal.getX();
                        else if( d == 1 ) scNormals[count] = ( float ) normal.getY();
                        else if( d == 2 ) scNormals[count] = ( float ) normal.getZ();
                        count++;
                    }
                }
            }

            logService.warn( "Converted " + scVertices.length + " vertices and " + scNormals.length + " normals " );
            scMesh.setVertices( BufferUtils.BufferUtils.allocateFloatAndPut( scVertices ) );
            scMesh.setNormals( BufferUtils.BufferUtils.allocateFloatAndPut( scNormals ) );
            scMesh.setTexcoords( BufferUtils.BufferUtils.allocateFloat( 0 ) );
            scMesh.setIndices( BufferUtils.BufferUtils.allocateInt( 0 ) );

            scMesh.recalculateNormals();

            scMesh.setBoundingBoxCoords( boundingBox );
            scMesh.setDirty( true );
            //scMesh.setScale(new GLVector(1f, 1f, 1f));

            return scMesh;
        }
        return null;
    }

    public static graphics.scenery.Mesh getSceneryMesh( net.imagej.mesh.Mesh mesh, LogService logService ) {
        graphics.scenery.Mesh scMesh;
        if( mesh != null ) {
            int numDimension = 3;
            scMesh = new graphics.scenery.Mesh();

            List<Triangle> facets = mesh.getTriangles();

            float[] scVertices = new float[facets.size() * 3 * numDimension];
            float[] scNormals = new float[facets.size() * 3 * numDimension];

            float[] boundingBox = new float[] { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                                                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                                                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };

            int count = 0;
            List<RealLocalizable> vertices;
            for( Triangle tri : facets ) {
                //TriangularFacet tri = (TriangularFacet) facet;
                vertices = tri.getVertices();
                Vertex3 normal = tri.getNormal();
                for( RealLocalizable v : vertices ) {
                    for( int d = 0; d < numDimension; d++ ) {
                        scVertices[count] = ( float ) v.getDoublePosition( d );

                        if( scVertices[count] < boundingBox[d] )// min
                            boundingBox[d] = scVertices[count];

                        if( scVertices[count] > boundingBox[d + 3] )// min
                            boundingBox[d + 3] = scVertices[count];

                        if( d == 0 ) scNormals[count] = ( float ) normal.getX();
                        else if( d == 1 ) scNormals[count] = ( float ) normal.getY();
                        else if( d == 2 ) scNormals[count] = ( float ) normal.getZ();
                        count++;
                    }
                }
            }

            logService.warn( "Converted " + scVertices.length + " vertices and " + scNormals.length + " normals " );
            scMesh.setVertices( BufferUtils.BufferUtils.allocateFloatAndPut( scVertices ) );
            scMesh.setNormals( BufferUtils.BufferUtils.allocateFloatAndPut( scNormals ) );
            scMesh.setTexcoords( BufferUtils.BufferUtils.allocateFloat( 0 ) );
            scMesh.setIndices( BufferUtils.BufferUtils.allocateInt( 0 ) );

            scMesh.recalculateNormals();

            scMesh.setBoundingBoxCoords( boundingBox );
            scMesh.setDirty( true );
            //scMesh.setScale(new GLVector(1f, 1f, 1f));

            return scMesh;
        }
        return null;
    }

    public static Mesh getOpsMesh( graphics.scenery.Mesh scMesh, LogService logService ) {

        if( scMesh != null ) {
            DefaultMesh mesh = new DefaultMesh();

            //float[] scVertices = scMesh.getVertices().array();

            FloatBuffer verts = scMesh.getVertices();

            logService.warn( "Converting mesh a: initial has remaining: " + verts.hasRemaining() );

            // Flip if it looks like we're on the wrong side
            if( !verts.hasRemaining() ) verts.flip();

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
                mesh.addFace( tri );
            }

            verts.flip();

            return mesh;
        }

        return null;
    }

}
