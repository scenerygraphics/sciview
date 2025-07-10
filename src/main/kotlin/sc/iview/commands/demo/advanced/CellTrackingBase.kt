package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.AnalogInputWrapper
import graphics.scenery.controls.behaviours.VRTouch
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import graphics.scenery.ui.*
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.*
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.RAIVolume
import graphics.scenery.volumes.Volume
import org.joml.*
import org.mastodon.mamut.model.Spot
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView
import sc.iview.commands.demo.advanced.HedgehogAnalysis.SpineGraphVertex
import sc.iview.controls.behaviours.MoveInstanceVR
import sc.iview.controls.behaviours.MultiButtonManager
import sc.iview.controls.behaviours.VR2HandNodeTransform
import sc.iview.controls.behaviours.VRGrabTheWorld
import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Base class for different VR cell tracking purposes. It includes functionality to add spines and edgehogs,
 * as used by [EyeTracking], and registers controller bindings via [inputSetup]. It is possible to register observers
 * that listen to timepoint changes with [registerObserver].
 * @param [sciview] The [SciView] instance to use
 */
open class CellTrackingBase(
    open var sciview: SciView
) {
    val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    lateinit var sessionId: String
    lateinit var sessionDirectory: Path

    lateinit var hmd: OpenVRHMD

    val hedgehogs = Mesh()
    val hedgehogIds = AtomicInteger(0)
    lateinit var volume: Volume

    val referenceTarget = Icosphere(0.004f, 2)

    @Volatile var eyeTrackingActive = false
    var playing = false
    var direction = PlaybackDirection.Backward
    var volumesPerSecond = 6f
    var skipToNext = false
    var skipToPrevious = false

    var volumeScaleFactor = 1.0f

    private lateinit var lightTetrahedron: List<PointLight>

    val volumeTPWidget = TextBoard()

    /** determines whether the volume and hedgehogs should keep listening for updates or not */
    var cellTrackingActive: Boolean = false

    /** Takes a list of [SpineGraphVertex] and its positions to create the corresponding track in Mastodon.
     * In the case of controller tracking, the points were already sent to Mastodon one by one via [singleLinkTrackedCallback] and the list is not needed.
     * Set the first boolean to true if the coordinates are in world space. The bridge will convert them to Mastodon coords.
     * The first Spot defines whether to start with an existing spot, so the lambda will use that as starting point.
     * The second spot defines whether we want to merge into this spot. */
    var trackCreationCallback: ((List<Pair<Vector3f, SpineGraphVertex>>?, Boolean, Spot?, Spot?) -> Unit)? = null
    /** Passes the current time point, the cursor position and its radius to the bridge to either create a new spot
     * or delete an existing spot if there is a spot selected.
     * The deleteBranch flag indicates whether we want to delete the whole branch or just a spot.  */
    var spotCreateDeleteCallback: ((tp: Int, sciviewPos: Vector3f, radius: Float, deleteBranch: Boolean) -> Unit)? = null
    /** Select a spot based on the controller tip's position, current time point and a multiple of the radius
     * in which a selection event is counted as valid. addOnly prevents deselection from clicking away. */
    var spotSelectCallback: ((sciviewPos: Vector3f, tp: Int, radiusFactor: Float, addOnly: Boolean) -> Pair<Spot?, Boolean>)? = null
    var spotMoveInitCallback: ((Vector3f) -> Unit)? = null
    var spotMoveDragCallback: ((Vector3f) -> Unit)? = null
    var spotMoveEndCallback: ((Vector3f) -> Unit)? = null
    /** Links a selected spot to the closest spot to handle merge events. */
    var spotLinkCallback: (() -> Unit)? = null
    /** Generates a single link between a new position and the previously annotated one.
     * Sends the position data to the bridge for intermediary keeping. The integer is the timepoint.
     * The Float contains the cursor's radius in sciview space.
     * The boolean specifies whether the link preview should be rendered. */
    var singleLinkTrackedCallback: ((pos: Vector3f, tp: Int, radius: Float, preview: Boolean) -> Unit)? = null
    var toggleTrackingPreviewCallback: ((Boolean) -> Unit)? = null
    var rebuildGeometryCallback: (() -> Unit)? = null

    var stageSpotsCallback: (() -> Unit)? = null
    var predictSpotsCallback: ((all: Boolean) -> Unit)? = null
    var trainSpotsCallback: (() -> Unit)? = null
    var neighborLinkingCallback: (() -> Unit)? = null
    // TODO add train flow functionality
    var trainFlowCallback: (() -> Unit)? = null
    /** Reverts to the point previously saved by Mastodon's undo recorder. */
    var mastodonUndoCallback: (() -> Unit)? = null
    /** Returns a list of spots currently selected in Mastodon. Used to determine whether to scale the cursor or the spots. */
    var getSelectionCallback: (() -> List<InstancedNode.Instance>)? = null
    /** Adjusts the radii of spots, both in sciview and Mastodon. */
    var scaleSpotsCallback: ((radius: Float, update: Boolean) -> Unit)? = null

    enum class HedgehogVisibility { Hidden, PerTimePoint, Visible }

    enum class PlaybackDirection { Forward, Backward }

    enum class ElephantMode { StageSpots, TrainAll, PredictTP, PredictAll, NNLinking }

    var hedgehogVisibility = HedgehogVisibility.Hidden

    var leftVRController: TrackedDevice? = null
    var rightVRController: TrackedDevice? = null

    var cursor = CursorTool
    var leftElephantColumn: Column? = null
    var leftColumnPredict: Column? = null
    var leftColumnLink: Column? = null
    var leftUndoMenu: Column? = null

    var enableTrackingPreview = true

    val leftMenuList = mutableListOf<Column>()
    var leftMenuIndex = 0

    val grabButtonManager = MultiButtonManager()
    val resetRotationBtnManager = MultiButtonManager()

    val mapper = CellTrackingButtonMapper

    private val observers = mutableListOf<TimepointObserver>()

    open fun run() {
        sciview.toggleVRRendering()
        logger.info("VR mode has been toggled")
        hmd = sciview.hub.getWorkingHMD() as? OpenVRHMD ?: throw IllegalStateException("Could not find headset")

        // Try to load the correct button mapping corresponding to the controller layout
        val isProfileLoaded = mapper.loadProfile(hmd.manufacturer)
        if (!isProfileLoaded) {
            throw IllegalStateException("Could not load profile, headset type unknown!")
        }
        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial {
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f)
        }

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
                        attachCursorAndTPWidget()
                        device.model?.name = "rightHand"
                    } else if (device.role == TrackerRole.LeftHand) {
                        device.model?.name = "leftHand"
                        setupElephantMenu()
                        setupGeneralMenu()
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

    /** Notifies all active observers of a change of timepoint. */
    private fun notifyObservers(timepoint: Int) {
        observers.forEach { it.onTimePointChanged(timepoint) }
    }

    /** Attaches a column of [Gui3DElement]s to the left VR controller and adds the column to [leftMenuList]. */
    protected fun createWristMenuColumn(
        vararg elements: Gui3DElement,
        debug: Boolean = false,
        name: String = "Menu"
    ): Column {
        val column = Column(*elements, centerVertically = true, centerHorizontally = true)
        column.ifSpatial {
            scale = Vector3f(0.05f)
            position = Vector3f(0.05f, 0.05f, column.height / 20f + 0.1f)
            rotation = Quaternionf().rotationXYZ(-1.57f, 1.57f, 0f)
        }
        leftVRController?.model?.let {
            sciview.addNode(column, parent = it, activePublish = false)
            if (debug) {
                column.children.forEach { child ->
                    val bb = BoundingGrid()
                    bb.node = child
                    bb.gridColor = Vector3f(0.5f, 1f, 0.4f)
                    sciview.addNode(bb, parent = it)
                }
            }
        }
        column.name = name
        column.pack()
        leftMenuList.add(column)
        return column
    }

    var controllerTrackingActive = false

    /** Intermediate storage for a single track created with the controllers.
     * Once tracking is finished, this track is sent to Mastodon. */
    var controllerTrackList = mutableListOf<Pair<Vector3f, SpineGraphVertex>>()
    var startWithExistingSpot: Spot? = null

    /** This lambda is called every time the user performs a click with controller-based tracking. */
    val trackCellsWithController = ClickBehaviour { _, _ ->
        if (!controllerTrackingActive) {
            controllerTrackingActive = true
            cursor.setTrackingColor()
            // we dont want animation, because we track step by step
            playing = false
            // Assume the user didn't click on an existing spot to start the track.
            startWithExistingSpot = null
        }
        // play the volume backwards, step by step, so cell split events can simply be turned into a merge event
        if (volume.currentTimepoint > 0) {
            val p = cursor.getPosition()
            // did the user click on an existing cell and wants to merge the track into it?
            val (selected, isValidSelection) =
                spotSelectCallback?.invoke(p, volume.currentTimepoint, cursor.radius, false) ?: (null to false)
            // If this is the first spot we track, and its a valid existing spot, mark it as such
            if (isValidSelection && controllerTrackList.size == 0) {
                startWithExistingSpot = selected
                logger.info("Set startWithExistingPost to $startWithExistingSpot")
            }
            logger.debug("Tracked a new spot at position $p")
            logger.debug("Do we want to merge? $isValidSelection. Selected spot is $selected")
            // Create a placeholder link during tracking for immediate feedback
            singleLinkTrackedCallback?.invoke(p, volume.currentTimepoint, cursor.radius, enableTrackingPreview)

//            controllerTrackList.add(
//                p to SpineGraphVertex(
//                    volume.currentTimepoint,
//                    p,
//                    volume.spatial().world.transform((Vector3f(p)).xyzw()).xyz(),
//                    controllerTrackList.size,
//                    0f // This is ugly, but we don't care about the sampled value of the volume here
//                )
//            )
            volume.goToTimepoint(volume.currentTimepoint - 1)
            // If the user clicked a cell and its *not* the first in the track, we assume it is a merge event and end the tracking
            if (isValidSelection && controllerTrackList.size > 1) {
                endControllerTracking(selected)
            }
            // This will also redraw all geometry using Mastodon as source
            notifyObservers(volume.currentTimepoint)
        } else {
            sciview.camera?.showMessage("Reached the first time point!", centered = true, distance = 2f, size = 0.2f)
            // Let's head back to the last timepoint for starting a new track fast-like
            volume.goToLastTimepoint()
            endControllerTracking()
        }
    }

    /** Stops the current controller tracking process and sends the created track to Mastodon. */
    private fun endControllerTracking(mergeSpot: Spot? = null) {
        if (controllerTrackingActive) {
            logger.info("Ending controller tracking now and sending ${controllerTrackList.size} spots to Mastodon to chew on.")
            controllerTrackingActive = false
            trackCreationCallback?.invoke(null, true, startWithExistingSpot, mergeSpot)
            controllerTrackList.clear()
            cursor.resetColor()
        }
    }

    fun setupElephantMenu() {
        val unpressedColor = Vector3f(0.81f, 0.81f, 1f)
        val touchingColor = Vector3f(0.7f, 0.65f, 1f)
        val pressedColor = Vector3f(0.54f, 0.44f, 0.96f)
        val stageSpotsButton = Button(
            "Stage all",
            command = { updateElephantActions(ElephantMode.StageSpots) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, touchingColor = touchingColor, pressedColor = pressedColor)
        val trainAllButton = Button(
            "Train All TPs",
            command = { updateElephantActions(ElephantMode.TrainAll) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, touchingColor = touchingColor, pressedColor = pressedColor)
        val predictAllButton = Button(
            "Predict All",
            command = { updateElephantActions(ElephantMode.PredictAll) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, touchingColor = touchingColor, pressedColor = pressedColor)
        val predictTPButton = Button(
            "Predict TP",
            command = { updateElephantActions(ElephantMode.PredictTP) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, touchingColor = touchingColor, pressedColor = pressedColor)
        val linkingButton = Button(
            "NN linking",
            command = { updateElephantActions(ElephantMode.NNLinking) }, byTouch = true, depressDelay = 500,
            color = unpressedColor, touchingColor = touchingColor, pressedColor = pressedColor)

        leftElephantColumn =
            createWristMenuColumn(stageSpotsButton, name = "Stage Menu")
        leftElephantColumn?.visible = false
        leftColumnPredict = createWristMenuColumn(trainAllButton, predictTPButton, predictAllButton, name = "Train/Predict Menu")
        leftColumnPredict?.visible = false
        leftColumnLink = createWristMenuColumn(linkingButton, name = "Linking Menu")
        leftColumnLink?.visible = false
    }

    var lastButtonTime = System.currentTimeMillis()

    /** Ensure that only a single Elephant action is triggered at a time */
    private fun updateElephantActions(mode: ElephantMode) {
        val buttonTime = System.currentTimeMillis()

        if ((buttonTime - lastButtonTime) > 1000) {

            thread {
                when (mode) {
                    ElephantMode.StageSpots -> stageSpotsCallback?.invoke()
                    ElephantMode.TrainAll -> trainSpotsCallback?.invoke()
                    ElephantMode.PredictTP -> predictSpotsCallback?.invoke(false)
                    ElephantMode.PredictAll -> predictSpotsCallback?.invoke(true)
                    ElephantMode.NNLinking -> neighborLinkingCallback?.invoke()
                }

                logger.info("We locked the buttons for ${(buttonTime-lastButtonTime)} ms ")
                lastButtonTime = buttonTime
            }

        } else {
            sciview.camera?.showMessage("Have some patience!", duration = 1500, distance = 2f, size = 0.2f, centered = true)
        }

    }

    fun setupGeneralMenu() {

        val cam = sciview.camera ?: throw IllegalStateException("Could not find camera")

        val color = Vector3f(0.8f)
        val pressedColor = Vector3f(0.95f, 0.35f, 0.25f)
        val touchingColor = Vector3f(0.7f, 0.55f, 0.55f)

        val undoButton = Button(
            "Undo",
            command = { mastodonUndoCallback?.invoke() }, byTouch = true, depressDelay = 250,
            color = color, pressedColor = pressedColor, touchingColor = touchingColor
        )
        val toggleTrackingPreviewBtn = ToggleButton(
            "Preview Off", "Preview On", command = {
                enableTrackingPreview = !enableTrackingPreview
                toggleTrackingPreviewCallback?.invoke(enableTrackingPreview)
            }, byTouch = true,
            color = color,
            touchingColor = Vector3f(0.67f, 0.9f, 0.63f),
            pressedColor = Vector3f(0.35f, 0.95f, 0.25f),
            default = true
        )
        val togglePlaybackDirBtn = ToggleButton(
            textFalse = "BW", textTrue = "FW", command = {
                direction = if (direction == PlaybackDirection.Forward) {
                    PlaybackDirection.Backward
                } else {
                    PlaybackDirection.Forward
                }
            }, byTouch = true,
            color = Vector3f(0.52f, 0.87f, 0.86f),
            touchingColor = color,
            pressedColor = Vector3f(0.84f, 0.87f, 0.52f)
        )
        val playSlowerBtn = Button(
            "<", command = {
                volumesPerSecond = maxOf(volumesPerSecond - 1f, 1f)
                cam.showMessage(
                    "Speed: ${"%.0f".format(volumesPerSecond)} vol/s",
                    distance = 1.2f, size = 0.2f, centered = true
                )
            }, byTouch = true, depressDelay = 250,
            color = color, pressedColor = pressedColor, touchingColor = touchingColor
        )
        val playFasterBtn = Button(
            ">", command = {
                volumesPerSecond = minOf(volumesPerSecond + 1f, 20f)
                cam.showMessage(
                    "Speed: ${"%.0f".format(volumesPerSecond)} vol/s",
                    distance = 1.2f, size = 0.2f, centered = true
                )
            }, byTouch = true, depressDelay = 250,
            color = color, pressedColor = pressedColor, touchingColor = touchingColor
        )
        val goToLastBtn = Button(
            ">|", command = {
                playing = false
                volume.goToLastTimepoint()
                notifyObservers(volume.currentTimepoint)
                cam.showMessage("Jumped to timepoint ${volume.currentTimepoint}.",
                    distance = 1.2f, size = 0.2f, centered = true)
            }, byTouch = true, depressDelay = 250,
            color = color, pressedColor = pressedColor, touchingColor = touchingColor
        )
        val goToFirstBtn = Button(
            "|<", command = {
                playing = false
                volume.goToFirstTimepoint()
                notifyObservers(volume.currentTimepoint)
                cam.showMessage("Jumped to timepoint ${volume.currentTimepoint}.",
                    distance = 1.2f, size = 0.2f, centered = true)
            }, byTouch = true, depressDelay = 250,
            color = color, pressedColor = pressedColor, touchingColor = touchingColor
        )

        val timeControlRow = Row(goToFirstBtn, playSlowerBtn, togglePlaybackDirBtn, playFasterBtn, goToLastBtn)

        leftUndoMenu = createWristMenuColumn(undoButton, name = "Left Undo Menu")
        leftUndoMenu?.visible = false
        val previewMenu = createWristMenuColumn(toggleTrackingPreviewBtn, name = "Preview Menu")
        previewMenu.visible = false
        val timeMenu = createWristMenuColumn(timeControlRow, name = "Time Menu")
        timeMenu.visible = false
    }


    private fun cycleLeftMenus() {
        leftMenuList.forEach { it.visible = false }
        leftMenuIndex = (leftMenuIndex + 1) % leftMenuList.size
        logger.debug("Cycling to ${leftMenuList[leftMenuIndex].name}")
        leftMenuList[leftMenuIndex].visible = true
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

    /** Attach a spherical cursor to the right controller. */
    private fun attachCursorAndTPWidget(debug: Boolean = false) {
        // Only attach if not already attached
        if (sciview.findNodes { it.name == "VR Cursor" }.isNotEmpty()) {
            return
        }

        volumeTPWidget.text = volume.currentTimepoint.toString()
        volumeTPWidget.name = "Volume Timepoint Widget"
        volumeTPWidget.fontColor = Vector4f(0.4f, 0.45f, 1f, 1f)
        volumeTPWidget.spatial {
            scale = Vector3f(0.07f)
            position = Vector3f(-0.05f, -0.05f, 0.12f)
            rotation = Quaternionf().rotationXYZ(-1.57f, -1.57f, 0f)
        }

        rightVRController?.model?.let {
            cursor.attachCursor(sciview, it)
            sciview.addNode(volumeTPWidget, activePublish = false, parent = it)
        }
    }

    /** Object that represents the 3D cursor in form of a sphere. It needs to be attached to a VR controller via [attachCursor].
     * The current cursor position can be obtained with [getPosition]. The current radius is stored in [radius].
     * The tool can be scaled up and down with [scaleByFactor].
     * [resetColor], [setSelectColor] and [setTrackingColor] allow changing the cursor's color to reflect the currently active operation. */
    object CursorTool {
        private val logger by lazyLogger()
        var radius: Float = 0.007f
            private set
        val cursor = Sphere(radius)
        private val initPos = Vector3f(-0.01f, -0.05f, -0.03f)

        fun getPosition() = cursor.spatial().worldPosition()

        fun attachCursor(sciview: SciView, parent: Node, debug: Boolean = false) {
            cursor.name = "VR Cursor"
            cursor.material {
                diffuse = Vector3f(0.15f, 0.2f, 1f)
            }
            cursor.spatial().position = initPos
            sciview.addNode(cursor, parent = parent)

            if (debug) {
                val bb = BoundingGrid()
                bb.node = cursor
                bb.name = "Cursor BB"
                bb.lineWidth = 2f
                bb.gridColor = Vector3f(1f, 0.3f, 0.25f)
                sciview.addNode(bb, parent = parent)
            }
            logger.info("Attached cursor to controller.")
        }

        fun scaleByFactor(factor: Float) {
            var clampedFac = 1f
            // Only apply the factor if we are in the radius range 0.001f - 0.1f
            if ((factor < 1f && radius > 0.001f) || (factor > 1f && radius < 0.15f)) {
                clampedFac = factor
            }
            radius *= clampedFac
            cursor.spatial().scale = Vector3f(radius/0.007f)
            cursor.spatial().position = Vector3f(initPos) + Vector3f(initPos).normalize().times(radius - 0.007f)
        }

        fun resetColor() {
            cursor.material().diffuse = Vector3f(0.15f, 0.2f, 1f)
        }

        fun setSelectColor() {
            cursor.material().diffuse = Vector3f(1f, 0.25f, 0.25f)
        }

        fun setTrackingColor() {
            cursor.material().diffuse = Vector3f(0.65f, 1f, 0.22f)
        }

    }

    open fun inputSetup()
    {
        val cam = sciview.camera ?: throw IllegalStateException("Could not find camera")

        sciview.sceneryInputHandler?.let { handler ->
            listOf(
                "move_forward_fast",
                "move_back_fast",
                "move_left_fast",
                "move_right_fast").forEach { name ->
                handler.getBehaviour(name)?.let { behaviour ->
                    mapper.setKeyBindAndBehavior(hmd, name, behaviour)
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

        class ScaleCursorOrSpotsBehavior(val factor: Float): DragBehaviour {
            var isSelected = false
            override fun init(p0: Int, p1: Int) {
                // determine whether we selected spots or not
                isSelected = getSelectionCallback?.invoke()?.isNotEmpty() ?: false
            }

            override fun drag(p0: Int, p1: Int) {
                if (isSelected) {
                    scaleSpotsCallback?.invoke(factor, false)
                } else {
                    // Make cursor movement a little stronger than
                    cursor.scaleByFactor(factor * factor)
                }
            }

            override fun end(p0: Int, p1: Int) {
                scaleSpotsCallback?.invoke(factor, true)
            }
        }

        val scaleCursorOrSpotsUp = AnalogInputWrapper(ScaleCursorOrSpotsBehavior(1.02f), sciview.currentScene)

        val scaleCursorOrSpotsDown = AnalogInputWrapper(ScaleCursorOrSpotsBehavior(0.98f), sciview.currentScene)

        val faster = ClickBehaviour { _, _ ->
            volumesPerSecond = maxOf(minOf(volumesPerSecond+0.2f, 20f), 1f)
            cam.showMessage("Speed: ${"%.1f".format(volumesPerSecond)} vol/s",distance = 1.2f, size = 0.2f, centered = true)
        }

        val slower = ClickBehaviour { _, _ ->
            volumesPerSecond = maxOf(minOf(volumesPerSecond-0.2f, 20f), 1f)
            cam.showMessage("Speed: ${"%.1f".format(volumesPerSecond)} vol/s",distance = 2f, size = 0.2f, centered = true)
        }

        val playPause = ClickBehaviour { _, _ ->
            playing = !playing
            if(playing) {
                cam.showMessage("Playing",distance = 2f, size = 0.2f, centered = true)
            } else {
                cam.showMessage("Paused",distance = 2f, size = 0.2f, centered = true)
            }
        }

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

        mapper.setKeyBindAndBehavior(hmd, "stepFwd", nextTimepoint)
        mapper.setKeyBindAndBehavior(hmd, "stepBwd", prevTimepoint)
//        mapper.setKeyBindAndBehavior(hmd, "faster", faster)
//        mapper.setKeyBindAndBehavior(hmd, "slower", slower)
        mapper.setKeyBindAndBehavior(hmd, "play_pause", playPause)
        mapper.setKeyBindAndBehavior(hmd, "radiusIncrease", scaleCursorOrSpotsUp)
        mapper.setKeyBindAndBehavior(hmd, "radiusDecrease", scaleCursorOrSpotsDown)

        /** Local class that handles double assignment of the left A key which is used to cycle menus as well as
         * reset the rotation when pressed while the [VR2HandNodeTransform] is active. */
        class CycleMenuAndLockAxisBehavior(val button: OpenVRHMD.OpenVRButton, val role: TrackerRole)
            : DragBehaviour {
            fun registerConfig() {
                logger.debug("Setting up keybinds for CycleMenuAndLockAxisBehavior")
                resetRotationBtnManager.registerButtonConfig(button, role)
            }
            override fun init(x: Int, y: Int) {
                resetRotationBtnManager.pressButton(button, role)
                if (!resetRotationBtnManager.isTwoHandedActive()) {
                    cycleLeftMenus()
                }
            }
            override fun drag(x: Int, y: Int) {}
            override fun end(x: Int, y: Int) {
                resetRotationBtnManager.releaseButton(button, role)
            }
        }

        val leftAButtonBehavior = CycleMenuAndLockAxisBehavior(OpenVRHMD.OpenVRButton.A, TrackerRole.LeftHand)
        leftAButtonBehavior.let {
            it.registerConfig()
            mapper.setKeyBindAndBehavior(hmd, "cycleMenu", it)
        }

        mapper.setKeyBindAndBehavior(hmd, "controllerTracking", trackCellsWithController)

        /** Several behaviors mapped per default to the right menu button. If controller tracking is active,
         * end the tracking. If not, clicking will either create or delete a spot, depending on whether the user
         * previously selected a spot. Holding the button for more than 0.5s deletes the whole connected branch. */
        class AddDeleteResetBehavior : DragBehaviour {
            var start = System.currentTimeMillis()
            var wasExecuted = false
            override fun init(x: Int, y: Int) {
                start = System.currentTimeMillis()
                wasExecuted = false
            }
            override fun drag(x: Int, y: Int) {
                if (System.currentTimeMillis() - start > 500 && !wasExecuted) {
                    val p = cursor.getPosition()
                    spotCreateDeleteCallback?.invoke(volume.currentTimepoint, p, cursor.radius, true)
                    wasExecuted = true
                }
            }
            override fun end(x: Int, y: Int) {
                if (controllerTrackingActive) {
                    endControllerTracking()
                } else {
                    val p = cursor.getPosition()
                    logger.debug("Got cursor position: $p")
                    if (!wasExecuted) {
                        spotCreateDeleteCallback?.invoke(volume.currentTimepoint, p, cursor.radius, false)
                    }
                }
            }
        }

        mapper.setKeyBindAndBehavior(hmd, "addDeleteReset", AddDeleteResetBehavior())

        class DragSelectBehavior: DragBehaviour {
            var time = System.currentTimeMillis()
            override fun init(x: Int, y: Int) {
                time = System.currentTimeMillis()
                val p = cursor.getPosition()
                cursor.setSelectColor()
                spotSelectCallback?.invoke(p, volume.currentTimepoint, cursor.radius, false)
            }
            override fun drag(x: Int, y: Int) {
                // Only perform the selection method ten times a second
                if (System.currentTimeMillis() - time > 100) {
                    val p = cursor.getPosition()
                    spotSelectCallback?.invoke(p, volume.currentTimepoint, cursor.radius, true)
                    time = System.currentTimeMillis()
                }
            }
            override fun end(x: Int, y: Int) {
                cursor.resetColor()
            }
        }

        mapper.setKeyBindAndBehavior(hmd, "select", DragSelectBehavior())

        // this behavior is needed for touching the menu buttons
        VRTouch.createAndSet(sciview.currentScene, hmd, listOf(TrackerRole.RightHand), false, customTip = cursor.cursor)

        VRGrabTheWorld.createAndSet(
            sciview.currentScene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Side),
            listOf(TrackerRole.LeftHand),
            grabButtonManager,
            1.5f
        )

        VR2HandNodeTransform.createAndSet(
            hmd,
            OpenVRHMD.OpenVRButton.Side,
            sciview.currentScene,
            lockYaxis = false,
            target = volume,
            onEndCallback = rebuildGeometryCallback,
            resetRotationBtnManager = resetRotationBtnManager,
            resetRotationButton = MultiButtonManager.ButtonConfig(leftAButtonBehavior.button, leftAButtonBehavior.role)
        )

        // drag behavior can stay enabled regardless of current tool mode
        MoveInstanceVR.createAndSet(
            sciview.currentScene, hmd, listOf(OpenVRHMD.OpenVRButton.Side), listOf(TrackerRole.RightHand),
            grabButtonManager,
            { cursor.getPosition() },
            spotMoveInitCallback,
            spotMoveDragCallback,
            spotMoveEndCallback,
        )

        hmd.allowRepeats += OpenVRHMD.OpenVRButton.Trigger to TrackerRole.LeftHand
        logger.info("Registered VR controller bindings.")

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

                    if(eyeTrackingActive && newTimepoint == 0) {
                        eyeTrackingActive = false
                        playing = false
                        referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f)}
                        logger.info("Deactivated eye tracking by reaching timepoint 0.")
                        sciview.camera!!.showMessage("Tracking deactivated.",distance = 2f, size = 0.2f, centered = true)
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
        hedgehogFileWriter.write("Timepoint;Origin;Direction;LocalEntry;LocalExit;LocalDirection;HeadPosition;HeadOrientation;Position;Confidence;Samples\n")

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
            trackCreationCallback?.invoke(track.points, false, null, null)
            rebuildGeometryCallback?.invoke()
        } else {
            logger.warn("Tried to send track data to Mastodon but couldn't find the callbacks!")
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
        cellTrackingActive = false
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