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
package sc.iview.commands.edit

import graphics.scenery.*
import graphics.scenery.attribute.material.HasMaterial
import net.imagej.lut.LUTService
import okio.withLock
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.util.ColorRGB
import org.scijava.widget.ChoiceWidget
import org.scijava.widget.NumberWidget
import sc.iview.event.NodeChangedEvent
import java.util.*

/**
 * A command for interactively editing a node's properties.
 *
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG Dresden
 * @author Curtis Rueden
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
@Plugin(type = Command::class, initializer = "updateCommandFields", visible = false)
class BasicProperties : InspectorInteractiveCommand() {
    @Parameter
    private lateinit var uiSrv: UIService

    @Parameter
    private lateinit var lutService: LUTService

    /* Basic properties */

    @Parameter(required = false, style = ChoiceWidget.LIST_BOX_STYLE, callback = "refreshSceneNodeInDialog")
    private val sceneNode: String? = null

    @Parameter(label = "Visible", callback = "updateNodeProperties", style = "group:Basic")
    private var visible = false

    @Parameter(label = "Color", required = false, callback = "updateNodeProperties", style = "group:Basic")
    private var colour: ColorRGB? = null

    @Parameter(label = "Name", callback = "updateNodeProperties", style = "group:Basic")
    private var name: String = ""

    @Parameter(label = "Position X", style = NumberWidget.SPINNER_STYLE+ ",group:Basic" + ",format:0.000", stepSize = "0.1", callback = "updateNodeProperties")
    private var positionX = 0f

    @Parameter(label = "Position Y", style = NumberWidget.SPINNER_STYLE+ ",group:Basic" + ",format:0.000", stepSize = "0.1", callback = "updateNodeProperties")
    private var positionY = 0f

    @Parameter(label = "Position Z", style = NumberWidget.SPINNER_STYLE+ ",group:Basic" + ",format:0.000", stepSize = "0.1", callback = "updateNodeProperties")
    private var positionZ = 0f


    @Parameter(label = "[Rotation & Scaling]Scale X", style = NumberWidget.SPINNER_STYLE+"group:Rotation & Scaling" + ",format:0.000", stepSize = "0.1", callback = "updateNodeProperties")
    private var scaleX = 1f

    @Parameter(label = "[Rotation & Scaling]Scale Y", style = NumberWidget.SPINNER_STYLE+"group:Rotation & Scaling" + ",format:0.000", stepSize = "0.1", callback = "updateNodeProperties")
    private var scaleY = 1f

    @Parameter(label = "[Rotation & Scaling]Scale Z", style = NumberWidget.SPINNER_STYLE+"group:Rotation & Scaling" + ",format:0.000", stepSize = "0.1", callback = "updateNodeProperties")
    private var scaleZ = 1f

    @Parameter(label = "[Rotation & Scaling]Rotation Phi", style = NumberWidget.SPINNER_STYLE+"group:Rotation & Scaling" + ",format:0.000", min = PI_NEG, max = PI_POS, stepSize = "0.01", callback = "updateNodeProperties")
    private var rotationPhi = 0f

    @Parameter(label = "[Rotation & Scaling]Rotation Theta", style = NumberWidget.SPINNER_STYLE+"group:Rotation & Scaling" + ",format:0.000", min = PI_NEG, max = PI_POS, stepSize = "0.01", callback = "updateNodeProperties")
    private var rotationTheta = 0f

    @Parameter(label = "[Rotation & Scaling]Rotation Psi", style = NumberWidget.SPINNER_STYLE+"group:Rotation & Scaling" + ",format:0.000", min = PI_NEG, max = PI_POS, stepSize = "0.01", callback = "updateNodeProperties")
    private var rotationPsi = 0f


    var sceneNodeChoices = ArrayList<String>()

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    override fun cancel() {
        // noop
    }

    private fun rebuildSceneObjectChoiceList() {
        fieldsUpdating.withLock {
            sceneNodeChoices = ArrayList()
            var count = 0
            // here, we want all nodes of the scene, not excluding PointLights and Cameras
            for(node in sciView.getSceneNodes { _: Node? -> true }) {
                sceneNodeChoices.add(makeIdentifier(node, count))
                count++
            }
            val sceneNodeSelector = info.getMutableInput("sceneNode", String::class.java)
            sceneNodeSelector.choices = sceneNodeChoices

            //todo: if currentSceneNode is set, put it here as current item
            sceneNodeSelector.setValue(this, sceneNodeChoices[sceneNodeChoices.size - 1])
            refreshSceneNodeInDialog()
        }
    }

    /**
     * find out, which node is currently selected in the dialog.
     */
    private fun refreshSceneNodeInDialog() {
        val identifier = sceneNode //sceneNodeSelector.getValue(this);
        currentSceneNode = null
        var count = 0
        for (node in sciView.getSceneNodes { _: Node? -> true }) {
            if (identifier == makeIdentifier(node, count)) {
                currentSceneNode = node
                //System.out.println("current node found");
                break
            }
            count++
        }

        // update property fields according to scene node properties
        sciView.setActiveNode(currentSceneNode)
        updateCommandFields()
        if (sceneNodeChoices.size != sciView.getSceneNodes { _: Node? -> true }.size) {
            rebuildSceneObjectChoiceList()
        }
    }

    /** Updates command fields to match current scene node properties.  */
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun updateCommandFields() {
        val node = currentSceneNode ?: return

        fieldsUpdating.withLock {

            // update colour
            val colourVector = when {
                node is PointLight -> node.emissionColor
                node is HasMaterial -> node.material().diffuse
                else -> Vector3f(0.5f)
            }

            colour = ColorRGB(
                (colourVector[0] * 255).toInt(),  //
                (colourVector[1] * 255).toInt(),  //
                (colourVector[2] * 255).toInt()
            )

            // update visibility
            visible = node.visible

            // update position
            val position = node.spatialOrNull()?.position ?: Vector3f(0.0f)
            positionX = position[0]
            positionY = position[1]
            positionZ = position[2]

            // update rotation
            val eulerAngles = node.spatialOrNull()?.rotation?.getEulerAnglesXYZ(Vector3f()) ?: Vector3f(0.0f)
            rotationPhi = eulerAngles.x()
            rotationTheta = eulerAngles.y()
            rotationPsi = eulerAngles.z()

            // update scale
            val scale = node.spatialOrNull()?.scale ?: Vector3f(1.0f)
            scaleX = scale.x()
            scaleY = scale.y()
            scaleZ = scale.z()

            name = node.name
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode ?: return
        fieldsUpdating.withLock {

            // update visibility
            node.visible = visible

            // update colour
            val cVector = Vector3f(
                colour!!.red / 255f,
                colour!!.green / 255f,
                colour!!.blue / 255f
            )
            if(node is PointLight) {
                node.emissionColor = cVector
            } else {
                node.ifMaterial {
                    diffuse = cVector
                }
            }

            // update spatial properties
            node.ifSpatial {
                position = Vector3f(positionX, positionY, positionZ)
                scale = Vector3f(scaleX, scaleY, scaleZ)
                rotation = Quaternionf().rotateXYZ(rotationPhi, rotationTheta, rotationPsi)
            }
            node.name = name

            events.publish(NodeChangedEvent(node))
        }
    }

    private fun makeIdentifier(node: Node, count: Int): String {
        return "" + node.name + "[" + count + "]"
    }
}
