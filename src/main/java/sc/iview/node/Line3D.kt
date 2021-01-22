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
class Line3D : RenderableNode {
    private var edges: MutableList<RenderableNode>
    private var joints: MutableList<RenderableNode>? = null
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
                val edge = Cylinder.betweenPoints(
                        points[k - 1],
                        points[k],
                        edgeWidth.toFloat(),
                        1f,
                        15)
                addLine(edge)
            }
            if (sphereJoints) {
                val joint = Sphere(edgeWidth.toFloat(), 15)
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
                val edge = Cylinder.betweenPoints(
                        points[k - 1],
                        points[k],
                        edgeWidth.toFloat(),
                        1f,
                        15)
                edge.renderable().material = mat
                addLine(edge)
            }
            if (sphereJoints) {
                val joint = Sphere(edgeWidth.toFloat(), 15)
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

    fun addLine(l: RenderableNode) {
        edges.add(l)
        addChild(l)
        generateBoundingBox()
    }

    fun getEdges(): List<RenderableNode> {
        return edges
    }

    /**
     * Generates an [OrientedBoundingBox] for this [Node]. This will take
     * geometry information into consideration if this Node implements [HasGeometry].
     * In case a bounding box cannot be determined, the function will return null.
     */
    override fun generateBoundingBox(): OrientedBoundingBox {
        var bb = OrientedBoundingBox(this, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f)
        for (n in children) {
            val cBB = n.generateBoundingBox()
            if (cBB != null) bb = bb.expand(bb, cBB)
        }
        return bb
    }
}
