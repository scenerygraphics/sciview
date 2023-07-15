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
package sc.iview.commands.add

import graphics.scenery.Icosphere
import graphics.scenery.Mesh
import graphics.scenery.Node
import graphics.scenery.attribute.material.Material.DepthTest
import graphics.scenery.primitives.Cylinder
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.SciView.Companion.create
import sc.iview.commands.MenuWeights.ADD
import sc.iview.commands.MenuWeights.EDIT_ADD_COMPASS

/**
 * Command to orientation compass (R,G,B cylinders oriented along X,Y,Z axes, respectively) to the scene
 *
 * @author Vladimir Ulman
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = ADD), Menu(
        label = "Compass",
        weight = EDIT_ADD_COMPASS
    )]
)
class AddOrientationCompass : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private val axisLength = 0.1f

    @Parameter
    private val AXESBARRADIUS = 0.001f

    @Parameter
    private val xColor = Vector3f(1f, 0f, 0f)

    @Parameter
    private val yColor = Vector3f(0f, 1f, 0f)

    @Parameter
    private val zColor = Vector3f(0f, 0f, 1f)
    private fun makeAxis(axisLength: Float, angleX: Float, angleY: Float, angleZ: Float, color: Vector3f): Node {
        val axisNode = Cylinder(AXESBARRADIUS, axisLength, 4)
        axisNode.name = "compass axis: X"
        axisNode.spatial().rotation = Quaternionf().rotateXYZ(angleX, angleY, angleZ)
        axisNode.ifMaterial {
            diffuse.set(color)
            depthTest = DepthTest.Always
            blending.transparent = true
        }
        val axisCap = Icosphere(AXESBARRADIUS, 2)
        axisCap.ifSpatial {
            position = Vector3f(0.0f, axisLength, 0.0f)
        }
        axisCap.material().diffuse.set(color)
        axisCap.material().depthTest = DepthTest.Always
        axisCap.material().blending.transparent = true
        axisNode.addChild(axisCap)
        return axisNode
    }

    override fun run() {
        val root: Node = Mesh("Scene orientation compass")

        //NB: RGB colors ~ XYZ axes
        //x axis:
        var axisNode = makeAxis(axisLength, 0f, 0f, (-0.5 * Math.PI).toFloat(), xColor)
        axisNode.name = "compass axis: X"
        root.addChild(axisNode)

        //y axis:
        axisNode = makeAxis(axisLength, 0f, 0f, 0f, yColor)
        axisNode.name = "compass axis: Y"
        root.addChild(axisNode)

        //z axis:
        axisNode = makeAxis(axisLength, (0.5 * Math.PI).toFloat(), 0f, 0f, zColor)
        axisNode.name = "compass axis: Z"
        root.addChild(axisNode)
        sciView.addNode(root)
        sciView.camera!!.addChild(root)
        root.update.add {
            val cam = sciView.camera
            root.position = cam!!.viewportToView(Vector2f(-0.9f, 0.7f))
            root.rotation = Quaternionf(cam.rotation).conjugate().normalize()
            null
        }
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(AddOrientationCompass::class.java, true, argmap)
        }
    }
}