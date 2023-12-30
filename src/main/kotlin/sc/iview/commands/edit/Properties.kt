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
package sc.iview.commands.edit

import TractogramTools
import graphics.scenery.*
import graphics.scenery.attribute.material.HasMaterial
import graphics.scenery.primitives.Line
import graphics.scenery.primitives.TextBoard
import graphics.scenery.volumes.Colormap.Companion.fromColorTable
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
import graphics.scenery.volumes.VolumeManager
import net.imagej.lut.LUTService
import net.imglib2.display.ColorTable
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.command.InteractiveCommand
import org.scijava.event.EventService
import org.scijava.log.LogService
import org.scijava.module.Module
import org.scijava.module.ModuleItem
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.util.ColorRGB
import org.scijava.widget.Button
import org.scijava.widget.ChoiceWidget
import org.scijava.widget.NumberWidget
import sc.iview.SciView
import sc.iview.event.NodeChangedEvent
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * A command for interactively editing a node's properties.
 *
 *  * TODO: If the list of sceneNode changes while this dialog is open, it may
 * not be notified and thus, may cause strange behaviours. Furthermore,
 * refreshing the list of choises does not work. :(
 *  * Todo: Change the order of the property items. Scene node must be on top,
 * as the user selects here which object to manipulate.
 *  * Todo: As soon as object selection in Scenery itself works, the node
 * pulldown may be removed entirely.
 *
 * To add new properties you need to do a few things:
 * 1. Create a @Parameter for the variable and ensure the style has an appropriate group
 * Note: I believe the group relates to the class name, but I'm confused about where that happens.
 * 2. Add code to get the value from the node to updateCommandFields
 * 3. Add code to set the value of the node to updateNodeProperties
 *
 *
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG Dresden
 * @author Curtis Rueden
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
@Plugin(type = Command::class, initializer = "initValues", visible = false)
class Properties : InteractiveCommand() {
    @Parameter
    private lateinit var uiSrv: UIService

    @Parameter
    private lateinit var lutService: LUTService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var events: EventService

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

    /* Camera properties */

    @Parameter(label = "Active", required = false, callback = "updateNodeProperties", style = "group:Camera")
    private var active = false

    /* Volume properties */

    @Parameter(label = "Timepoint", callback = "updateNodeProperties", style = NumberWidget.SLIDER_STYLE+",group:Volume")
    private var timepoint = 0

    @Parameter(label = "Play", callback = "playTimeSeries", style = "group:Volume")
    private var playPauseButton: Button? = null

    @Volatile
    @Parameter(label = "Speed", min = "1", max = "10", style = NumberWidget.SCROLL_BAR_STYLE + ",group:Volume", persist = false)
    private var playSpeed = 4

    @Parameter(label = "Min", callback = "updateNodeProperties", style = "group:Volume")
    private var min = 0

    @Parameter(label = "Max", callback = "updateNodeProperties", style = "group:Volume")
    private var max = 255

    @Parameter(label = "Color map", choices = [], callback = "updateNodeProperties", style = "group:Volume")
    private var colormapName: String = "Red"

    @Parameter(label = " ", style = "group:Volume")
    private var colormap = dummyColorTable

    @Parameter(label = "AO steps", style = NumberWidget.SPINNER_STYLE+"group:Volume", callback = "updateNodeProperties")
    private val occlusionSteps = 0

    @Parameter(label = "Volume slicing mode", callback = "updateNodeProperties", style = ChoiceWidget.LIST_BOX_STYLE+"group:Volume")
    private var slicingMode: String = Volume.SlicingMode.Slicing.name

    @Parameter(label = "Width", callback = "updateNodeProperties", visibility = ItemVisibility.MESSAGE, style = "group:Volume")
    private var width: Int = 0

    @Parameter(label = "Height", callback = "updateNodeProperties", visibility = ItemVisibility.MESSAGE, style = "group:Volume")
    private var height: Int = 0

    @Parameter(label = "Depth", callback = "updateNodeProperties", visibility = ItemVisibility.MESSAGE, style = "group:Volume")
    private var depth: Int = 0

    /* Light properties */

    @Parameter(label = "Intensity", style = NumberWidget.SPINNER_STYLE+ ",group:Lighting", stepSize = "0.1", callback = "updateNodeProperties")
    private var intensity = 0f

    /* Rotation and Scaling properties */

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

    /* Bounding Grid properties */
    @Parameter(label = "Grid Color", callback = "updateNodeProperties", style = "group:Grid")
    private var gridColor: ColorRGB? = null

    @Parameter(label = "Ticks only", callback = "updateNodeProperties", style = "group:Grid")
    private var ticksOnly = false

    /* TextBoard properties */
    @Parameter(label = "Text", callback = "updateNodeProperties", style = "group:Text")
    private var text: String? = null

    @Parameter(label = "Text Color", callback = "updateNodeProperties", style = "group:Text")
    private var fontColor: ColorRGB? = null

    @Parameter(label = "Background Color", callback = "updateNodeProperties", style = "group:Text")
    private var backgroundColor: ColorRGB? = null

    @Parameter(label = "Transparent Background", callback = "updateNodeProperties", style = "group:Text")
    private var transparentBackground = false

    /* Targets properties */

    @Parameter(label = "Sliced volumes", callback = "updateNodeProperties", style = "group:Targets")
    private var slicedVolumes: VolumeSelectorWidget.VolumeSelection = VolumeSelectorWidget.VolumeSelection()

    /* Line properties */

    @Parameter(label = "Edge width", callback = "updateNodeProperties", style = "group:Line")
    private var edgeWidth = 0

    private val slicingModeChoices = Volume.SlicingMode.values().toMutableList()

    var fieldsUpdating = true
    var sceneNodeChoices = ArrayList<String>()
    private var currentSceneNode: Node? = null

    @Parameter
    private lateinit var log: LogService

    /* Tractogram properties */

    @Parameter(label = "Select brain region", choices = [], style = "group:Select Streamlines")
    private var firstBrainRegion: String = "none"

    @Parameter(label = "Select brain region", choices = [], style = "group:Select Streamlines")
    private var secondBrainRegion: String = "none"

    @Parameter(label = "Calculate Selection", callback = "selection", style = "group:Select Streamlines")
    private lateinit var selection: Button

    @Parameter(label = "Exclusion regions", style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE + ",group:Select Streamlines")
    private var exclusion = false

    @Parameter(label = "Select minimal length of fibers to be displayed", persist = false, callback = "filterLength", style = "group:Select Streamlines")
    private var minLength: Float = 0f

    @Parameter(label = "Select maximal length of fibers to be displayed", persist = false, callback = "filterLength", style = "group:Select Streamlines")
    private var maxLength: Float = 0f

    @Parameter(label = "Select maximal number of shown streamlines", callback = "changedMaxStreamlines", persist = true, style = "group:Select Streamlines")
    private var maxStreamlines: Int = 1000

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    override fun cancel() {}

    /**
     * Nothing is done here, as the refreshing of the objects properties works via
     * callback methods.
     */
    override fun run() {}
    fun setSceneNode(node: Node?) {
        currentSceneNode = node
        updateCommandFields()
    }

    protected fun initValues() {
        rebuildSceneObjectChoiceList()
        refreshSceneNodeInDialog()
        updateCommandFields()
    }

    private fun rebuildSceneObjectChoiceList() {
        fieldsUpdating = true
        sceneNodeChoices = ArrayList()
        var count = 0
        // here, we want all nodes of the scene, not excluding PointLights and Cameras
        for (node in sciView.getSceneNodes { _: Node? -> true }) {
            sceneNodeChoices.add(makeIdentifier(node, count))
            count++
        }
        val sceneNodeSelector = info.getMutableInput("sceneNode", String::class.java)
        sceneNodeSelector.choices = sceneNodeChoices

        //todo: if currentSceneNode is set, put it here as current item
        sceneNodeSelector.setValue(this, sceneNodeChoices[sceneNodeChoices.size - 1])
        refreshSceneNodeInDialog()
        fieldsUpdating = false
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
        updateCommandFields()
        if (sceneNodeChoices.size != sciView.getSceneNodes { _: Node? -> true }.size) {
            rebuildSceneObjectChoiceList()
        }
    }

    private fun <T> maybeRemoveInput(name: String, type: Class<T>) {
        try {
            val item = info.getMutableInput(name, type) ?: return
            info.removeInput(item)
        } catch (npe: NullPointerException) {
            return
        }
    }

    private fun maybeRemoveInputTractogram(){
        maybeRemoveInput("firstBrainRegion", String::class.java)
        maybeRemoveInput("secondBrainRegion", String::class.java)
        maybeRemoveInput("selection", Button::class.java)
        maybeRemoveInput("exclusion", java.lang.Boolean::class.java)
        maybeRemoveInput("minLength", java.lang.Float::class.java)
        maybeRemoveInput("maxLength", java.lang.Float::class.java)
        maybeRemoveInput("maxCurvature", java.lang.Float::class.java)
    }

    private fun maybeRemoveInputVolume(){
        maybeRemoveInput("slicingMode", String::class.java)
        maybeRemoveInput("min", java.lang.Integer::class.java)
        maybeRemoveInput("max", java.lang.Integer::class.java)
        maybeRemoveInput("timepoint", java.lang.Integer::class.java)
        maybeRemoveInput("colormap", ColorTable::class.java)
        maybeRemoveInput("colormapName", String::class.java)
        maybeRemoveInput("playPauseButton", Button::class.java)
        maybeRemoveInput("playSpeed", java.lang.Integer::class.java)
        maybeRemoveInput("width", java.lang.Integer::class.java)
        maybeRemoveInput("height", java.lang.Integer::class.java)
        maybeRemoveInput("depth", java.lang.Integer::class.java)
    }

    private var playing = false
    private var timeSeriesPlayer: Thread? = null
    fun playTimeSeries() {
        if (currentSceneNode !is Volume) {
            return
        }
        val v = currentSceneNode as Volume
        playing = !playing
        if (playing && timeSeriesPlayer == null) {
            timeSeriesPlayer = Thread {
                while (playing) {
                    try {
                        var nextTimepoint = v.currentTimepoint + 1
                        if (nextTimepoint >= v.timepointCount) {
                            nextTimepoint = 0
                        }
                        v.goToTimepoint(nextTimepoint)
                        timepoint = nextTimepoint

                        Thread.sleep(1000L / playSpeed)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
            info.getMutableInput("playPauseButton", Button::class.java).label = "Pause"
            timeSeriesPlayer!!.start()
        } else {
            try {
                timeSeriesPlayer!!.join(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            timeSeriesPlayer = null
            info.getMutableInput("playPauseButton", Button::class.java).setLabel("Play")
        }
    }

    /**
     * Creates a new scene node that contains a selection of streamlines. This selection is determined by one or
     * two given brain regions (meshes), in which the streamlines have to start / end. The original streamlines to
     * select from are given in the metadata object "tractogramCalc" of the current scene node.
     * */
    fun selection(){
        log.info("Selecting streamlines connecting brain region $firstBrainRegion with brain region $secondBrainRegion.")
        currentSceneNode?.let {node ->
            // TODO: Use data structure to verify the characteristics of the current node and access the following
            // objects via that structure, so that no check is needed if these objects actually exist
            val streamlineCalcs = node.metadata["tractogramCalc"] as TractogramTools
            val parcellationObject = node.getChildrenByName("Brain areas")[0] as RichNode
            parcellationObject.children.forEach{it.visible = false}
            val tractogramObject = node.getChildrenByName("Tractogram")[0]
            tractogramObject.visible = false

            val meshes = ArrayList<Mesh>(2)

            arrayOf(firstBrainRegion, secondBrainRegion).forEach {
                if(it != "None"){
                    val mesh = parcellationObject.getChildrenByName(it)[0] as Mesh
                    mesh.visible = true
                    meshes.add(mesh)
                }
            }
            if(meshes.isNotEmpty()){
                val selectedTractogram = streamlineCalcs.streamlineSelectionTransformedMesh(parcellationObject, tractogramObject as RichNode, meshes, !exclusion)
                sciView.addNode(selectedTractogram, true, tractogramObject.parent ?: RichNode())
            }else{
                log.warn("No selection meshes provided. No streamlines could be selected.")
            }
        }?: log.error("Current scene node is null. Streamline selection can't be performed.")

    }

    /**
     * Filter streamlines of the tractogram object, so that only streamlines with a length between minLength and maxLength
     * are shown.
     * */
    fun filterLength(){
        val lengthMin = minLength
        val lengthMax = maxLength
        // TODO: Use data structure to verify the characteristics of the current node and access the following
        // objects via that structure, so that no check is needed if these objects actually exist
        currentSceneNode?.getChildrenByName("Tractogram")?.get(0)?.children?.forEach{streamline ->
            val length = streamline.metadata["length"] as Float
            streamline.visible = !(length < lengthMin || length > lengthMax)
        }
    }

    /**
     * Updates the tractogram (as the current scene node) to match the command field maxStreamlines.
     *
     * // TODO: see TODOs within the code: the function does not work like expected yet.
     */
    fun changedMaxStreamlines(){
        val testing = false
        if(testing){
            // TODO: Check min / max requirements for maxStreamlines: Should not go lower than 0 and not higher than 5000 or so
            // TODO: Check first if the current scene node is actually a tractogram by checking with data structure, to
            // ensure it has the following children and metadata attached to it
            val currentParent = currentSceneNode?.parent
            val streamlineCalcs = currentParent?.metadata?.get("tractogramCalc") as TractogramTools
            val streamlineNumData = streamlineCalcs.changeNumberOfStreamlines((currentSceneNode?.metadata?.get("Streamlines")
                ?: ArrayList<ArrayList<Vector3f>>()) as ArrayList<ArrayList<Vector3f>>,
                currentSceneNode as RichNode,
                maxStreamlines)

            if(streamlineNumData.reduction){
                streamlineNumData.streamlines.children.forEach{
                    sciView.deleteNode(it)
                    // TODO: nodes are still displayed in the node tree, which should not happen
                }
            }else{
                sciView.publishNode(streamlineNumData.streamlines)
                // TODO: changeNumberOfStreamlines works directly on the node, however the node is not updated in the node
                // tree and the new streamlines don't show in the scene. It might help to remove the old node and add
                // the new, updated one
            }
        }else{
            log.warn("Changing the maximum streamline count is not supported yet.")
        }
    }

    /** Updates command fields to match current scene node properties.  */
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun updateCommandFields() {
        val node = currentSceneNode ?: return

        fieldsUpdating = true

        // update colour
        val colourVector = when {
            node is PointLight -> node.emissionColor
            node is HasMaterial -> node.material().diffuse
            else -> Vector3f(0.5f)
        }

        colour = ColorRGB((colourVector[0] * 255).toInt(),  //
                (colourVector[1] * 255).toInt(),  //
                (colourVector[2] * 255).toInt())

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
        if (node is Volume) {

            val slicingModeInput = info.getMutableInput("slicingMode", String::class.java)
            val slicingMethods = Volume.SlicingMode.values().map { mode: Volume.SlicingMode -> mode.toString() }.toList()
            slicingModeInput.setChoices(slicingMethods)
            slicingMode = slicingModeChoices[Volume.SlicingMode.values().indexOf(node .slicingMode)].toString()

            val lutNameItem = info.getMutableInput("colormapName", String::class.java)
            lutNameItem.choices = sciView.getAvailableLUTs()
            val cachedColormapName = node.metadata["sciview.colormapName"] as? String

            if(cachedColormapName != null && sciView.getLUT(cachedColormapName) != null) {
                colormapName = cachedColormapName
            }

            try {
                val cm = sciView.getLUT(colormapName)
                // Ensure the node matches
                node.colormap = fromColorTable(cm)
                node.metadata["sciview.colormapName"] = colormapName

                colormap = cm
            } catch (ioe: IOException) {
                log.error("Could not load LUT $colormapName")
            }

            width = node.getDimensions().x
            height = node.getDimensions().y
            depth = node.getDimensions().z

            min = node.converterSetups[0].displayRangeMin.toInt()
            max = node.converterSetups[0].displayRangeMax.toInt()

            val maxTimepoint = node.timepointCount - 1
            if(maxTimepoint > 0) {
                timepoint = node.currentTimepoint
                val timepointInput = info.getMutableInput("timepoint", java.lang.Integer::class.java)
                timepointInput.minimumValue = java.lang.Integer.valueOf(0) as java.lang.Integer
                timepointInput.maximumValue = java.lang.Integer.valueOf(maxTimepoint) as java.lang.Integer
            } else {
                maybeRemoveInput("timepoint", java.lang.Integer::class.java)
                maybeRemoveInput("playPauseButton", Button::class.java)
                maybeRemoveInput("playSpeed", java.lang.Integer::class.java)
            }

            maybeRemoveInput("colour", ColorRGB::class.java)
            maybeRemoveInputTractogram()
        } else {
            if (node.name == "tractogram parent") {
                // TODO: Check data structure to verify and check that all following relevant objects are there and of
                // correct dataformat
                val firstBrainRegionItem = info.getMutableInput("firstBrainRegion", String::class.java)
                val secondBrainRegionItem = info.getMutableInput("secondBrainRegion", String::class.java)
                val parcellation = node.getChildrenByName("Brain areas")[0]
                val brainAreas = parcellation.metadata["brainAreas"] as ArrayList<String>
                firstBrainRegionItem.choices = brainAreas
                secondBrainRegionItem.choices = brainAreas

                // read fiber length from metadata of tractogram and create sliders with according min/max values
                val tractogram = node.getChildrenByName("Tractogram")[0]
                val minLengthItem = info.getMutableInput("minLength", java.lang.Float::class.java)
                val maxLengthItem = info.getMutableInput("maxLength", java.lang.Float::class.java)
                val minimumLength = 0f as java.lang.Float
                val maximumLength = tractogram.metadata["maxLength"] as java.lang.Float
                minLengthItem.minimumValue = minimumLength
                minLengthItem.maximumValue = maximumLength
                maxLengthItem.minimumValue = minimumLength
                maxLengthItem.maximumValue = maximumLength
                maxLength = maximumLength.toFloat()
                minLengthItem.widgetStyle = NumberWidget.SLIDER_STYLE + ",group:Select Streamlines"
                maxLengthItem.widgetStyle = NumberWidget.SLIDER_STYLE + ",group:Select Streamlines"
                maybeRemoveInputVolume()
                maybeRemoveInput("maxStreamlines", java.lang.Integer::class.java)
            }else if(node.name.contains("Tractogram")){
                maxStreamlines = (node.parent?.metadata?.get("tractogramCalc") as TractogramTools).getMaxStreamlines()
                maybeRemoveInputVolume()
                maybeRemoveInputTractogram()
            }else{
                maybeRemoveInputTractogram()
                maybeRemoveInputVolume()
                maybeRemoveInput("maxStreamlines", java.lang.Integer::class.java)
            }
        }

        if (node is PointLight) {
            intensity = node.intensity
        } else {
            maybeRemoveInput("intensity", java.lang.Float::class.java)
        }

        if (node is Camera) {
            maybeRemoveInput("colour", ColorRGB::class.java)
            maybeRemoveInput("visible", java.lang.Boolean::class.java)
            val scene = node.getScene()
            active = if (scene != null) {
                scene.findObserver() === node
            } else {
                false
            }
        } else {
            maybeRemoveInput("active", java.lang.Boolean::class.java)
        }

        if (node is BoundingGrid) {
            gridColor = ColorRGB(node.gridColor.x().toInt() * 255,
                    node.gridColor.y().toInt() * 255,
                    node.gridColor.z().toInt() * 255)
            ticksOnly = node.ticksOnly > 0
        } else {
            maybeRemoveInput("gridColor", ColorRGB::class.java)
            maybeRemoveInput("ticksOnly", java.lang.Boolean::class.java)
        }

        if (node is TextBoard) {
            text = node.text
            fontColor = ColorRGB(
                    node.fontColor.x().toInt() * 255,
                    node.fontColor.y().toInt() * 255,
                    node.fontColor.z().toInt() * 255
            )
            backgroundColor = ColorRGB(
                    node.backgroundColor.x().toInt() * 255,
                    node.backgroundColor.y().toInt() * 255,
                    node.backgroundColor.z().toInt() * 255
            )
            transparentBackground = node.transparent > 0
        } else {
            maybeRemoveInput("fontColor", ColorRGB::class.java)
            maybeRemoveInput("backgroundColor", ColorRGB::class.java)
            maybeRemoveInput("transparentBackground", java.lang.Boolean::class.java)
            maybeRemoveInput("text", String::class.java)
        }

        if (node is SlicingPlane){
            sciView.hub.get<VolumeManager>()?.let {
                slicedVolumes.availableVolumes = it.nodes
            }
            slicedVolumes.clear()
            slicedVolumes.addAll(node.slicedVolumes)
        } else {
            maybeRemoveInput("slicedVolumes", VolumeSelectorWidget.VolumeSelection::class.java)
        }

        if (node is Line){
            edgeWidth = node.edgeWidth.toInt()
        } else {
            maybeRemoveInput("edgeWidth", java.lang.Integer::class.java)
        }

        name = node.name
        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    protected fun updateNodeProperties() {
        val node = currentSceneNode ?: return
        if (fieldsUpdating) {
            return
        }

        // update visibility
        node.visible = visible

        // update colour
        val cVector = Vector3f(colour!!.red / 255f,
                colour!!.green / 255f,
                colour!!.blue / 255f)
        if (node is PointLight) {
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

        if (node is PointLight) {
            node.intensity = intensity
        }
        if (node is Volume) {
            try {
                val cm = sciView.getLUT(colormapName)
                node.colormap = fromColorTable(cm!!)
                node.metadata["sciview.colormapName"] = colormapName
                log.info("Setting new colormap to $colormapName / $cm")

                colormap = cm
            } catch (ioe: IOException) {
                log.error("Could not load LUT $colormapName")
            }
            node.goToTimepoint(timepoint)
            node.converterSetups[0].setDisplayRange(min.toDouble(), max.toDouble())
            val slicingModeIndex = slicingModeChoices.indexOf(Volume.SlicingMode.valueOf(slicingMode))
            if (slicingModeIndex != -1) {
                node.slicingMode = Volume.SlicingMode.values()[slicingModeIndex]
            }
        }
        if (node is Camera) {
            val scene = node.getScene()
            if (active && scene != null) {
                scene.activeObserver = node
            }
        }
        if (node is BoundingGrid) {
            val ticks = if (ticksOnly) {
                1
            } else {
                0
            }
            node.ticksOnly = ticks
            node.gridColor = Vector3f(
                    gridColor!!.red / 255.0f,
                    gridColor!!.green / 255.0f,
                    gridColor!!.blue / 255.0f
            )
        }
        if (node is TextBoard) {
            val transparent = if (transparentBackground) {
                1
            } else {
                0
            }
            node.transparent = transparent
            node.text = text!!
            node.fontColor = Vector4f(
                    fontColor!!.red / 255.0f,
                    fontColor!!.green / 255.0f,
                    fontColor!!.blue / 255.0f,
                    1f
            )
            node.backgroundColor = Vector4f(
                    backgroundColor!!.red / 255.0f,
                    backgroundColor!!.green / 255.0f,
                    backgroundColor!!.blue / 255.0f,
                    1f
            )
        }

        if (node is SlicingPlane){
            val old = node.slicedVolumes
            val new = slicedVolumes
            val removed = old.filter { !new.contains(it) }
            val added = new.filter{!old.contains(it)}
            removed.forEach { node.removeTargetVolume(it) }
            added.forEach{node.addTargetVolume(it)}
        }

        if (node is Line) {
            node.edgeWidth = edgeWidth.toFloat()
        }

        events.publish(NodeChangedEvent(node))
    }

    private fun makeIdentifier(node: Node, count: Int): String {
        return "" + node.name + "[" + count + "]"
    }

    fun addInput(input: ModuleItem<*>, module: Module) {
        super.addInput(input)
        inputModuleMaps[input] = module
    }

    fun getCustomModuleForModuleItem(moduleInfo: ModuleItem<*>): Module? {
        val custom = inputModuleMaps[moduleInfo]
        log.info("Custom module found: $custom")
        return custom
    }



    companion object {
        private const val PI_NEG = "-3.142"
        private const val PI_POS = "3.142"
        private var dummyColorTable: ColorTable? = null

        private val inputModuleMaps = ConcurrentHashMap<ModuleItem<*>, Module>()

        init {
            dummyColorTable = object : ColorTable {
                override fun lookupARGB(v: Double, v1: Double, v2: Double): Int {
                    return 0
                }

                override fun getComponentCount(): Int {
                    return 0
                }

                override fun getLength(): Int {
                    return 0
                }

                override fun get(i: Int, i1: Int): Int {
                    return 0
                }

                override fun getResampled(i: Int, i1: Int, i2: Int): Int {
                    return 0
                }
            }
        }
    }
}
