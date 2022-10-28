package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.bionictracking.HedgehogAnalysis
import graphics.scenery.bionictracking.SpineMetadata
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.numerics.Random
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Volume
import org.joml.*
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Test without VR and Eye Tracker", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class Test: Command{
    @Parameter
    private lateinit var sciview: SciView

    lateinit var hmd: OpenVRHMD
    val referenceTarget = Icosphere(0.004f, 2)
    //val calibrationTarget = Icosphere(0.02f, 2)
    val TestTarget = Icosphere(0.1f, 2)

    val laser = Cylinder(0.005f, 0.2f, 10)


    lateinit var sessionId: String
    lateinit var sessionDirectory: Path
    lateinit var point1:Icosphere
    lateinit var point2:Icosphere


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
    var direction = PlaybackDirection.Backward
    @Parameter(label = "Volumes per second")
    var volumesPerSecond = 1
    var skipToNext = false
    var skipToPrevious = false
//	var currentVolume = 0

    var volumeScaleFactor = 1.0f

    override fun run() {

        sciview.addChild(TestTarget)
        TestTarget.visible = false


//        sciview.toggleVRRendering()
//        hmd = sciview.hub.getWorkingHMD() as? OpenVRHMD ?: throw IllegalStateException("Could not find headset")
        sessionId = "BionicTracking-generated-${SystemHelpers.formatDateTime()}"
        sessionDirectory = Files.createDirectory(Paths.get(System.getProperty("user.home"), "Desktop", sessionId))

        referenceTarget.visible = false
        referenceTarget.ifMaterial{
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(0.8f, 0.8f, 0.8f)
        }
        sciview.camera!!.addChild(referenceTarget)

        laser.visible = false
        laser.ifMaterial{diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        sciview.addChild(laser)

        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }
        shell.name = "shell"
        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        sciview.addChild(shell)

        volume = sciview.find("volume") as Volume
        volume.visible = false

        point1 = Icosphere(0.1f, 2)
        point1.spatial().position = Vector3f(1.858f,2f,8.432f)
        point1.ifMaterial{ diffuse = Vector3f(0.5f, 0.3f, 0.8f)}
        sciview.addChild(point1)

        point2 = Icosphere(0.1f, 2)
        point2.spatial().position = Vector3f(1.858f, 2f, -10.39f)
        point2.ifMaterial {diffuse = Vector3f(0.3f, 0.8f, 0.3f)}
        sciview.addChild(point2)


        val connector = Cylinder.betweenPoints(point1.position, point2.position)
        connector.ifMaterial {diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        sciview.addChild(connector)


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
                    //println("timepoint: "+ newTimepoint);

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
                        //dumpHedgehog()
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
        setupControllerforTracking()

    }

    private fun setupControllerforTracking( keybindingTracking: String = "U") {
        thread {
            val cam = sciview.camera as? DetachedHeadCamera ?: return@thread
            volume.visible = true
            volume.runRecursive { it.visible = true }
            playing = true
            tracking = true
            //val p = hmd.getPose(TrackedDeviceType.Controller).firstOrNull { it.name == "Controller-3" }?.position

            if(true)
            {
//                val p = Vector3f(0f,0f,-1f)
//                referenceTarget.position = p
//                referenceTarget.visible = true
                val headCenter = point1.position//cam.viewportToWorld(Vector2f(0.0f, 0.0f))
                val pointWorld = point2.position///Matrix4f(cam.world).transform(p.xyzw()).xyz()
//
                val direction = (pointWorld - headCenter).normalize()

                if (tracking) {
//                                    log.info("Starting spine from $headCenter to $pointWorld")
                    //System.out.println("tracking!!!!!!!!!!")
//                    println("direction:"+ direction.toString())
                    addSpine(headCenter, direction, volume,0.8f, volume.viewerState.currentTimepoint)
                    showTrack()
                }
                Thread.sleep(200)
            }
            //referenceTarget.visible = true
            // Pupil has mm units, so we divide by 1000 here to get to scenery units


        }       // bind calibration start to menu key on controller

    }

    private fun showTrack()
    {
//        val file = File("C:\\Users\\lanru\\Desktop\\BionicTracking-generated-2022-05-25 16.04.52\\Hedgehog_1_2022-05-25 16.06.03.csv")
        val file = File("C:\\Users\\lanru\\Desktop\\BionicTracking-generated-2022-10-19 13.48.51\\Hedgehog_1_2022-10-19 13.49.41.csv")

        var volumeDimensions = volume.getDimensions()
        var selfdefineworlfmatrix = volume.spatial().world
       // volumeDimensions = Vector3f(700.0f,660.0f,113.0f)
//        selfdefineworlfmatrix = Matrix4f(
//            0.015f, 0f, 0f, 0f,
//            0f, -0.015f, 0f, 0f,
//            0f, 0f, 0.045f, 0f,
//            -5f, 8f, -2f, 1f
//        )
        val analysis = HedgehogAnalysis.fromCSVWithMatrix(file,selfdefineworlfmatrix)
        print("volume.getDimensions(): "+ volume.getDimensions())
        print("volume.spatial().world: "+ volume.spatial().world)
        print("selfdefineworlfmatrix: "+ selfdefineworlfmatrix)

        val track = analysis.run()

        print("flag1")
        val master = Cylinder(0.1f, 1.0f, 10)
            master.setMaterial (ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag"))
        print("flag2")
        master.ifMaterial{
            ambient = Vector3f(0.1f, 0f, 0f)
            diffuse = Random.random3DVectorFromRange(0.2f, 0.8f)
            metallic = 0.01f
            roughness = 0.5f
        }

        val mInstanced = InstancedNode(master)
        sciview.addNode(mInstanced)


        print("flag3")
        if(track == null)
        {
            return
        }
        print("flag4")
        track.points.windowed(2, 1).forEach { pair ->

            val element = mInstanced.addInstance()
            val p0 = Vector3f(pair[0].first)//direct product
            val p1 = Vector3f(pair[1].first)
            val p0w = Matrix4f(volume.spatial().world).transform(p0.xyzw()).xyz()
            val p1w = Matrix4f(volume.spatial().world).transform(p1.xyzw()).xyz()
            element.spatial().orientBetweenPoints(p0w,p1w, rescale = true, reposition = true)

            val tp = pair[0].second.timepoint
            val pp = Icosphere(0.1f, 1)
            pp.name = "trackpoint_${tp}_${pair[0].first.x}_${pair[0].first.y}_${pair[0].first.z}"
//            println("volumeDimensions: " + volumeDimensions)
//            println("volume.spatial().world: " + volume.spatial().world)
            println("the local position of the point is:" + pair[0].first)
            println("the world position of the point is: "+ p0w)
            pp.spatial().position = p0w
            pp.material().diffuse = Vector3f(0.5f, 0.3f, 0.8f)
            sciview.addNode(pp)
        }
    }

    fun addSpine(center: Vector3f, direction: Vector3f, volume: Volume, confidence: Float, timepoint: Int) {
        val cam = sciview.camera as? DetachedHeadCamera ?: return
        val sphere = volume.boundingBox?.getBoundingSphere() ?: return

        val sphereDirection = sphere.origin.minus(center)

        val sphereDist = Math.sqrt(sphereDirection.x * sphereDirection.x + sphereDirection.y * sphereDirection.y + sphereDirection.z * sphereDirection.z) - sphere.radius

        val p1 =  center
        val temp = direction.mul(sphereDist + 2.0f * sphere.radius)


        val p2 = Vector3f(center).add(temp)


//        print("center position: " + p1.toString())
//        print("p2 position" + p2.toString())

//        TestTarget.visible = true
//        TestTarget.ifSpatial { position = p2}


//        val spine = (hedgehogs.children.last() as InstancedNode).addInstance()
//        spine.spatial().orientBetweenPoints(p1, p2, true, true)
//        spine.visible = true

        val intersection = volume.spatial().intersectAABB(p1, (p2 - p1).normalize())
        System.out.println(intersection);
        if(intersection is MaybeIntersects.Intersection) {
            // get local entry and exit coordinates, and convert to UV coords
            val localEntry = (intersection.relativeEntry) //.add(Vector3f(1.0f)) ) .mul (1.0f / 2.0f)
            val localExit = (intersection.relativeExit) //.add (Vector3f(1.0f)) ).mul  (1.0f / 2.0f)
            val nf = DecimalFormat("0.0000")
            println("Ray intersects volume at world=${intersection.entry.toString(nf)}/${intersection.exit.toString(nf)} local=${localEntry.toString(nf)}/${localExit.toString(nf)} ")

//            System.out.println("localEntry:"+ localEntry.toString())
//            System.out.println("localExit:" + localExit.toString())
//            val worldpositionEntry = volume.spatial().world.transform((Vector3f(localEntry)).xyzw()).xyz()
//            val worldpositionExit = volume.spatial().world.transform((Vector3f(localExit)).xyzw()).xyz()
//            System.out.println("worldEntry:"+ worldpositionEntry.toString())
//            System.out.println("worldExit:" + worldpositionExit.toString())


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
                val count = samples.filterNotNull().count { it > 0.002f }

                    println("count of samples: "+ count.toString())
println(samples)

//                spine.metadata["spine"] = metadata
//                spine.instancedProperties["ModelMatrix"] = { spine.spatial().world }
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
//    fun dumpHedgehog() {
//        var lastHedgehog =  hedgehogs.children.last() as InstancedNode
//        val hedgehogId = hedgehogIds.incrementAndGet()
//
//        val hedgehogFile = sessionDirectory.resolve("Hedgehog_${hedgehogId}_${SystemHelpers.formatDateTime()}.csv").toFile()
//        val hedgehogFileWriter = hedgehogFile.bufferedWriter()
//        hedgehogFileWriter.write("Timepoint,Origin,Direction,LocalEntry,LocalExit,LocalDirection,HeadPosition,HeadOrientation,Position,Confidence,Samples\n")
//
//        val trackFile = sessionDirectory.resolve("Tracks.tsv").toFile()
//        val trackFileWriter = BufferedWriter(FileWriter(trackFile, true))
//        if(!trackFile.exists()) {
//            trackFile.createNewFile()
//            trackFileWriter.write("# BionicTracking cell track listing for ${sessionDirectory.fileName}\n")
//            trackFileWriter.write("# TIME\tX\tYt\t\tZ\tTRACK_ID\tPARENT_TRACK_ID\tSPOT\tLABEL\n")
//        }
//
//
//        val spines = lastHedgehog.instances.mapNotNull { spine ->
//            spine.metadata["spine"] as? SpineMetadata
//        }
//
//        spines.forEach { metadata ->
//            hedgehogFileWriter.write("${metadata.timepoint};${metadata.origin};${metadata.direction};${metadata.localEntry};${metadata.localExit};${metadata.localDirection};${metadata.headPosition};${metadata.headOrientation};${metadata.position};${metadata.confidence};${metadata.samples.joinToString(";")}\n")
//        }
//        hedgehogFileWriter.close()
//
//        val existingAnalysis = lastHedgehog.metadata["HedgehogAnalysis"] as? HedgehogAnalysis.Track
//        val track = if(existingAnalysis is HedgehogAnalysis.Track) {
//            existingAnalysis
//        } else {
//            val h = HedgehogAnalysis(spines, Matrix4f(volume.world), Vector3f(volume.getDimensions()))
//            h.run()
//        }
//
//        if(track == null) {
////            logger.warn("No track returned")
//            sciview.camera?.showMessage("No track returned", distance = 1.2f, size = 0.2f,messageColor = Vector4f(1.0f, 0.0f, 0.0f,1.0f))
//            return
//        }
//
//        lastHedgehog.metadata["HedgehogAnalysis"] = track
//        lastHedgehog.metadata["Spines"] = spines
//
////        logger.info("---\nTrack: ${track.points.joinToString("\n")}\n---")
//
//        val master = if(lastHedgehog == null) {
//            val m = Cylinder(3f, 1.0f, 10)
//            m.ifMaterial {
//                ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag")
//                diffuse = Random.random3DVectorFromRange(0.2f, 0.8f)
//                roughness = 1.0f
//                metallic = 0.0f
//                cullingMode = Material.CullingMode.None
//            }
//            m.name = "Track-$hedgehogId"
//            val mInstanced = InstancedNode(m)
//            mInstanced
//        } else {
//            null
//        }
//
//        val parentId = 0
//        val volumeDimensions = volume.getDimensions()
//
//        trackFileWriter.newLine()
//        trackFileWriter.newLine()
//        trackFileWriter.write("# START OF TRACK $hedgehogId, child of $parentId\n")
//        track.points.windowed(2, 1).forEach { pair ->
//            if(master != null) {
//                val element = master.addInstance()
//                element.spatial().orientBetweenPoints(Vector3f(pair[0].first).mul(Vector3f(volumeDimensions)), Vector3f(pair[1].first).mul(Vector3f(volumeDimensions)), rescale = true, reposition = true)
//                element.parent = volume
//                master.instances.add(element)
//            }
//            val p = Vector3f(pair[0].first).mul(Vector3f(volumeDimensions))//direct product
//            val tp = pair[0].second.timepoint
//            trackFileWriter.write("$tp\t${p.x()}\t${p.y()}\t${p.z()}\t${hedgehogId}\t$parentId\t0\t0\n")
//        }
//
//        master?.let { volume.addChild(it) }
//
//        trackFileWriter.close()
//    }

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