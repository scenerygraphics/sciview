package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.numerics.Random
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.MaybeIntersects
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Volume
import org.joml.*
import sc.iview.SciView
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
import kotlin.concurrent.thread

/**
 * A class to test to show tracks and perform track analysis from saved CSV tracking files without
 * the requirement of a VR headset.
 */
class TrackingTest(
    sciview: SciView
): CellTrackingBase(sciview) {

    val TestTarget = Icosphere(0.1f, 2)

    val laser = Cylinder(0.005f, 0.2f, 10)

    lateinit var point1:Icosphere
    lateinit var point2:Icosphere

    val confidenceThreshold = 0.60f

    override fun run() {

        sciview.addNode(TestTarget)
        TestTarget.visible = false

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
        sciview.addNode(laser)

        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }
        shell.name = "shell"
        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        sciview.addNode(shell)

        volume = sciview.find("volume") as Volume
        volume.visible = false

        point1 = Icosphere(0.1f, 2)
        point1.spatial().position = Vector3f(1.858f,2f,8.432f)
        point1.ifMaterial{ diffuse = Vector3f(0.5f, 0.3f, 0.8f)}
        sciview.addNode(point1)

        point2 = Icosphere(0.1f, 2)
        point2.spatial().position = Vector3f(1.858f, 2f, -10.39f)
        point2.ifMaterial {diffuse = Vector3f(0.3f, 0.8f, 0.3f)}
        sciview.addNode(point2)


        val connector = Cylinder.betweenPoints(point1.spatial().position, point2.spatial().position)
        connector.ifMaterial {diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        sciview.addNode(connector)


        val bb = BoundingGrid()
        bb.node = volume
        bb.visible = false

        sciview.addNode(hedgehogs)

        val pupilFrameLimit = 20
        var lastFrame = System.nanoTime()

        val debugBoard = TextBoard()
        debugBoard.name = "debugBoard"
        debugBoard.spatial().scale = Vector3f(0.05f, 0.05f, 0.05f)
        debugBoard.spatial().position = Vector3f(0.0f, -0.3f, -0.9f)
        debugBoard.text = ""
        debugBoard.visible = false
        sciview.camera?.addChild(debugBoard)

        val lights = Light.createLightTetrahedron<PointLight>(Vector3f(0.0f, 0.0f, 0.0f), spread = 5.0f, radius = 15.0f, intensity = 5.0f)
        lights.forEach { sciview.addNode(it) }


        thread{
            inputSetup()
        }

        launchUpdaterThread()
    }

    override fun inputSetup()
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
            eyeTrackingActive = true

            if(true)
            {
                val headCenter = point1.spatial().position//cam.viewportToWorld(Vector2f(0.0f, 0.0f))
                val pointWorld = point2.spatial().position///Matrix4f(cam.world).transform(p.xyzw()).xyz()

                val direction = (pointWorld - headCenter).normalize()

                if (eyeTrackingActive) {
                    addSpine(headCenter, direction, volume,0.8f, volume.viewerState.currentTimepoint)
                    showTrack()
                }
                Thread.sleep(200)
            }
        }

    }

    private fun showTrack()
    {
        val file = File("C:\\Users\\lanru\\Desktop\\BionicTracking-generated-2022-10-19 13.48.51\\Hedgehog_1_2022-10-19 13.49.41.csv")

        var volumeDimensions = volume.getDimensions()
        var selfdefineworlfmatrix = volume.spatial().world
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
            println("the local position of the point is:" + pair[0].first)
            println("the world position of the point is: "+ p0w)
            pp.spatial().position = p0w
            pp.material().diffuse = Vector3f(0.5f, 0.3f, 0.8f)
            sciview.addNode(pp)
        }
    }

    override fun addSpine(center: Vector3f, direction: Vector3f, volume: Volume, confidence: Float, timepoint: Int) {
        val cam = sciview.camera as? DetachedHeadCamera ?: return
        val sphere = volume.boundingBox?.getBoundingSphere() ?: return

        val sphereDirection = sphere.origin.minus(center)
        val sphereDist = Math.sqrt(sphereDirection.x * sphereDirection.x + sphereDirection.y * sphereDirection.y + sphereDirection.z * sphereDirection.z) - sphere.radius

        val p1 =  center
        val temp = direction.mul(sphereDist + 2.0f * sphere.radius)
        val p2 = Vector3f(center).add(temp)

        val intersection = volume.spatial().intersectAABB(p1, (p2 - p1).normalize())
        System.out.println(intersection);
        if(intersection is MaybeIntersects.Intersection) {
            // get local entry and exit coordinates, and convert to UV coords
            val localEntry = (intersection.relativeEntry) //.add(Vector3f(1.0f)) ) .mul (1.0f / 2.0f)
            val localExit = (intersection.relativeExit) //.add (Vector3f(1.0f)) ).mul  (1.0f / 2.0f)
            val nf = DecimalFormat("0.0000")
            println("Ray intersects volume at world=${intersection.entry.toString(nf)}/${intersection.exit.toString(nf)} local=${localEntry.toString(nf)}/${localExit.toString(nf)} ")

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
                val count = samples.filterNotNull().count { it > 0.002f }

                logger.info("count of samples: $count")
                logger.info(samples.joinToString { ", " })
            }
        }
    }
}