package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.ControllerDrag
import graphics.scenery.controls.behaviours.VRTouch
import graphics.scenery.primitives.Cylinder
import graphics.scenery.ui.Button
import graphics.scenery.ui.Column
import graphics.scenery.ui.Gui3DElement
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.RAIVolume
import graphics.scenery.volumes.Volume
import org.joml.*
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import sc.iview.commands.demo.advanced.HedgehogAnalysis.SpineGraphVertex
import sc.iview.controls.behaviours.MoveInstanceVR
import sc.iview.controls.behaviours.MultiVRButtonStateManager
import sc.iview.controls.behaviours.VR2HandNodeTransform
import sc.iview.controls.behaviours.VRGrabTheWorld
import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Base class for different VR cell tracking purposes. It includes functionality to add spines and edgehogs,
 * as used by [EyeTracking], and registers controller bindings via [inputSetup]. It is possible to register observers
 * that listen to timepoint changes with [registerObserver].
 * @param [sciview] The [SciView] instance to use
 */
open class CellTrackingBase(
    open var sciview: SciView
) {
    val logger by lazyLogger()

    lateinit var sessionId: String
    lateinit var sessionDirectory: Path

    lateinit var hmd: OpenVRHMD

    val hedgehogs = Mesh()
    val hedgehogIds = AtomicInteger(0)
    lateinit var volume: Volume

    val referenceTarget = Icosphere(0.004f, 2)

    @Volatile var tracking = false
    var playing = true
    var direction = PlaybackDirection.Backward
    var volumesPerSecond = 1
    var skipToNext = false
    var skipToPrevious = false

    var volumeScaleFactor = 1.0f

    private lateinit var lightTetrahedron: List<PointLight>

    // determines whether the volume and hedgehogs should keep listening for updates or not
    var cellTrackingActive: Boolean = false

    /** Takes a [SpineGraphVertex] and its positions to create the corresponding track in Mastodon. */
    var trackCreationCallback: ((List<Pair<Vector3f, SpineGraphVertex>>) -> Unit)? = null
    var spotCreationCallback: ((tp: Int, sciviewPos: Vector3f, connectToPrev: Boolean) -> Unit)? = null
    /** Select a spot based on the controller tip's position, current time point and a multiple of the radius
     * in which a selection event is counted as valid. */
    var spotSelectionCallback: ((sciviewPos: Vector3f, tp: Int, radiusFactor: Float) -> Boolean)? = null
    /** Deletes the currently selected spot. */
    var spotDeletionCallback: (() -> Unit)? = null
    var spotMoveInitCallback: ((Vector3f) -> Unit)? = null
    var spotMoveDragCallback: ((Vector3f) -> Unit)? = null
    var spotMoveEndCallback: ((Vector3f) -> Unit)? = null
    /** Links a selected spot to the previously created spot. */
    var spotLinkCallback: (() -> Unit)? = null
    /** Resets the previously created spot to null, so a new track can be generated with the controller. */
    var resetTrackingCallback: (() -> Unit)? = null
    var rebuildGeometryCallback: (() -> Unit)? = null

    var stageSpotsCallback: (() -> Unit)? = null
    var predictSpotsCallback: (() -> Unit)? = null
    var trainSpotsCallback: (() -> Unit)? = null
    var neighborLinkingCallback: (() -> Unit)? = null
    // TODO add train flow functionality
    var trainFlowCallback: (() -> Unit)? = null

    enum class HedgehogVisibility { Hidden, PerTimePoint, Visible }

    enum class PlaybackDirection { Forward, Backward }

    enum class ElephantMode { stageSpots, trainAll, predict, NNLinking }

    private val tools = mapOf("Create" to "addSpotWithController",
        "Edit" to "selectSpotWithController",
        "Delete" to "deleteSpotWithController",
        "Track" to "trackCellWithController",)

    private var currentTool = "Edit"

    var hedgehogVisibility = HedgehogVisibility.Hidden

    var leftVRController: TrackedDevice? = null
    var rightVRController: TrackedDevice? = null

    var tip = Sphere(0.007f)
    var leftToolColumn: Column? = null

    var leftElephantColumn: Column? = null

    val buttonManager = MultiVRButtonStateManager()

    private val observers = mutableListOf<TimepointObserver>()

    val deleteSpotBehavior = ClickBehaviour { _, _ ->
        spotDeletionCallback?.invoke()
        rebuildGeometryCallback?.invoke()
    }

    val resetControllerTrack = ClickBehaviour { _, _ ->
        resetTrackingCallback?.invoke()
    }

    open fun run() {
        sciview.toggleVRRendering()
        logger.info("VR mode has been toggled")
        hmd = sciview.hub.getWorkingHMD() as? OpenVRHMD ?: throw IllegalStateException("Could not find headset")

        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }

        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        shell.name = "Shell"
        sciview.addNode(shell)

        lightTetrahedron = Light.createLightTetrahedron<PointLight>(
            Vector3f(0.0f, 0.0f, 0.0f),
            spread = 5.0f,
            radius = 15.0f,
            intensity = 5.0f
        )
        lightTetrahedron.forEach { sciview.addNode(it) }

        val volnodes = sciview.findNodes { node -> Volume::class.java.isAssignableFrom(node.javaClass) }

        val v = (volnodes.firstOrNull() as? Volume)
        if(v == null) {
            logger.warn("No volume found, bailing")
            return
        } else {
            logger.info("found ${volnodes.size} volume nodes. Using the first one: ${volnodes.first()}")
            volume = v
        }

        thread {
            logger.info("Adding onDeviceConnect handlers")
            hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
                logger.info("onDeviceConnect called, cam=${sciview.camera}")
                if(device.type == TrackedDeviceType.Controller) {
                    logger.info("Got device ${device.name} at $timestamp")
                    device.model?.let { hmd.attachToNode(device, it, sciview.camera) }
                    when (device.role) {
                        TrackerRole.Invalid -> {}
                        TrackerRole.LeftHand -> leftVRController = device
                        TrackerRole.RightHand -> rightVRController = device
                    }
                    if (device.role == TrackerRole.RightHand) {
                        addTip()
                        device.name = "rightHand"
                        leftVRController?.name = "leftHand"
                    } else if (device.role == TrackerRole.LeftHand) {
                        device.name = "leftHand"
                        rightVRController?.name = "rightHand"
                    }
                }
            }
        }

        thread {
            logger.info("started thread for inputSetup")
            inputSetup()
        }

        cellTrackingActive = true
        launchUpdaterThread()
    }

    /** Registers a new observer that will get updated whenever the VR user triggers a timepoint update. */
    fun registerObserver(observer: TimepointObserver) {
        observers.add(observer)
    }

    /** Unregisters the timepoint observer. */
    fun unregisterObserver(observer: TimepointObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers(timepoint: Int) {
        observers.forEach { it.onTimePointChanged(timepoint) }
    }

    private fun setupLeftToolMenu() {
        val createButton = Button(
                "Create", command = this::selectCreateButton, byTouch =  true, stayPressed = true,
                color = Vector3f(0.5f, 0.95f, 0.45f),
                pressedColor = Vector3f(0.2f, 1f, 0.15f),
            )
        val editButton = Button(
            "Edit", command = this::selectEditButton, byTouch =  true, stayPressed = true,
            color = Vector3f(0.4f, 0.45f, 0.95f),
            pressedColor = Vector3f(0.15f, 0.2f, 1f)
        )

        val trackButton = Button(
            "Track", command = this::selectTrackButton, byTouch =  true, stayPressed = true,
            color = Vector3f(0.9f, 0.9f, 0.5f),
            pressedColor = Vector3f(1f, 1f, 0.2f)
        )

        leftToolColumn = createGenericWristMenu(createButton, editButton, trackButton)
        leftToolColumn?.name = "Left Tool Column"
        // Use editing as default option
        selectEditButton()
    }

    private fun createGenericWristMenu(vararg elements: Gui3DElement): Column {
        val column = Column(*elements, centerVertically = true, centerHorizontally = true)
        column.ifSpatial {
            scale = Vector3f(0.05f)
            position = Vector3f(0.05f, 0.05f, column.height / 20f + 0.1f)
            rotation = Quaternionf().rotationXYZ(-1.57f, 1.57f, 0f)
        }
        leftVRController?.model?.let {
            sciview.addNode(column, parent = it)
            column.children.forEach { child ->
                val bb = BoundingGrid()
                bb.node = child
                bb.gridColor = Vector3f(0.5f, 1f, 0.4f)
                sciview.addNode(bb, parent = it)
            }
        }
        column.pack()
        logger.info("height for column is ${column.height}")
        return column
    }

    val addSpotWithController = ClickBehaviour { _, _ ->
        val p = getTipPosition()
        logger.debug("Got tip position: $p")
        spotCreationCallback?.invoke(volume.currentTimepoint, p, false)
    }

    val selectSpotWithController = ClickBehaviour { _, _ ->
        val p = getTipPosition()
        logger.debug("Got tip position: $p")
        spotSelectionCallback?.invoke(p, volume.currentTimepoint, 2f)
    }

    val trackCellsWithController = ClickBehaviour { _, _ ->
        // we dont want animation, because we track step by step
        playing = false
        // play the volume backwards, step by step, so cell split events can simply be turned into a merge event
        if (volume.currentTimepoint > 0) {
            val p = getTipPosition()
            logger.debug("Got tip position: $p")
            // did the user click on an existing cell and wants to merge the track into it?
            val wantMerge = spotSelectionCallback?.invoke(p, volume.currentTimepoint, 1.5f) ?: false
            if (!wantMerge) {
                logger.info("Tracked a new spot at position $p")
                spotCreationCallback?.invoke(volume.currentTimepoint, p, true)
            } else {
                spotLinkCallback?.invoke()
            }
            volume.goToTimepoint(volume.currentTimepoint - 1)
            notifyObservers(volume.currentTimepoint)
        } else {
            sciview.camera?.showMessage("Reached the first time point!", centered = true)
        }
    }

    private fun updateLeftToolButtonActions(pressed: String) {
        leftToolColumn?.let {
            it.children.filterIsInstance<Button>().forEach {
                // force release all buttons that are currently not selected
                if (it.text != pressed) {
                    it.release(true)
                }
            }
        }
        // remove the currently selected tool
        unregisterCurrentTool()
        // add the currently selected tool as behavior
        val behavior = when (pressed) {
            "Create" -> addSpotWithController
            "Edit" -> selectSpotWithController
            "Track" -> trackCellsWithController
            else -> null
        }
        currentTool = pressed
        behavior?.let {
            hmd.addBehaviour(tools[pressed]!!, behavior)
            hmd.addKeyBinding(tools[pressed]!!, TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Trigger)
        }
    }

    private fun selectCreateButton() {
        logger.info("Selected the spot creation tool")
        updateLeftToolButtonActions("Create")
        tip.material { diffuse = Vector3f(0.2f, 1f, 0.15f) }
    }

    private fun selectEditButton() {
        logger.info("Selected the spot editing tool")
        updateLeftToolButtonActions("Edit")
        tip.material { diffuse = Vector3f(0.15f, 0.2f, 1f) }
        hmd.addBehaviour("DeleteSpot", deleteSpotBehavior)
        hmd.addKeyBinding("DeleteSpot", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.A)
    }

    private fun selectTrackButton() {
        logger.info("Selected the cell tracking tool")
        updateLeftToolButtonActions("Track")
        tip.material { diffuse = Vector3f(1f, 1f, 0.2f) }
        resetTrackingCallback?.invoke()
        hmd.addBehaviour("ResetTrack", resetControllerTrack)
        hmd.addKeyBinding("ResetTrack", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.A)
        volume.goToLastTimepoint()
    }

    private fun unregisterCurrentTool() {

        fun maybeRemoveBehavior(s: String) {
            val b = hmd.getBehaviour(s)
            if (b != null) {
                hmd.removeBehaviour(s)
                hmd.removeKeyBinding(s)
                logger.info("unregistered $s")
            }
        }

        tools[currentTool]?.let {
            maybeRemoveBehavior(it)
        }
        // Remove additional behaviors that might have been added
        when (currentTool) {
            "Edit" -> maybeRemoveBehavior("DeleteSpot")
            "Track" -> maybeRemoveBehavior("ResetTrack")
        }
    }

    fun setupElephantMenu() {
        val unpressedColor = Vector3f(0.81f, 0.81f, 1f)
        val pressedColor = Vector3f(0.54f, 0.44f, 1f)
        val stageSpotsButton = Button(
            "Stage all",
            command = { updateElephantActions(ElephantMode.stageSpots) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, pressedColor = pressedColor)
        val trainAllButton = Button(
            "Train All TPs",
            command = { updateElephantActions(ElephantMode.trainAll) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, pressedColor = pressedColor)
        val predictAllButton = Button(
            "Predict All TPs",
            command = {updateElephantActions(ElephantMode.predict) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, pressedColor = pressedColor)
        val linkingButton = Button(
            "NN linking",
            command = {updateElephantActions(ElephantMode.NNLinking) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, pressedColor = pressedColor)

        leftElephantColumn =
            createGenericWristMenu(stageSpotsButton, trainAllButton, predictAllButton, linkingButton)
        leftElephantColumn?.name = "Left Elephant Menu"
    }

    var lastButtonTime = TimeSource.Monotonic.markNow()

    private fun updateElephantActions(mode: ElephantMode) {
        val buttonTime = TimeSource.Monotonic.markNow()

        if (buttonTime.minus(lastButtonTime) > 2.0.seconds) {
            when (mode) {
                ElephantMode.stageSpots -> stageSpotsCallback?.invoke()
                ElephantMode.trainAll -> trainSpotsCallback?.invoke()
                ElephantMode.predict -> predictSpotsCallback?.invoke()
                ElephantMode.NNLinking -> neighborLinkingCallback?.invoke()
            }
        } else {
            sciview.camera?.showMessage("Have some patience!", duration = 1500)
        }
        lastButtonTime = buttonTime
    }

    private fun toggleLeftWristMenus() {
        if (leftToolColumn == null) {
            logger.info("Setting up left wrist menu")
            setupLeftToolMenu()
            leftToolColumn!!.visible = false
        }
        if (leftElephantColumn == null) {
            logger.info("Setting up left elephant menu")
            setupElephantMenu()
            leftElephantColumn!!.visible = true
        }

        leftElephantColumn!!.visible = !leftElephantColumn!!.visible
        leftToolColumn!!.visible = !leftToolColumn!!.visible
        val ev = leftElephantColumn!!.visible
        logger.info("Toggled from ${if (ev) "tool" else "elephant"} menu to ${if (!ev) "tool" else "elephant"} menu.")
    }


    fun addHedgehog() {
        logger.info("added hedgehog")
        val hedgehog = Cylinder(0.005f, 1.0f, 16)
        hedgehog.visible = false
        hedgehog.setMaterial(ShaderMaterial.fromFiles("DeferredInstancedColor.frag", "DeferredInstancedColor.vert"))
        val hedgehogInstanced = InstancedNode(hedgehog)
        hedgehogInstanced.visible = false
        hedgehogInstanced.instancedProperties["ModelMatrix"] = { hedgehog.spatial().world}
        hedgehogInstanced.instancedProperties["Metadata"] = { Vector4f(0.0f, 0.0f, 0.0f, 0.0f) }
        hedgehogs.addChild(hedgehogInstanced)
    }

    fun addTip() {
        tip.material {
            diffuse = Vector3f(0.15f, 0.2f, 1f)
        }
        tip.spatial().position = Vector3f(0.0f, -0f, -0.05f)
//        val bb = BoundingGrid()
//        bb.node = tip
//        bb.name = "Tip BB"
//        bb.lineWidth = 2f
//        bb.gridColor = Vector3f(1f, 0.3f, 0.25f)
        rightVRController?.model?.let {
            sciview.addNode(tip, parent = it)
//            sciview.addNode(bb, parent = it)
            logger.info("Added tip to right controller")
        }
    }

    open fun inputSetup()
    {
        val cam = sciview.camera ?: throw IllegalStateException("Could not find camera")

        sciview.sceneryInputHandler?.let { handler ->
            hashMapOf(
                "move_forward_fast" to (TrackerRole.LeftHand to OpenVRHMD.OpenVRButton.Up),
                "move_back_fast" to (TrackerRole.LeftHand to OpenVRHMD.OpenVRButton.Down),
                "move_left_fast" to (TrackerRole.LeftHand to OpenVRHMD.OpenVRButton.Left),
                "move_right_fast" to (TrackerRole.LeftHand to OpenVRHMD.OpenVRButton.Right)).forEach { (name, key) ->
                handler.getBehaviour(name)?.let { b ->
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key.first, key.second)
                }
            }
        }

        val toggleHedgehog = ClickBehaviour { _, _ ->
            val current = HedgehogVisibility.entries.indexOf(hedgehogVisibility)
            hedgehogVisibility = HedgehogVisibility.entries.get((current + 1) % 3)

            when(hedgehogVisibility) {
                HedgehogVisibility.Hidden -> {
                    hedgehogs.visible = false
                    hedgehogs.runRecursive { it.visible = false }
                    cam.showMessage("Hedgehogs hidden",distance = 2f, size = 0.2f, centered = true)
                }

                HedgehogVisibility.PerTimePoint -> {
                    hedgehogs.visible = true
                    cam.showMessage("Hedgehogs shown per timepoint",distance = 2f, size = 0.2f, centered = true)
                }

                HedgehogVisibility.Visible -> {
                    hedgehogs.visible = true
                    cam.showMessage("Hedgehogs visible",distance = 2f, size = 0.2f, centered = true)
                }
            }
        }

        val nextTimepoint = ClickBehaviour { _, _ ->
            skipToNext = true
        }

        val prevTimepoint = ClickBehaviour { _, _ ->
            skipToPrevious = true
        }

        val faster = ClickBehaviour { _, _ ->
            volumesPerSecond = maxOf(minOf(volumesPerSecond+1, 20), 1)
            cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 1.2f, size = 0.2f, centered = true)
        }

        val slower = ClickBehaviour { _, _ ->
            volumesPerSecond = maxOf(minOf(volumesPerSecond-1, 20), 1)
            cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 2f, size = 0.2f, centered = true)
        }

        val playPause = ClickBehaviour { _, _ ->
            playing = !playing
            if(playing) {
                cam.showMessage("Playing",distance = 2f, size = 0.2f, centered = true)
            } else {
                cam.showMessage("Paused",distance = 2f, size = 0.2f, centered = true)
            }
        }

        val move = ControllerDrag(TrackerRole.LeftHand, hmd) { volume }

        val deleteLastHedgehog = ConfirmableClickBehaviour(
            armedAction = { timeout ->
                cam.showMessage("Deleting last track, press again to confirm.",distance = 2f, size = 0.2f,
                    messageColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                    backgroundColor = Vector4f(1.0f, 0.2f, 0.2f, 1.0f),
                    duration = timeout.toInt(),
                    centered = true)

            },
            confirmAction = {
                hedgehogs.children.removeLast()
                volume.children.last { it.name.startsWith("Track-") }?.let { lastTrack ->
                    volume.removeChild(lastTrack)
                }
                val hedgehogId = hedgehogIds.get()
                val hedgehogFile = sessionDirectory.resolve("Hedgehog_${hedgehogId}_${SystemHelpers.formatDateTime()}.csv").toFile()
                val hedgehogFileWriter = BufferedWriter(FileWriter(hedgehogFile, true))
                hedgehogFileWriter.newLine()
                hedgehogFileWriter.newLine()
                hedgehogFileWriter.write("# WARNING: TRACK $hedgehogId IS INVALID\n")
                hedgehogFileWriter.close()

                cam.showMessage("Last track deleted.",distance = 2f, size = 0.2f,
                    messageColor = Vector4f(1.0f, 0.2f, 0.2f, 1.0f),
                    backgroundColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                    duration = 1000,
                    centered = true
                )
            })

//        hmd.addBehaviour("playback_direction", ClickBehaviour { _, _ ->
//            direction = if(direction == PlaybackDirection.Forward) {
//                PlaybackDirection.Backward
//            } else {
//                PlaybackDirection.Forward
//            }
//            cam.showMessage("Playing: ${direction}", distance = 2f, centered = true)
//        })

//        val cellDivision = ClickBehaviour { _, _ ->
//            cam.showMessage("Adding cell division", distance = 2f, duration = 1000)
//            dumpHedgehog()
//            addHedgehog()
//        }

        hmd.addBehaviour("skip_to_next", nextTimepoint)
        hmd.addBehaviour("skip_to_prev", prevTimepoint)
        hmd.addBehaviour("faster", faster)
        hmd.addBehaviour("slower", slower)
        hmd.addBehaviour("play_pause", playPause)
        hmd.addKeyBinding("skip_to_next", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Right)
        hmd.addKeyBinding("skip_to_prev", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Left)
        hmd.addKeyBinding("faster", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Up)
        hmd.addKeyBinding("slower", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Down)
        hmd.addKeyBinding("play_pause", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Menu)

//        hmd.addBehaviour("toggle_hedgehog", toggleHedgehog)
//        hmd.addBehaviour("delete_hedgehog", deleteLastHedgehog)
//        hmd.addBehaviour("cell_division", cellDivision)
//        hmd.addKeyBinding("cell_division", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Trigger)

//        hmd.addBehaviour("trigger_move", move)
//        hmd.addKeyBinding("trigger_move", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Side)

        hmd.addBehaviour("toggleLeftWristMenu", ClickBehaviour { _, _ -> toggleLeftWristMenus() })
        hmd.addKeyBinding("toggleLeftWristMenu", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.A)

        // this behavior is needed for touching the menu buttons
        VRTouch.createAndSet(sciview.currentScene, hmd, listOf(TrackerRole.RightHand), false, customTip = tip)

        VRGrabTheWorld.createAndSet(
            sciview.currentScene, hmd, listOf(OpenVRHMD.OpenVRButton.Side), listOf(TrackerRole.LeftHand), buttonManager
        )

        VR2HandNodeTransform.createAndSet(
            hmd,
            OpenVRHMD.OpenVRButton.Side,
            sciview.currentScene,
            target = volume,
            onEndCallback = rebuildGeometryCallback
        )

        // drag behavior can stay enabled regardless of current tool mode
        MoveInstanceVR.createAndSet(
            sciview.currentScene, hmd, listOf(OpenVRHMD.OpenVRButton.Side), listOf(TrackerRole.RightHand),
            buttonManager,
            { getTipPosition() },
            spotMoveInitCallback,
            spotMoveDragCallback,
            spotMoveEndCallback,
        )

//        hmd.addKeyBinding("toggle_hedgehog", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Side)
//        hmd.addKeyBinding("delete_hedgehog", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Side)

//        hmd.addKeyBinding("playback_direction", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Menu)

        hmd.allowRepeats += OpenVRHMD.OpenVRButton.Trigger to TrackerRole.LeftHand
        logger.info("Registered VR controller bindings.")

    }

    /** Returns the world position of the right controller's tool tip as [Vector3f]. */
    fun getTipPosition(): Vector3f {
        return tip.boundingBox?.center ?: Vector3f(0f)
    }

    /**
     * Launches a thread that updates the volume time points, the hedgehog visibility and reference target color.
     */
    fun launchUpdaterThread() {
        thread {
            while(!sciview.isInitialized) { Thread.sleep(200) }

            while(sciview.running && cellTrackingActive) {
                if(playing || skipToNext || skipToPrevious) {
                    val oldTimepoint = volume.viewerState.currentTimepoint
                    if (skipToNext || playing) {
                        skipToNext = false
                        if(direction == PlaybackDirection.Forward) {
                            notifyObservers(oldTimepoint + 1)
                        } else {
                            notifyObservers(oldTimepoint - 1)
                        }
                    } else {
                        skipToPrevious = false
                        if(direction == PlaybackDirection.Forward) {
                            notifyObservers(oldTimepoint - 1)
                        } else {
                            notifyObservers(oldTimepoint + 1)
                        }
                    }
                    val newTimepoint = volume.viewerState.currentTimepoint


                    if(hedgehogs.visible) {
                        if(hedgehogVisibility == HedgehogVisibility.PerTimePoint) {
                            hedgehogs.children.forEach { hh ->
                                val hedgehog = hh as InstancedNode
                                hedgehog.instances.forEach {
                                    if (it.metadata.isNotEmpty()) {
                                        it.visible = (it.metadata["spine"] as SpineMetadata).timepoint == volume.viewerState.currentTimepoint
                                    }
                                }
                            }
                        } else {
                            hedgehogs.children.forEach { hh ->
                                val hedgehog = hh as InstancedNode
                                hedgehog.instances.forEach { it.visible = true }
                            }
                        }
                    }

                    if(tracking && oldTimepoint == (volume.timepointCount-1) && newTimepoint == 0) {
                        tracking = false

                        referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f)}
                        sciview.camera!!.showMessage("Tracking deactivated.",distance = 1.2f, size = 0.2f)
                        dumpHedgehog()
                    }
                }

                Thread.sleep((1000.0f/volumesPerSecond).toLong())
            }
            logger.info("CellTracking updater thread has stopped.")
        }
    }

    open fun addSpine(center: Vector3f, direction: Vector3f, volume: Volume, confidence: Float, timepoint: Int) {
        val cam = sciview.camera as? DetachedHeadCamera ?: return
        val sphere = volume.boundingBox?.getBoundingSphere() ?: return

        val sphereDirection = sphere.origin.minus(center)
        val sphereDist = Math.sqrt(sphereDirection.x * sphereDirection.x + sphereDirection.y * sphereDirection.y + sphereDirection.z * sphereDirection.z) - sphere.radius

        val p1 = center
        val temp = direction.mul(sphereDist + 2.0f * sphere.radius)
        val p2 = Vector3f(center).add(temp)

        val spine = (hedgehogs.children.last() as InstancedNode).addInstance()
        spine.spatial().orientBetweenPoints(p1, p2, true, true)
        spine.visible = false

        val intersection = volume.spatial().intersectAABB(p1, (p2 - p1).normalize(), true)

        if (volume.boundingBox?.isInside(cam.spatial().position)!!) {
            logger.info("Can't track inside the volume! Please move out of the volume and try again")
            return
        }
        if(intersection is MaybeIntersects.Intersection) {
            // get local entry and exit coordinates, and convert to UV coords
            val localEntry = (intersection.relativeEntry)
            val localExit = (intersection.relativeExit)
            // TODO We dont need the local direction for grid traversal, but its still in the spine metadata for now
            val localDirection = Vector3f(0f)
            val (samples, samplePos) = volume.sampleRayGridTraversal(localEntry, localExit) ?: (null to null)
            val volumeScale = (volume as RAIVolume).getVoxelScale()

            if (samples != null && samplePos != null) {
                val metadata = SpineMetadata(
                    timepoint,
                    center,
                    direction,
                    intersection.distance,
                    localEntry,
                    localExit,
                    localDirection,
                    cam.headPosition,
                    cam.headOrientation,
                    cam.spatial().position,
                    confidence,
                    samples.map { it ?: 0.0f },
                    samplePos.map { it?.mul(volumeScale) ?: Vector3f(0f) }
                )
                val count = samples.filterNotNull().count { it > 0.2f }

                spine.metadata["spine"] = metadata
                spine.instancedProperties["ModelMatrix"] = { spine.spatial().world }
                // TODO: Show confidence as color for the spine
                spine.instancedProperties["Metadata"] =
                    { Vector4f(confidence, timepoint.toFloat() / volume.timepointCount, count.toFloat(), 0.0f) }
            }
        }
    }

    /**
     * Dumps a given hedgehog including created tracks to a file.
     * If [hedgehog] is null, the last created hedgehog will be used, otherwise the given one.
     * If [hedgehog] is not null, the cell track will not be added to the scene.
     */
    fun dumpHedgehog(){
        logger.info("dumping hedgehog...")
        val lastHedgehog =  hedgehogs.children.last() as InstancedNode
        val hedgehogId = hedgehogIds.incrementAndGet()

        val hedgehogFile = sessionDirectory.resolve("Hedgehog_${hedgehogId}_${SystemHelpers.formatDateTime()}.csv").toFile()
        val hedgehogFileWriter = hedgehogFile.bufferedWriter()
        hedgehogFileWriter.write("Timepoint,Origin,Direction,LocalEntry,LocalExit,LocalDirection,HeadPosition,HeadOrientation,Position,Confidence,Samples\n")

        val trackFile = sessionDirectory.resolve("Tracks.tsv").toFile()
        val trackFileWriter = BufferedWriter(FileWriter(trackFile, true))
        if(!trackFile.exists()) {
            trackFile.createNewFile()
            trackFileWriter.write("# BionicTracking cell track listing for ${sessionDirectory.fileName}\n")
            trackFileWriter.write("# TIME\tX\tYt\t\tZ\tTRACK_ID\tPARENT_TRACK_ID\tSPOT\tLABEL\n")
        }


        val spines = lastHedgehog.instances.mapNotNull { spine ->
            spine.metadata["spine"] as? SpineMetadata
        }

        spines.forEach { metadata ->
            hedgehogFileWriter.write("${metadata.timepoint};${metadata.origin};${metadata.direction};${metadata.localEntry};${metadata.localExit};${metadata.localDirection};${metadata.headPosition};${metadata.headOrientation};${metadata.position};${metadata.confidence};${metadata.samples.joinToString(";")}\n")
        }
        hedgehogFileWriter.close()

        val existingAnalysis = lastHedgehog.metadata["HedgehogAnalysis"] as? HedgehogAnalysis.Track
        val track = if(existingAnalysis is HedgehogAnalysis.Track) {
            existingAnalysis
        } else {
            val h = HedgehogAnalysis(spines, Matrix4f(volume.spatial().world))
            h.run()
        }

        if(track == null) {
            logger.warn("No track returned")
            sciview.camera?.showMessage("No track returned", distance = 1.2f, size = 0.2f,messageColor = Vector4f(1.0f, 0.0f, 0.0f,1.0f))
            return
        }

        lastHedgehog.metadata["HedgehogAnalysis"] = track
        lastHedgehog.metadata["Spines"] = spines

        val parentId = 0
        val volumeDimensions = volume.getDimensions()

        trackFileWriter.newLine()
        trackFileWriter.newLine()
        trackFileWriter.write("# START OF TRACK $hedgehogId, child of $parentId\n")
        if (trackCreationCallback != null && rebuildGeometryCallback != null) {
            trackCreationCallback?.invoke(track.points)
            rebuildGeometryCallback?.invoke()
        }
        track.points.windowed(2, 1).forEach { pair ->
            val p = Vector3f(pair[0].first).mul(Vector3f(volumeDimensions)) // direct product
            val tp = pair[0].second.timepoint
            trackFileWriter.write("$tp\t${p.x()}\t${p.y()}\t${p.z()}\t${hedgehogId}\t$parentId\t0\t0\n")
        }
        trackFileWriter.close()
    }

    /**
     * Stops the current tracking environment and restore the original state.
     * This method should be overridden if functionality is extended, to make sure any extra objects are also deleted.
     */
    open fun stop() {
        lightTetrahedron.forEach { sciview.deleteNode(it) }
        // Try to find and delete possibly existing VR objects
        listOf("Shell", "leftHand", "rightHand").forEach {
            val n = sciview.find(it)
            n?.let { sciview.deleteNode(n) }
        }
        logger.info("Cleaned up basic VR objects.")
        sciview.toggleVRRendering()
        logger.info("Shut down eye tracking environment and disabled VR.")
    }

}