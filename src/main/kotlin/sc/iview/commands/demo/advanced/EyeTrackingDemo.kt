package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.ControllerDrag
import graphics.scenery.controls.eyetracking.PupilEyeTracker
import graphics.scenery.textures.Texture
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Volume
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.joml.*
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.awt.image.DataBufferByte
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.PI
import org.scijava.log.LogService
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import sc.iview.commands.demo.animation.ParticleDemo

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Utilize Eye Tracker for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class EyeTrackingDemo: Command{
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

    val pupilTracker = PupilEyeTracker(calibrationType = PupilEyeTracker.CalibrationType.WorldSpace, port = System.getProperty("PupilPort", "50020").toInt())
    lateinit var hmd: OpenVRHMD
    val referenceTarget = Icosphere(0.004f, 2)
    val calibrationTarget = Icosphere(0.02f, 2)
    val laser = Cylinder(0.005f, 0.2f, 10)


    lateinit var sessionId: String
    lateinit var sessionDirectory: Path

    val hedgehogs = Mesh()

    enum class HedgehogVisibility { Hidden, PerTimePoint, Visible }
    var hedgehogVisibility = HedgehogVisibility.Hidden

    lateinit var volume: Volume

    val confidenceThreshold = 0.60f

    enum class PlaybackDirection {
        Forward,
        Backward
    }

    @Volatile var tracking = false
    var playing = false
    var direction = PlaybackDirection.Forward
    var volumesPerSecond = 4
    var skipToNext = false
    var skipToPrevious = false
//	var currentVolume = 0

    var volumeScaleFactor = 1.0f

    override fun run() {

        sciview.toggleVRRendering()
        log.info("VR mode has been toggled")
        hmd = sciview.hub.getWorkingHMD() as? OpenVRHMD ?: throw IllegalStateException("Could not find headset")
        sessionId = "BionicTracking-generated-${SystemHelpers.formatDateTime()}"
        sessionDirectory = Files.createDirectory(Paths.get(System.getProperty("user.home"), "Desktop", sessionId))

        referenceTarget.visible = false
        referenceTarget.ifMaterial{
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(0.8f, 0.8f, 0.8f)
        }
        sciview.camera!!.addChild(referenceTarget)

        calibrationTarget.visible = false
        calibrationTarget.material {
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        sciview.camera!!.addChild(calibrationTarget)

        laser.visible = false
        laser.ifMaterial{diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        sciview.addChild(laser)

        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }

        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        sciview.addChild(shell)

//        volume = sciview.find("volume") as Volume
        val volnodes = sciview.findNodes { node -> Volume::class.java.isAssignableFrom(node.javaClass) }

        val v = (volnodes.firstOrNull() as? Volume)
        if(v == null) {
            log.warn("No volume found, bailing")
            return
        } else {
            log.info("found ${volnodes.size} volume nodes. Using the first one: ${volnodes.first()}")
            volume = v
        }
        volume.visible = false

        val bb = BoundingGrid()
        bb.node = volume
        bb.visible = false

        sciview.addChild(hedgehogs)

        val eyeFrames = Mesh("eyeFrames")
        val left = Box(Vector3f(1.0f, 1.0f, 0.001f))
        val right = Box(Vector3f(1.0f, 1.0f, 0.001f))
        left.spatial().position = Vector3f(-1.0f, 1.5f, 0.0f)
        left.spatial().rotation = left.rotation.rotationZ(PI.toFloat())
        right.spatial().position = Vector3f(1.0f, 1.5f, 0.0f)
        eyeFrames.addChild(left)
        eyeFrames.addChild(right)

        sciview.addChild(eyeFrames)

        val pupilFrameLimit = 20
        var lastFrame = System.nanoTime()

        pupilTracker.subscribeFrames { eye, texture ->
            if(System.nanoTime() - lastFrame < pupilFrameLimit*10e5) {
                return@subscribeFrames
            }

            val node = if(eye == 1) {
                left
            } else {
                right
            }

            val stream = ByteArrayInputStream(texture)
            val image = ImageIO.read(stream)
            val data = (image.raster.dataBuffer as DataBufferByte).data

            node.ifMaterial {
                textures["diffuse"] = Texture(
                    Vector3i(image.width, image.height, 1),
                    3,
                    UnsignedByteType(),
                    BufferUtils.allocateByteAndPut(data)
            ) }

            lastFrame = System.nanoTime()
        }

        // TODO: Replace with cam.showMessage()
        val debugBoard = TextBoard()
        debugBoard.name = "debugBoard"
        debugBoard.scale = Vector3f(0.05f, 0.05f, 0.05f)
        debugBoard.position = Vector3f(0.0f, -0.3f, -0.9f)
        debugBoard.text = ""
        debugBoard.visible = false
        sciview.camera?.addChild(debugBoard)

        val lights = Light.createLightTetrahedron<PointLight>(Vector3f(0.0f, 0.0f, 0.0f), spread = 5.0f, radius = 15.0f, intensity = 5.0f)
        lights.forEach { sciview.addChild(it) }

        thread {
            log.info("Adding onDeviceConnect handlers")
            hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
                log.info("onDeviceConnect called, cam=${sciview.camera}")
                if(device.type == TrackedDeviceType.Controller) {
                    log.info("Got device ${device.name} at $timestamp")
                    device.model?.let { hmd.attachToNode(device, it, sciview.camera) }
                }
            }
        }
        thread{
            log.info("started thread for inputSetup")
            inputSetup()
        }
        thread {
            while(!sciview.isInitialized) { Thread.sleep(200) }

            while(sciview.running) {
                if(playing || skipToNext || skipToPrevious) {
                    val oldTimepoint = volume.viewerState.currentTimepoint
                    val newVolume = if(skipToNext || playing) {
                        skipToNext = false
                        if(direction == PlaybackDirection.Forward) {
                            volume.nextTimepoint()
                        } else {
                            volume.previousTimepoint()
                        }
                    } else {
                        skipToPrevious = false
                        if(direction == PlaybackDirection.Forward) {
                            volume.previousTimepoint()
                        } else {
                            volume.nextTimepoint()
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
        }

    }

    fun addHedgehog() {
        log.info("added hedgehog")
        val hedgehog = Cylinder(0.005f, 1.0f, 16)
        hedgehog.visible = false
        hedgehog.setMaterial(ShaderMaterial.fromClass(ParticleDemo::class.java))
        var hedgehogInstanced = InstancedNode(hedgehog)
        hedgehogInstanced.instancedProperties["ModelMatrix"] = { hedgehog.spatial().world}
        hedgehogInstanced.instancedProperties["Metadata"] = { Vector4f(0.0f, 0.0f, 0.0f, 0.0f) }
        hedgehogs.addChild(hedgehogInstanced)
    }


    fun inputSetup()
    {
        val cam = sciview.camera ?: throw IllegalStateException("Could not find camera")

        sciview.sceneryInputHandler?.let { handler ->
            hashMapOf(
                    "move_forward_fast" to "K",
                    "move_back_fast" to "J",
                    "move_left_fast" to "H",
                    "move_right_fast" to "L").forEach { (name, key) ->
                handler.getBehaviour(name)?.let { b ->
                    hmd.addBehaviour(name, b)
                    hmd.addKeyBinding(name, key)
                }
            }
        }

        val toggleHedgehog = ClickBehaviour { _, _ ->
            val current = HedgehogVisibility.values().indexOf(hedgehogVisibility)
            hedgehogVisibility = HedgehogVisibility.values().get((current + 1) % 3)

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
                volumeScaleFactor = minOf(volumeScaleFactor * 1.2f, 3.0f)
                volume.scale =Vector3f(1.0f) .mul(volumeScaleFactor)
            }
        }

        val slowerOrScale = ClickBehaviour { _, _ ->
            if(playing) {
                volumesPerSecond = maxOf(minOf(volumesPerSecond-1, 20), 1)
                cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 2f, size = 0.2f, centered = true)
            } else {
                volumeScaleFactor = maxOf(volumeScaleFactor / 1.2f, 0.1f)
                volume.scale = Vector3f(1.0f) .mul(volumeScaleFactor)
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

        hmd.addBehaviour("playback_direction", ClickBehaviour { _, _ ->
            direction = if(direction == PlaybackDirection.Forward) {
                PlaybackDirection.Backward
            } else {
                PlaybackDirection.Forward
            }
            cam.showMessage("Playing: ${direction}", distance = 2f, centered = true)
        })

        val cellDivision = ClickBehaviour { _, _ ->
            cam.showMessage("Adding cell division", distance = 2f, duration = 1000)
            dumpHedgehog()
            addHedgehog()
        }

        hmd.addBehaviour("skip_to_next", nextTimepoint)
        hmd.addBehaviour("skip_to_prev", prevTimepoint)
        hmd.addBehaviour("faster_or_scale", fasterOrScale)
        hmd.addBehaviour("slower_or_scale", slowerOrScale)
        hmd.addBehaviour("play_pause", playPause)
        hmd.addBehaviour("toggle_hedgehog", toggleHedgehog)
        hmd.addBehaviour("trigger_move", move)
        hmd.addBehaviour("delete_hedgehog", deleteLastHedgehog)
        hmd.addBehaviour("cell_division", cellDivision)

        hmd.addKeyBinding("toggle_hedgehog", "X")
        hmd.addKeyBinding("delete_hedgehog", "Y")
        hmd.addKeyBinding("skip_to_next", "D")
        hmd.addKeyBinding("skip_to_prev", "A")
        hmd.addKeyBinding("faster_or_scale", "W")
        hmd.addKeyBinding("slower_or_scale", "S")
        hmd.addKeyBinding("play_pause", "M")
        hmd.addKeyBinding("playback_direction", "N")
        hmd.addKeyBinding("cell_division", "T")

        hmd.allowRepeats += OpenVRHMD.OpenVRButton.Trigger to TrackerRole.LeftHand
        log.info("calibration should start now")
        setupCalibration()

    }


    private fun setupCalibration(keybindingCalibration: String = "N", keybindingTracking: String = "U") {
        val startCalibration = ClickBehaviour { _, _ ->
            thread {
                val cam = sciview.camera as? DetachedHeadCamera ?: return@thread
                pupilTracker.gazeConfidenceThreshold = confidenceThreshold
                if (!pupilTracker.isCalibrated) {
                    log.info("pupil is currently uncalibrated")
                    pupilTracker.onCalibrationInProgress = {
                        cam.showMessage("Crunching equations ...",distance = 2f, size = 0.2f, messageColor = Vector4f(1.0f, 0.8f, 0.0f, 1.0f), duration = 15000, centered = true)
                    }

                    pupilTracker.onCalibrationFailed = {
                        cam.showMessage("Calibration failed.",distance = 2f, size = 0.2f, messageColor = Vector4f(1.0f, 0.0f, 0.0f, 1.0f), centered = true)
                    }

                    pupilTracker.onCalibrationSuccess = {
                        cam.showMessage("Calibration succeeded!", distance = 2f, size = 0.2f, messageColor = Vector4f(0.0f, 1.0f, 0.0f, 1.0f), centered = true)
//						cam.children.find { it.name == "debugBoard" }?.visible = true

                        for (i in 0 until 20) {
                            referenceTarget.ifMaterial{diffuse = Vector3f(0.0f, 1.0f, 0.0f)}
                            Thread.sleep(100)
                            referenceTarget.ifMaterial { diffuse = Vector3f(0.8f, 0.8f, 0.8f)}
                            Thread.sleep(30)
                        }

                        hmd.removeBehaviour("start_calibration")
                        hmd.removeKeyBinding("start_calibration")

                        val toggleTracking = ClickBehaviour { _, _ ->
                            if (tracking) {
                                log.info("deactivating tracking...")
                                referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f) }
                                cam.showMessage("Tracking deactivated.",distance = 2f, size = 0.2f, centered = true)
                                dumpHedgehog()
                            } else {
                                log.info("activating tracking...")
                                addHedgehog()
                                referenceTarget.ifMaterial { diffuse = Vector3f(1.0f, 0.0f, 0.0f) }
                                cam.showMessage("Tracking active.",distance = 2f, size = 0.2f, centered = true)
                            }
                            tracking = !tracking
                        }
                        hmd.addBehaviour("toggle_tracking", toggleTracking)
                        hmd.addKeyBinding("toggle_tracking", keybindingTracking)

                        volume.visible = true
                        volume.runRecursive { it.visible = true }
                        playing = true
                    }

                    pupilTracker.unsubscribeFrames()
                    sciview.deleteNode(sciview.find("eyeFrames"))

                    log.info("Starting eye tracker calibration")
                    cam.showMessage("Follow the white rabbit.", distance = 2f, size = 0.2f,duration = 1500, centered = true)
                    pupilTracker.calibrate(cam, hmd,
                            generateReferenceData = true,
                            calibrationTarget = calibrationTarget)

                    pupilTracker.onGazeReceived = when (pupilTracker.calibrationType) {
                        //NEW
                        PupilEyeTracker.CalibrationType.WorldSpace -> { gaze ->
                            if (gaze.confidence > confidenceThreshold) {
                                val p = gaze.gazePoint()
                                referenceTarget.visible = true
                                // Pupil has mm units, so we divide by 1000 here to get to scenery units
                                referenceTarget.position = p
                                (cam.children.find { it.name == "debugBoard" } as? TextBoard)?.text = "${String.format("%.2f", p.x())}, ${String.format("%.2f", p.y())}, ${String.format("%.2f", p.z())}"

                                val headCenter = cam.viewportToWorld(Vector2f(0.0f, 0.0f))
                                val pointWorld = Matrix4f(cam.world).transform(p.xyzw()).xyz()
                                val direction = (pointWorld - headCenter).normalize()

                                if (tracking) {
//                                    log.info("Starting spine from $headCenter to $pointWorld")
                                    addSpine(headCenter, direction, volume, gaze.confidence, volume.viewerState.currentTimepoint)
                                }
                            }
                        }

//                        else -> {gaze-> }
                    }

                    log.info("Calibration routine done.")
                }

                // bind calibration start to menu key on controller

            }
        }
        hmd.addBehaviour("start_calibration", startCalibration)
        hmd.addKeyBinding("start_calibration", keybindingCalibration)
    }
    fun addSpine(center: Vector3f, direction: Vector3f, volume: Volume, confidence: Float, timepoint: Int) {
        val cam = sciview.camera as? DetachedHeadCamera ?: return
        val sphere = volume.boundingBox?.getBoundingSphere() ?: return

        val sphereDirection = sphere.origin.minus(center)
        val sphereDist = Math.sqrt(sphereDirection.x * sphereDirection.x + sphereDirection.y * sphereDirection.y + sphereDirection.z * sphereDirection.z) - sphere.radius

        val p1 = center
        val temp = direction.mul(sphereDist + 2.0f * sphere.radius)
        val p2 = Vector3f(center).add(temp)

        val spine = (hedgehogs.children.last() as InstancedNode).addInstance()
        spine.spatial().orientBetweenPoints(p1, p2, true, true)
        spine.visible = true

        val intersection = volume.intersectAABB(p1, (p2 - p1).normalize())
//		System.out.println(intersection);
        if(intersection is MaybeIntersects.Intersection) {
            // get local entry and exit coordinates, and convert to UV coords
            val localEntry = (intersection.relativeEntry) //.add(Vector3f(1.0f)) ) .mul (1.0f / 2.0f)
            val localExit = (intersection.relativeExit) //.add (Vector3f(1.0f)) ).mul  (1.0f / 2.0f)
            // TODO: Allow for sampling a given time point of a volume
            val (samples, localDirection) = volume.sampleRay(localEntry, localExit) ?: (null to null)

            if (samples != null && localDirection != null) {
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
                        cam.position,
                        confidence,
                        samples.map { it ?: 0.0f }
                )
                val count = samples.filterNotNull().count { it > 0.2f }

                spine.metadata["spine"] = metadata
                spine.instancedProperties["ModelMatrix"] = { spine.spatial().world }
                // TODO: Show confidence as color for the spine
                spine.instancedProperties["Metadata"] = { Vector4f(confidence, timepoint.toFloat()/volume.timepointCount, count.toFloat(), 0.0f) }
            }
        }
    }

    val hedgehogIds = AtomicInteger(0)
    /**
     * Dumps a given hedgehog including created tracks to a file.
     * If [hedgehog] is null, the last created hedgehog will be used, otherwise the given one.
     * If [hedgehog] is not null, the cell track will not be added to the scene.
     */
    fun dumpHedgehog() {
        log.info("dumping hedgehog...")
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
            val h = HedgehogAnalysis(spines, Matrix4f(volume.world))
            h.run()
        }

        if(track == null) {
//            logger.warn("No track returned")
            sciview.camera?.showMessage("No track returned", distance = 1.2f, size = 0.2f,messageColor = Vector4f(1.0f, 0.0f, 0.0f,1.0f))
            return
        }

        lastHedgehog.metadata["HedgehogAnalysis"] = track
        lastHedgehog.metadata["Spines"] = spines

//        logger.info("---\nTrack: ${track.points.joinToString("\n")}\n---")

        val cylinder = Cylinder(0.01f, 1.0f, 6)
        cylinder.setMaterial(ShaderMaterial.fromFiles("DeferredInstancedColor.vert", "DeferredInstancedColor.frag")) {
            diffuse = Vector3f(1f)
            ambient = Vector3f(1f)
            roughness = 1f
        }

        cylinder.name = "Track-$hedgehogId"
        val mainTrack = InstancedNode(cylinder)
        mainTrack.instancedProperties["Color"] = { Vector4f(1f) }

        val parentId = 0
        val volumeDimensions = volume.getDimensions()

        trackFileWriter.newLine()
        trackFileWriter.newLine()
        trackFileWriter.write("# START OF TRACK $hedgehogId, child of $parentId\n")
        track.points.windowed(2, 1).forEach { pair ->
            if(mainTrack != null) {
                val element = mainTrack.addInstance()
                element.addAttribute(Material::class.java, cylinder.material())
                element.spatial().orientBetweenPoints(Vector3f(pair[0].first), Vector3f(pair[1].first), rescale = true, reposition = true)
                element.parent = volume
//                mainTrack.instances.add(element)
            }
            val p = Vector3f(pair[0].first).mul(Vector3f(volumeDimensions))//direct product
            val tp = pair[0].second.timepoint
            trackFileWriter.write("$tp\t${p.x()}\t${p.y()}\t${p.z()}\t${hedgehogId}\t$parentId\t0\t0\n")
        }

        mainTrack.let { sciview.addNode(it, parent = volume) }

        trackFileWriter.close()
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(EyeTrackingDemo::class.java, true, argmap)
        }
    }
}