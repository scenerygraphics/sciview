/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
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
package sc.iview

import graphics.scenery.Mesh
import net.imglib2.RealLocalizable
import net.imglib2.RealPoint
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.Views
import org.joml.Vector3f
import org.scijava.util.ColorRGB
import org.scijava.util.ColorRGBA
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.*
import java.net.URL
import java.util.*
import javax.swing.ImageIcon

/**
 * Utility methods.
 *
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
object Utils {
    @JvmStatic
    fun convertToVector3f(color: ColorRGB): Vector3f {
        return if (color is ColorRGBA) {
            Vector3f(color.getRed() / 255f,  //
                    color.getGreen() / 255f,  //
                    color.getBlue() / 255f) //color.getAlpha() / 255f );
        } else Vector3f(color.red / 255f,  //
                color.green / 255f,  //
                color.blue / 255f)
    }

    @JvmStatic
    fun convertToARGB(screenshot: Img<UnsignedByteType>): Img<ARGBType> {
        val out: Img<ARGBType> = ArrayImgs.argbs(screenshot.dimension(0), screenshot.dimension(1))
        val outCur = out.cursor()
        val screenshotCursor = screenshot.cursor()

        while (outCur.hasNext()) {
            screenshotCursor.fwd()
            val a = screenshotCursor.get().get()
            screenshotCursor.fwd()
            val b = screenshotCursor.get().get()
            screenshotCursor.fwd()
            val g = screenshotCursor.get().get()
            screenshotCursor.fwd()
            val r = screenshotCursor.get().get()

            outCur.fwd()
            outCur.get().set(ARGBType.rgba(r, g, b, a))
        }

        return out
    }

    @JvmStatic
    fun getVertexList(m: Mesh): List<Vector3f> {
        val l: MutableList<Vector3f> = ArrayList()
        val vb = m.vertices
        while (vb.hasRemaining()) {
            val x = vb.get()
            val y = vb.get()
            val z = vb.get()
            l.add(Vector3f(x, y, z))
        }
        vb.flip()
        return l
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeXYZ(xyzFile: File, mesh: Mesh) {
        val w = BufferedWriter(FileWriter(xyzFile))
        val verts = getVertexList(mesh)
        for (v in verts) {
            w.write("""
    ${v.x()}, ${v.y()}, ${v.z()}
    
    """.trimIndent())
        }
        w.close()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeXYZ(xyzFile: File, points: List<RealLocalizable>) {
        val w = BufferedWriter(FileWriter(xyzFile))
        if (points.isEmpty()) return
        val numDim = points[0].numDimensions()

        for (v in points) {
            for (d in 0 until numDim) {
                if (d > 0) w.write(", ")
                w.write("" + v.getDoublePosition(d))
            }
            w.write("\n")
        }
        w.close()
    }

    /**
     * Returns a scaled [ImageIcon] given in [resource], with a new [width] and [height].
     */
    @JvmStatic
    fun getScaledImageIcon(resource: URL, width: Int, height: Int): ImageIcon {
        val first = ImageIcon(resource)
        val resizedImg = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = resizedImg.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(first.image, 0, 0, width, height, null)
        g2.dispose()
        return ImageIcon(resizedImg)
    }

    /**
     * Create an array of normal vectors from a set of vertices corresponding to triangles
     *
     * @param verts vertices to use for computing normals, assumed to be ordered as triangles
     * @return array of normals
     */
    @JvmStatic
    fun makeNormalsFromVertices(verts: ArrayList<RealPoint>): FloatArray {
        val normals = FloatArray(verts.size) // div3 * 3coords
        var k = 0
        while (k < verts.size) {
            val v1 = Vector3f(verts[k].getFloatPosition(0),  //
                    verts[k].getFloatPosition(1),  //
                    verts[k].getFloatPosition(2))
            val v2 = Vector3f(verts[k + 1].getFloatPosition(0),
                    verts[k + 1].getFloatPosition(1),
                    verts[k + 1].getFloatPosition(2))
            val v3 = Vector3f(verts[k + 2].getFloatPosition(0),
                    verts[k + 2].getFloatPosition(1),
                    verts[k + 2].getFloatPosition(2))
            val a = v2.sub(v1)
            val b = v3.sub(v1)
            val n = a.cross(b).normalize()
            normals[k / 3] = n[0]
            normals[k / 3 + 1] = n[1]
            normals[k / 3 + 2] = n[2]
            k += 3
        }
        return normals
    }

    /**
     * Blocks the current thread while predicate is true
     *
     * @param predicate predicate function that returns true as long as this function should block
     * @param waitTime wait time before predicate re-evaluation
     */
    @JvmStatic
    fun blockWhile(predicate: () -> Boolean, waitTime: Int) {
        while (predicate.invoke()) {
            try {
                Thread.sleep(waitTime.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Write a scenery mesh as an stl to the given file
     * @param filename filename of the stl
     * @param scMesh mesh to save
     */
    @JvmStatic
    fun writeSCMesh(filename: String, scMesh: graphics.scenery.Mesh) {
        writeSCMesh(filename, scMesh, false)
    }

    /**
     * Write a scenery mesh as an stl to the given file
     * @param filename filename of the stl
     * @param scMesh mesh to save
     * @param useMeshScale apply mesh's scale when exporting vertices
     */
    @JvmStatic
    fun writeSCMesh(filename: String, scMesh: graphics.scenery.Mesh, useMeshScale: Boolean) {
        val f = File(filename)
        val out: BufferedOutputStream
        try {
            out = BufferedOutputStream(FileOutputStream(f))
            out.write("solid STL generated by FIJI\n".toByteArray())
            val normalsFB = scMesh.normals
            val verticesFB = scMesh.vertices
            val meshScale = scMesh.spatial().scale
            while (verticesFB.hasRemaining() && normalsFB.hasRemaining()) {
                out.write("""facet normal ${normalsFB.get()} ${normalsFB.get()} ${normalsFB.get()}
""".toByteArray())
                out.write("outer loop\n".toByteArray())
                for (v in 0..2) {
                    val x = verticesFB.get() * meshScale.x
                    val y = verticesFB.get() * meshScale.y
                    val z = verticesFB.get() * meshScale.z
                    out.write("""vertex	$x $y $z
""".toByteArray())
                }
                out.write("endloop\n".toByteArray())
                out.write("endfacet\n".toByteArray())
            }
            verticesFB.flip()
            out.write("endsolid vcg\n".toByteArray())
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class SciviewStandalone {
    }
    
    @JvmStatic
    fun createSciviewStandaloneObject() : SciviewStandalone {
        return SciviewStandalone()        
    }
}
