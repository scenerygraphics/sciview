package sc.iview.commands.demo.advanced

import com.intellij.ui.tabs.impl.ShapeTransform.Right
import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.ControllerDrag
import graphics.scenery.primitives.Cylinder
import graphics.scenery.ui.Button
import graphics.scenery.ui.Column
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.RAIVolume
import graphics.scenery.volumes.Volume
import org.joml.*
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView
import sc.iview.commands.demo.advanced.HedgehogAnalysis.SpineGraphVertex
import sc.iview.commands.file.Open
import sc.iview.controls.behaviours.MoveInstanceVR
import sc.iview.controls.behaviours.MultiVRButtonStateManager
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

    var trackCreationCallback: ((List<Pair<Vector3f, SpineGraphVertex>>) -> Unit)? = null
    var finalTrackCallback: (() -> Unit)? = null
    var spotCreationCallback: ((Int, Vector3f) -> Unit)? = null
    var spotSelectionCallback: ((Vector3f, Int) -> Unit)? = null
    var spotMoveInitCallback: ((Vector3f) -> Unit)? = null
    var spotMoveDragCallback: ((Vector3f) -> Unit)? = null
    var spotMoveEndCallback: ((Vector3f) -> Unit)? = null
    var rebuildGeometryCallback: (() -> Unit)? = null

    enum class HedgehogVisibility { Hidden, PerTimePoint, Visible }

    enum class RightTriggerModes {Create, Edit, Delete, Track}

    private val tools = mapOf(RightTriggerModes.Create to "addSpotWithController",
        RightTriggerModes.Edit to "selectSpotWithController",
        RightTriggerModes.Delete to "deleteSpotWithController",
        RightTriggerModes.Track to "trackCellWithController",)

    private val currentTool: RightTriggerModes = RightTriggerModes.Edit

    var hedgehogVisibility = HedgehogVisibility.Hidden

    var leftVRController: TrackedDevice? = null
    var rightVRController: TrackedDevice? = null
    lateinit var tip: Sphere

    val buttonManager = MultiVRButtonStateManager()

    enum class PlaybackDirection {
        Forward,
        Backward
    }

    private val observers = mutableListOf<TimepointObserver>()

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
                    } else if (device.role == TrackerRole.LeftHand) {
                        device.name = "leftHand"
                        setupLeftWristMenu()
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

    private fun setupLeftWristMenu() {
        val createButton = Button(
                "Create Spot", command = this::selectCreateButton,
                color = Vector3f(0.5f, 0.95f, 0.45f),
                pressedColor = Vector3f(0.2f, 1f, 0.15f),
            )
        val editButton = Button(
            "Edit Spot", command = this::selectEditButton,
            color = Vector3f(0.4f, 0.45f, 0.95f),
            pressedColor = Vector3f(0.15f, 0.2f, 1f)
        )
        val deleteButton = Button(
            "Delete Spot", command = this::selectDeleteButton,
            color = Vector3f(0.95f, 0.5f, 0.45f),
            pressedColor = Vector3f(1f, 0.2f, 0.15f)
        )
        val trackButton = Button(
            "Controller Tracking ", command = this::selectTrackButton,
            color = Vector3f(0.9f, 0.9f, 0.5f),
            pressedColor = Vector3f(1f, 1f, 0.2f)
        )
        val leftWristColumn = Column(createButton, editButton, deleteButton, trackButton)
        leftWristColumn.ifSpatial {
            position = Vector3f(0.0f, 0.0f, -.2f)
            rotation = Quaternionf().rotationY(90f)
        }

        leftVRController?.model?.let {
            sciview.addNode(leftWristColumn, parent = it)
        }
    }

    val addSpotWithController = ClickBehaviour { _, _ ->
        val p = getTipPosition()
        logger.debug("Got tip position: $p")
        spotCreationCallback?.invoke(volume.currentTimepoint, p)
    }

    val selectSpotWithController = ClickBehaviour { _, _ ->
        val p = getTipPosition()
        logger.debug("Got tip position: $p")
        spotSelectionCallback?.invoke(p, volume.currentTimepoint)
    }

    private fun selectCreateButton() {
        unregisterCurrentTool()
        hmd.addBehaviour(tools[RightTriggerModes.Create]!!, addSpotWithController)
        hmd.addKeyBinding(tools[RightTriggerModes.Create]!!, TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Trigger)
    }

    private fun selectEditButton() {
        unregisterCurrentTool()
        hmd.addBehaviour(tools[RightTriggerModes.Edit]!!, selectSpotWithController)
        hmd.addKeyBinding(tools[RightTriggerModes.Edit]!!, TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Trigger)
    }

    private fun selectDeleteButton() {
        unregisterCurrentTool()
    }

    private fun selectTrackButton() {
        unregisterCurrentTool()
    }

    private fun unregisterCurrentTool() {
        tools[currentTool]?.let {
            hmd.removeBehaviour(it)
            hmd.removeKeyBinding(it)
        }
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
        tip = Sphere(0.01f)
        tip.material {
            diffuse = Vector3f(0.8f, 0.9f, 1f)
        }
        tip.spatial().position = Vector3f(0.0f, -0f, -0.05f)
        rightVRController?.model?.let {
            sciview.addNode(tip, parent = it)
            logger.debug("Added tip to right controller")
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

        val fasterOrScale = ClickBehaviour { _, _ ->
            if(playing) {
                volumesPerSecond = maxOf(minOf(volumesPerSecond+1, 20), 1)
                cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 1.2f, size = 0.2f, centered = true)
            } else {
//                volumeScaleFactor = minOf(volumeScaleFactor * 1.05f, 100.0f)
//                volume.spatial().scale = Vector3f(1.0f) .mul(volumeScaleFactor)
            }
        }

        val slowerOrScale = ClickBehaviour { _, _ ->
            if(playing) {
                volumesPerSecond = maxOf(minOf(volumesPerSecond-1, 20), 1)
                cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 2f, size = 0.2f, centered = true)
            } else {
//                volumeScaleFactor = maxOf(volumeScaleFactor / 1.05f, 0.1f)
//                volume.spatial().scale = Vector3f(1.0f) .mul(volumeScaleFactor)
            }
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
        hmd.addBehaviour("faster_or_scale", fasterOrScale)
        hmd.addBehaviour("slower_or_scale", slowerOrScale)
        hmd.addBehaviour("play_pause", playPause)
        hmd.addKeyBinding("skip_to_next", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Right)
        hmd.addKeyBinding("skip_to_prev", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Left)
        hmd.addKeyBinding("faster_or_scale", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Up)
        hmd.addKeyBinding("slower_or_scale", TrackerRole.RightHand, OpenVRHMD.OpenVRButton.Down)
        hmd.addKeyBinding("play_pause", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Menu)

//        hmd.addBehaviour("toggle_hedgehog", toggleHedgehog)
//        hmd.addBehaviour("delete_hedgehog", deleteLastHedgehog)
//        hmd.addBehaviour("cell_division", cellDivision)
//        hmd.addKeyBinding("cell_division", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Trigger)

//        hmd.addBehaviour("trigger_move", move)
//        hmd.addKeyBinding("trigger_move", TrackerRole.LeftHand, OpenVRHMD.OpenVRButton.Side)



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
        return tip.spatial().worldPosition(tip.spatial().position)
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
        if (trackCreationCallback != null && finalTrackCallback != null) {
            trackCreationCallback?.invoke(track.points)
            finalTrackCallback?.invoke()
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