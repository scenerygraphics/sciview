package sc.iview.commands.demo.advanced

import bdv.util.BdvFunctions
import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.backends.ShaderType
import graphics.scenery.bionictracking.ConfirmableClickBehaviour
import graphics.scenery.bionictracking.HedgehogAnalysis
import graphics.scenery.bionictracking.SpineMetadata
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.behaviours.ControllerDrag
import graphics.scenery.controls.eyetracking.PupilEyeTracker
import graphics.scenery.controls.eyetracking.PupilEyeTrackerNew
import graphics.scenery.numerics.OpenSimplexNoise
import graphics.scenery.numerics.Random
import graphics.scenery.textures.Texture
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import net.imglib2.FinalInterval
import net.imglib2.Localizable
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.array.ArrayImgs
import net.imglib2.position.FunctionRandomAccessible
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.joml.*
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.widget.FileWidget
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.awt.image.DataBufferByte
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.PI
import net.imglib2.img.Img
import net.imglib2.view.Views
import org.lwjgl.openvr.OpenVR
import org.scijava.log.LogService
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Utilize VR Controller for Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class ControllerTrackingDemo: Command{
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

     lateinit var hmd: OpenVRHMD
    val referenceTarget = Icosphere(0.004f, 2)
    //val calibrationTarget = Icosphere(0.02f, 2)
    //val TestTarget = Icosphere(0.04f, 2)
    val laser = Cylinder(0.005f, 0.2f, 10)

    lateinit var TestTarget1:Icosphere
    lateinit var TestTarget2:Icosphere
    lateinit var TestTarget3:Icosphere
    lateinit var TestTarget4:Icosphere

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
//        center position: (-1.217E-2  2.478E+0  3.981E+0)pointWord: ( 1.650E-1  2.277E+0  3.012E+0)
//        sphere.origin: ( 1.500E+0 -3.000E-1 -3.050E-1)p2 position( 1.826E+0  3.938E-1 -6.069E+0)
        TestTarget1= Icosphere(0.08f, 2)
        TestTarget1.ifMaterial{ diffuse = Vector3f(0.5f, 0.3f, 0.8f)}
        TestTarget1.spatial().position =  Vector3f(-0.1059f, 2.394f, 4.086f)//Vector3f(1.858f,1.7f,2.432f)//
        TestTarget1.visible = true
        sciview.addChild(TestTarget1)

        TestTarget2= Icosphere(0.08f, 2)
        TestTarget2.ifMaterial{ diffuse = Vector3f(0.5f, 0.3f, 0.8f)}
        TestTarget2.spatial().position =  Vector3f(-0.0318f, 2.438f, 4.084f)//Vector3f(1.858f,1.7f,2.432f)//
        TestTarget2.visible = true
        sciview.addChild(TestTarget2)

//        TestTarget3= Icosphere(0.08f, 2)
//        TestTarget3.ifMaterial{ diffuse = Vector3f(0.5f, 0.3f, 0.8f)}
//        TestTarget3.spatial().position =  Vector3f(1.5f, -0.3f, -0.3050f)//Vector3f(1.858f,1.7f,2.432f)//
//        TestTarget3.visible = true
//        sciview.addChild(TestTarget3)
//
        TestTarget4= Icosphere(0.8f, 2)
        TestTarget4.ifMaterial{ diffuse = Vector3f(0.5f, 0.3f, 0.8f)}
        TestTarget4.spatial().position =  Vector3f(1.826f, 0.3938f, -6.069f)//( 1.826E+0  3.938E-1 -6.069E+0)
        TestTarget4.visible = true
        sciview.addChild(TestTarget4)


        sciview.toggleVRRendering()
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

//        calibrationTarget.visible = false
//        calibrationTarget.material {
//            roughness = 1.0f
//            metallic = 0.0f
//            diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
//        sciview.camera!!.addChild(calibrationTarget)

        laser.visible = false
        laser.ifMaterial{diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        sciview.addChild(laser)


        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }

        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        sciview.addChild(shell)

        volume = sciview.find("volume") as Volume
        //volume.visible = false

        val bb = BoundingGrid()
        bb.node = volume
        bb.visible = false

        sciview.addChild(hedgehogs)

        val pupilFrameLimit = 20
        var lastFrame = System.nanoTime()



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
                            hedgehogs.children.forEach { hedgehog->
                                val hedgehog = hedgehog as InstancedNode
                                hedgehog.instances.forEach {
                                    it.visible = (it.metadata["spine"] as SpineMetadata).timepoint == volume.viewerState.currentTimepoint
                                }
                            }
                        } else {
                            hedgehogs.children.forEach { hedgehog ->
                                val hedgehog = hedgehog as InstancedNode
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
        val hedgehog = Cylinder(0.005f, 1.0f, 16)
        hedgehog.visible = false
//        hedgehog.material = ShaderMaterial.fromClass(BionicTracking::class.java,
//                listOf(ShaderType.VertexShader, ShaderType.FragmentShader))
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
                    cam.showMessage("Hedgehogs hidden",distance = 1.2f, size = 0.2f)
                }

                HedgehogVisibility.PerTimePoint -> {
                    hedgehogs.visible = true
                    cam.showMessage("Hedgehogs shown per timepoint",distance = 1.2f, size = 0.2f)
                }

                HedgehogVisibility.Visible -> {
                    hedgehogs.visible = true
                    cam.showMessage("Hedgehogs visible",distance = 1.2f, size = 0.2f)
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
                cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 1.2f, size = 0.2f)
            } else {
                volumeScaleFactor = minOf(volumeScaleFactor * 1.2f, 3.0f)
                volume.scale =Vector3f(1.0f) .mul(volumeScaleFactor)
            }
        }

        val slowerOrScale = ClickBehaviour { _, _ ->
            if(playing) {
                volumesPerSecond = maxOf(minOf(volumesPerSecond-1, 20), 1)
                cam.showMessage("Speed: $volumesPerSecond vol/s",distance = 1.2f, size = 0.2f)
            } else {
                volumeScaleFactor = maxOf(volumeScaleFactor / 1.2f, 0.1f)
                volume.scale = Vector3f(1.0f) .mul(volumeScaleFactor)
            }
        }

        val playPause = ClickBehaviour { _, _ ->
            playing = !playing
            if(playing) {
                cam.showMessage("Playing",distance = 1.2f, size = 0.2f)
            } else {
                cam.showMessage("Paused",distance = 1.2f, size = 0.2f)
            }
        }

        val move = ControllerDrag(TrackerRole.LeftHand, hmd) { volume }

        val deleteLastHedgehog = ConfirmableClickBehaviour(
                armedAction = { timeout ->
                    cam.showMessage("Deleting last track, press again to confirm.",distance = 1.2f, size = 0.2f,
                            messageColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                            backgroundColor = Vector4f(1.0f, 0.2f, 0.2f, 1.0f),
                            duration = timeout.toInt())

                },
                confirmAction = {
                    hedgehogs.children.removeAt(hedgehogs.children.size-1)
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

                    cam.showMessage("Last track deleted.",distance = 1.2f, size = 0.2f,
                            messageColor = Vector4f(1.0f, 0.2f, 0.2f, 1.0f),
                            backgroundColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
                            duration = 1000)
                })

        hmd.addBehaviour("playback_direction", ClickBehaviour { _, _ ->
            direction = if(direction == PlaybackDirection.Forward) {
                PlaybackDirection.Backward
            } else {
                PlaybackDirection.Forward
            }
            cam.showMessage("Playing: ${direction}", distance = 1.2f, size =  0.2f, duration = 1000)
        })

        val cellDivision = ClickBehaviour { _, _ ->
            cam.showMessage("Adding cell division", distance = 1.2f, size =  0.2f, duration = 1000)
            //dumpHedgehog()
            //addHedgehog()
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

        setupControllerforTracking()

    }

    private fun setupControllerforTracking( keybindingTracking: String = "U") {
            thread {
                val cam = sciview.camera as? DetachedHeadCamera ?: return@thread
                val toggleTracking = ClickBehaviour { _, _ ->
                    if (tracking) {
                        referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f) }
                        cam.showMessage("Tracking deactivated.",distance = 1.2f, size = 0.2f)
                        dumpHedgehog()
                    } else {
                        addHedgehog()
                        referenceTarget.ifMaterial { diffuse = Vector3f(1.0f, 0.0f, 0.0f) }
                        cam.showMessage("Tracking active.",distance = 1.2f, size = 0.2f)
                    }
                    tracking = !tracking
                }
                hmd.addBehaviour("toggle_tracking", toggleTracking)
                hmd.addKeyBinding("toggle_tracking", keybindingTracking)

                volume.visible = true
                volume.runRecursive { it.visible = true }
                playing = true
                //val p = hmd.getPose(TrackedDeviceType.Controller).firstOrNull { it.name == "Controller-3" }?.position


                while(true)
                {
                    val p = Vector3f(0.1f,0f,-2.0f)
                    referenceTarget.spatial().position = p
                    referenceTarget.visible = true
                    //val headCenter = cam.spatial().viewportToWorld(Vector2f(0.0f, 0.0f))
                    val headCenter = Matrix4f(cam.spatial().world).transform(Vector3f(0.0f,0f,-1f).xyzw()).xyz()
                    val pointWorld = Matrix4f(cam.spatial().world).transform(p.xyzw()).xyz()
                    laser.spatial().orientBetweenPoints(headCenter,pointWorld)
                    laser.visible = true


//                    TestTarget.visible = true
                    TestTarget1.ifSpatial {  position = headCenter}
                    TestTarget2.ifSpatial {  position = pointWorld}

//                    print("center position: " + headCenter.toString())
//                    println("pointWord: " + pointWorld.toString())
                    val direction = (pointWorld - headCenter).normalize()
//                    val direction = cam.headOrientation.transform(Vector3f(0.0f,0.0f,-1.0f))
                    if (tracking) {
//                                    log.info("Starting spine from $headCenter to $pointWorld")
                        //System.out.println("tracking!!!!!!!!!!")
                        addSpine(headCenter, direction, volume,0.8f, volume.viewerState.currentTimepoint)
                    }
                }



            }       // bind calibration start to menu key on controller

    }
    fun addSpine(center: Vector3f, direction: Vector3f, volume: Volume, confidence: Float, timepoint: Int) {
        val cam = sciview.camera as? DetachedHeadCamera ?: return
        val sphere = volume.boundingBox?.getBoundingSphere() ?: return

        val sphereDirection = sphere.origin.minus(center)
        val sphereDist = Math.sqrt(sphereDirection.x * sphereDirection.x + sphereDirection.y * sphereDirection.y + sphereDirection.z * sphereDirection.z) - sphere.radius

        val p1 = center
        val temp = direction.mul(sphereDist + 2.0f * sphere.radius)
        val p2 = Vector3f(center).add(temp)

//        print("sphere.origin: " + sphere.origin.toString())
//        println("p2 position" + p2.toString())

        val spine = (hedgehogs.children.last() as InstancedNode).addInstance()
        spine.spatial().orientBetweenPoints(p1, p2, true, true)
        spine.visible = true

        val intersection = volume.intersectAABB(p1, (p2 - p1).normalize())

        if(intersection is MaybeIntersects.Intersection) {
//            System.out.println(intersection);
            // get local entry and exit coordinates, and convert to UV coords
            val localEntry = (intersection.relativeEntry) //.add(Vector3f(1.0f)) ) .mul (1.0f / 2.0f)
            val localExit = (intersection.relativeExit) //.add (Vector3f(1.0f)) ).mul  (1.0f / 2.0f)
            val (samples, localDirection) = volume.sampleRay(localEntry, localExit) ?: null to null
//            System.out.println("localEntry:"+ localEntry.toString())
//            System.out.println("localExit:" + localExit.toString())

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
                val count = samples.filterNotNull().count { it > 0.02f }

                spine.metadata["spine"] = metadata
                spine.instancedProperties["ModelMatrix"] = { spine.spatial().world }
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
        var lastHedgehog =  hedgehogs.children.last() as InstancedNode
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
            val h = HedgehogAnalysis(spines, Matrix4f(volume.world), Vector3f(volume.getDimensions()))
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

        val master = if(lastHedgehog == null) {
            val m = Cylinder(3f, 1.0f, 10)
            m.ifMaterial {
                ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag")
                diffuse = Random.random3DVectorFromRange(0.2f, 0.8f)
                roughness = 1.0f
                metallic = 0.0f
                cullingMode = Material.CullingMode.None
            }
            m.name = "Track-$hedgehogId"
            val mInstanced = InstancedNode(m)
            mInstanced
        } else {
            null
        }

        val parentId = 0
        val volumeDimensions = volume.getDimensions()

        trackFileWriter.newLine()
        trackFileWriter.newLine()
        trackFileWriter.write("# START OF TRACK $hedgehogId, child of $parentId\n")
        track.points.windowed(2, 1).forEach { pair ->
            if(master != null) {
                val element = master.addInstance()
                element.spatial().orientBetweenPoints(Vector3f(pair[0].first).mul(Vector3f(volumeDimensions)), Vector3f(pair[1].first).mul(Vector3f(volumeDimensions)), rescale = true, reposition = true)
                element.parent = volume
                master.instances.add(element)
            }
            val p = Vector3f(pair[0].first).mul(Vector3f(volumeDimensions))//direct product
            val tp = pair[0].second.timepoint
            trackFileWriter.write("$tp\t${p.x()}\t${p.y()}\t${p.z()}\t${hedgehogId}\t$parentId\t0\t0\n")
        }

        master?.let { volume.addChild(it) }

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