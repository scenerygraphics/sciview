/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
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
package sc.iview.node

import graphics.scenery.*
import org.joml.Vector3f
import org.scijava.util.ColorRGB
import org.scijava.util.Colors
import sc.iview.Utils
import java.util.ArrayList

/**
 * A 3D line composed of scenery Nodes
 *
 * @author Kyle Harrington
 */
class Line3D : Node {
    private var edges: MutableList<Node>
    private var joints: MutableList<Node>? = null
    private var edgeWidth = 0.05
    private var defaultColor = Colors.LIGHTSALMON
    private val sphereJoints = true

    constructor() {
        edges = ArrayList()
    }

    constructor(points: List<Vector3f>, colorRGB: ColorRGB, edgeWidth: Double) {
        defaultColor = colorRGB
        this.edgeWidth = edgeWidth
        edges = ArrayList()
        if (sphereJoints) joints = ArrayList()
        for (k in points.indices) {
            if (k > 0) {
                val edge: Node = Cylinder.betweenPoints(
                        points[k - 1],
                        points[k],
                        edgeWidth.toFloat(),
                        1f,
                        15)
                addLine(edge)
            }
            if (sphereJoints) {
                val joint: Node = Sphere(edgeWidth.toFloat(), 15)
                joint.position = points[k]
                joints!!.add(joint)
                addChild(joint)
            }
        }
    }

    constructor(points: List<Vector3f>, colors: List<ColorRGB>, edgeWidth: Double) {
        this.edgeWidth = edgeWidth
        edges = ArrayList()
        if (sphereJoints) joints = ArrayList()
        for (k in points.indices) {
            val c = Utils.convertToVector3f(colors[k])
            val mat = Material()
            mat.diffuse = c
            mat.ambient = c
            mat.specular = c
            if (k > 0) {
                val edge: Node = Cylinder.betweenPoints(
                        points[k - 1],
                        points[k],
                        edgeWidth.toFloat(),
                        1f,
                        15)
                edge.material = mat
                addLine(edge)
            }
            if (sphereJoints) {
                val joint: Node = Sphere(edgeWidth.toFloat(), 15)
                joint.material = mat
                joint.position = points[k]
                joints!!.add(joint)
                addChild(joint)
            }
        }
    }

    fun setColors(colors: List<ColorRGB>) {
        for (k in joints!!.indices) {
            val c = Utils.convertToVector3f(colors[k])
            val mat = Material()
            mat.diffuse = c
            mat.ambient = c
            mat.specular = c
            joints!![k].material = mat
            joints!![k].needsUpdate = true
            joints!![k].dirty = true
            edges[k].material = mat
            edges[k].needsUpdate = true
            edges[k].dirty = true
        }
    }

    fun addLine(l: Node) {
        edges.add(l)
        addChild(l)
        generateBoundingBox()
    }

    fun getEdges(): List<Node> {
        return edges
    }

    /**
     * Generates an [OrientedBoundingBox] for this [Node]. This will take
     * geometry information into consideration if this Node implements [HasGeometry].
     * In case a bounding box cannot be determined, the function will return null.
     */
    override fun generateBoundingBox(): OrientedBoundingBox? {
        var bb = OrientedBoundingBox(this, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f)
        for (n in children) {
            val cBB = n.generateBoundingBox()
            if (cBB != null) bb = bb.expand(bb, cBB)
        }
        return bb
    }
}
