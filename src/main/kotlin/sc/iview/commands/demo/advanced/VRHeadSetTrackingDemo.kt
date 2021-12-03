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
import sc.iview.commands.demo.animation.ParticleDemo

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Utilize VR Headset for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class VRHeadSetTrackingDemo: Command{
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

     lateinit var hmd: OpenVRHMD
    val referenceTarget = Icosphere(0.04f, 2)

    lateinit var sessionId: String
    lateinit var sessionDirectory: Path

    var hedgehogsList =  mutableListOf<InstancedNode>()

    enum class HedgehogVisibility { Hidden, PerTimePoint, Visible }
    var hedgehogVisibility = HedgehogVisibility.Hidden

    lateinit var volume: Volume

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


                    if(hedgehogsList.size>0) {
                        if(hedgehogVisibility == HedgehogVisibility.PerTimePoint) {
                            hedgehogsList.forEach { hedgehog->
                                hedgehog.instances.forEach {
                                    it.visible = (it.metadata["spine"] as SpineMetadata).timepoint == volume.viewerState.currentTimepoint
                                }
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
        val hedgehogMaster = Cylinder(0.1f, 1.0f, 16)
        hedgehogMaster.visible = false
        hedgehogMaster.setMaterial(ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag"))
        hedgehogMaster.ifMaterial{
            ambient = Vector3f(0.1f, 0f, 0f)
            diffuse = Random.random3DVectorFromRange(0.2f, 0.8f)
            metallic = 0.01f
            roughness = 0.5f
        }
        var hedgehogInstanced = InstancedNode(hedgehogMaster)
        sciview.addNode(hedgehogInstanced)
        hedgehogsList.add(hedgehogInstanced)
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
                    hedgehogsList.forEach { hedgehog ->
                        println("the number of spines: " + hedgehog.instances.size.toString())
                        hedgehog.instances.forEach { it.visible = false }
                    }
                    cam.showMessage("Hedgehogs hidden",distance = 1.2f, size = 0.2f)
                }

                HedgehogVisibility.PerTimePoint -> {
                    cam.showMessage("Hedgehogs shown per timepoint",distance = 1.2f, size = 0.2f)
                }

                HedgehogVisibility.Visible -> {
                    println("the number of hedgehogs: "+ hedgehogsList.size.toString())
                    hedgehogsList.forEach { hedgehog ->
                        println("the number of spines: " + hedgehog.instances.size.toString())
                        hedgehog.instances.forEach { it.visible = true }
                    }
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
                    hedgehogsList =  hedgehogsList.dropLast(1) as MutableList
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
                        tracking = false
                        dumpHedgehog()
                        println("before dumphedgehog: "+ hedgehogsList.last().instances.size.toString())
                    } else {
                        addHedgehog()
                        println("after addhedgehog: "+ hedgehogsList.last().instances.size.toString())
                        referenceTarget.ifMaterial { diffuse = Vector3f(1.0f, 0.0f, 0.0f) }
                        cam.showMessage("Tracking active.",distance = 1.2f, size = 0.2f)
                        tracking = true
                    }
                }
                hmd.addBehaviour("toggle_tracking", toggleTracking)
                hmd.addKeyBinding("toggle_tracking", keybindingTracking)

                volume.visible = true
                volume.runRecursive { it.visible = true }
                playing = true


                while(true)
                {

                    val headCenter = Matrix4f(cam.spatial().world).transform(Vector3f(0.0f,0f,-1f).xyzw()).xyz()
                    val pointWorld = Matrix4f(cam.spatial().world).transform(Vector3f(0.0f,0f,-2f).xyzw()).xyz()

                    referenceTarget.visible = true
                    referenceTarget.ifSpatial { position = Vector3f(0.0f,0f,-2f) }

                    val direction = (pointWorld - headCenter).normalize()
                    if (tracking) {
                        addSpine(headCenter, direction, volume,0.8f, volume.viewerState.currentTimepoint)
                    }
                }
            }

    }
    fun addSpine(center: Vector3f, direction: Vector3f, volume: Volume, confidence: Float, timepoint: Int) {
        val cam = sciview.camera as? DetachedHeadCamera ?: return
        val sphere = volume.boundingBox?.getBoundingSphere() ?: return

        val sphereDirection = Vector3f(sphere.origin).minus(Vector3f(center))
        val sphereDist = Math.sqrt(sphereDirection.x * sphereDirection.x + sphereDirection.y * sphereDirection.y + sphereDirection.z * sphereDirection.z) - sphere.radius

        val p1 = Vector3f(center)
        val temp = Vector3f(direction).mul(sphereDist + 2.0f * sphere.radius)
        val p2 = Vector3f(center).add(temp)

        var hedgehogsInstance = hedgehogsList.last()
        val spine = hedgehogsInstance.addInstance()
        spine.spatial().orientBetweenPoints(p1, p2,true,true)
        spine.visible = false

        val intersection = volume.intersectAABB(p1, (p2 - p1).normalize())

        if(intersection is MaybeIntersects.Intersection) {
            // get local entry and exit coordinates, and convert to UV coords
            val localEntry = (intersection.relativeEntry) //.add(Vector3f(1.0f)) ) .mul (1.0f / 2.0f)
            val localExit = (intersection.relativeExit) //.add (Vector3f(1.0f)) ).mul  (1.0f / 2.0f)
            val (samples, localDirection) = volume.sampleRay(localEntry, localExit) ?: null to null

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
                //println("cnt: " +  count.toString())
                spine.metadata["spine"] = metadata
                spine.instancedProperties["ModelMatrix"] = { spine.spatial().world }
//                spine.instancedProperties["Metadata"] = { Vector4f(confidence, timepoint.toFloat()/volume.timepointCount, count.toFloat(), 0.0f) }
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
        //println("size of hedgehogslist: " + hedgehogsList.size.toString())
        var lastHedgehog =  hedgehogsList.last()
        println("lastHedgehog: ${lastHedgehog}")
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

        val master = Cylinder(0.1f, 1.0f, 10)
        master.setMaterial (ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag"))

        master.ifMaterial{
            ambient = Vector3f(0.1f, 0f, 0f)
            diffuse = Random.random3DVectorFromRange(0.2f, 0.8f)
            metallic = 0.01f
            roughness = 0.5f
        }
        master.name = "Track-$hedgehogId"
        val mInstanced = InstancedNode(master)

        val parentId = 0
        val volumeDimensions = volume.getDimensions()
        sciview.addNode(mInstanced)

        trackFileWriter.newLine()
        trackFileWriter.newLine()
        trackFileWriter.write("# START OF TRACK $hedgehogId, child of $parentId\n")
        track.points.windowed(2, 1).forEach { pair ->
                val element = mInstanced.addInstance()
                val p0 = Vector3f(pair[0].first).mul(Vector3f(volumeDimensions))//direct product
                val p1 = Vector3f(pair[1].first).mul(Vector3f(volumeDimensions))
                val p0w = Matrix4f(volume.spatial().world).transform(p0.xyzw()).xyz()
                val p1w = Matrix4f(volume.spatial().world).transform(p1.xyzw()).xyz()
                element.spatial().orientBetweenPoints(p0w,p1w, rescale = true, reposition = true)
                //mInstanced.instances.add(element)
                val pp = Icosphere(0.01f, 1)
                pp.spatial().position = p0w
                pp.material().diffuse = Vector3f(0.5f, 0.3f, 0.8f)
                sciview.addChild(pp)

            val p = Vector3f(pair[0].first).mul(Vector3f(volumeDimensions))//direct product
            val tp = pair[0].second.timepoint
            trackFileWriter.write("$tp\t${p.x()}\t${p.y()}\t${p.z()}\t${hedgehogId}\t$parentId\t0\t0\n")
        }
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