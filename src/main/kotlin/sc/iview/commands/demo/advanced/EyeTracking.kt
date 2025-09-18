package sc.iview.commands.demo.advanced

import edu.mines.jtk.opt.Vect
import graphics.scenery.*
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.eyetracking.PupilEyeTracker
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import graphics.scenery.textures.Texture
import graphics.scenery.ui.Button
import graphics.scenery.ui.Column
import graphics.scenery.ui.ToggleButton
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.*
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.apache.commons.math3.ml.clustering.Clusterable
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import org.joml.*
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Vector
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.time.TimeSource

/**
 * Tracking class used for communicating with eye trackers, tracking cells with them in a sciview VR environment.
 * It calls the Hedgehog analysis on the eye tracking results and communicates the results to Mastodon via
 * [trackCreationCallback], which is called on every spine graph vertex that is extracted
 */
class EyeTracking(
    sciview: SciView
): CellTrackingBase(sciview) {

    val pupilTracker = PupilEyeTracker(calibrationType = PupilEyeTracker.CalibrationType.WorldSpace, port = System.getProperty("PupilPort", "50020").toInt())
    val calibrationTarget = Icosphere(0.02f, 2)
    val laser = Cylinder(0.005f, 0.2f, 10)

    val confidenceThreshold = 0.60f

    private lateinit var debugBoard: TextBoard

    var leftEyeTrackColumn: Column? = null

    enum class TrackingType { Follow, Pick }

    private var currentTrackingType = TrackingType.Follow

    override fun run() {
        // Do all the things for general VR startup before setting up the eye tracking environment
        super.run()

        sessionId = "BionicTracking-generated-${SystemHelpers.formatDateTime()}"
        sessionDirectory = Files.createDirectory(Paths.get(System.getProperty("user.home"), "Desktop", sessionId))

        referenceTarget.visible = false
        referenceTarget.ifMaterial{
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(0.8f, 0.8f, 0.8f)
        }
        referenceTarget.name = "Reference Target"
        sciview.camera?.addChild(referenceTarget)

        calibrationTarget.visible = false
        calibrationTarget.material {
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(1.0f, 1.0f, 1.0f)
        }
        calibrationTarget.name = "Calibration Target"
        sciview.camera?.addChild(calibrationTarget)

        laser.visible = false
        laser.ifMaterial{diffuse = Vector3f(1.0f, 1.0f, 1.0f) }
        laser.name = "Laser"
        sciview.addNode(laser)

        val bb = BoundingGrid()
        bb.node = volume
        bb.visible = false

        sciview.addNode(hedgehogs)

        val eyeFrames = Mesh("eyeFrames")
        val left = Box(Vector3f(1.0f, 1.0f, 0.001f))
        val right = Box(Vector3f(1.0f, 1.0f, 0.001f))
        left.spatial().position = Vector3f(-1.0f, 1.5f, 0.0f)
        left.spatial().rotation = left.spatial().rotation.rotationZ(PI.toFloat())
        right.spatial().position = Vector3f(1.0f, 1.5f, 0.0f)
        eyeFrames.addChild(left)
        eyeFrames.addChild(right)

        sciview.addNode(eyeFrames)

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
        debugBoard = TextBoard()
        debugBoard.name = "debugBoard"
        debugBoard.spatial().scale = Vector3f(0.05f, 0.05f, 0.05f)
        debugBoard.spatial().position = Vector3f(0.0f, -0.3f, -0.9f)
        debugBoard.text = ""
        debugBoard.visible = false
        sciview.camera?.addChild(debugBoard)

        hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
            if (device.type == TrackedDeviceType.Controller) {
                setupEyeTracking()
                setupEyeTrackingMenu()
            }
        }

        // Attach a behavior to the main loop that stops the eye tracking once we reached the first time point
        // and analyzes the created track.
        attachToLoop {
            val newTimepoint = volume.viewerState.currentTimepoint
            if (eyeTrackingActive && newTimepoint == 0) {
                eyeTrackingActive = false
                playing = false
                referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f) }
                logger.info("Deactivated eye tracking by reaching timepoint 0.")
                sciview.camera!!.showMessage("Tracking deactivated.", distance = 2f, size = 0.2f, centered = true)
                analyzeEyeTrack()
            }
        }

    }


    private fun setupEyeTracking() {
        val cam = sciview.camera as? DetachedHeadCamera ?: return

        val toggleTracking = ClickBehaviour { _, _ ->
            if (!pupilTracker.isCalibrated) {
                logger.warn("Can't do eye tracking because eye trackers are not calibrated yet.")
                return@ClickBehaviour
            }
            if (eyeTrackingActive) {
                logger.info("deactivated tracking through user input.")
                referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f) }
                cam.showMessage("Tracking deactivated.",distance = 2f, size = 0.2f, centered = true)
                if (currentTrackingType == TrackingType.Follow) {
                    analyzeEyeTrack()
                } else {
                    analyzeGazeClusters()
                }
                playing = false
            } else {
                logger.info("activating tracking...")
                playing = if (currentTrackingType == TrackingType.Follow) {
                    true
                } else {
                    false
                }
                addHedgehog()
                referenceTarget.ifMaterial { diffuse = Vector3f(1.0f, 0.0f, 0.0f) }
                cam.showMessage("Tracking active.",distance = 2f, size = 0.2f, centered = true)
            }
            eyeTrackingActive = !eyeTrackingActive
        }

        mapper.setKeyBindAndBehavior(hmd, "eyeTracking", toggleTracking)
    }

    private fun calibrateEyeTrackers(force: Boolean = false) {
        thread {
            val cam = sciview.camera as? DetachedHeadCamera ?: return@thread
            pupilTracker.gazeConfidenceThreshold = confidenceThreshold
            if (!pupilTracker.isCalibrated || force) {
                logger.info("Calibrating pupil trackers...")

                volume.visible = false

                pupilTracker.onCalibrationInProgress = {
                    cam.showMessage(
                        "Crunching equations ...",
                        distance = 2f, size = 0.2f,
                        messageColor = Vector4f(1.0f, 0.8f, 0.0f, 1.0f),
                        duration = 15000, centered = true
                    )
                }

                pupilTracker.onCalibrationFailed = {
                    cam.showMessage(
                        "Calibration failed.",
                        distance = 2f, size = 0.2f,
                        messageColor = Vector4f(1.0f, 0.0f, 0.0f, 1.0f),
                        centered = true
                    )
                }

                pupilTracker.onCalibrationSuccess = {
                    cam.showMessage(
                        "Calibration succeeded!",
                        distance = 2f, size = 0.2f,
                        messageColor = Vector4f(0.0f, 1.0f, 0.0f, 1.0f),
                        centered = true
                    )

                    for (i in 0 until 20) {
                        referenceTarget.ifMaterial{diffuse = Vector3f(0.0f, 1.0f, 0.0f) }
                        Thread.sleep(100)
                        referenceTarget.ifMaterial { diffuse = Vector3f(0.8f, 0.8f, 0.8f) }
                        Thread.sleep(30)
                    }

                    if (!pupilTracker.isCalibrated) {
                        hmd.removeBehaviour("start_calibration")
                        hmd.removeKeyBinding("start_calibration")
                    }

                    volume.visible = true
                    playing = false
                }

                pupilTracker.unsubscribeFrames()
                sciview.deleteNode(sciview.find("eyeFrames"))

                logger.info("Starting eye tracker calibration")
                cam.showMessage("Follow the white rabbit.", distance = 2f, size = 0.2f,duration = 1500, centered = true)

                pupilTracker.calibrate(cam, hmd,
                    generateReferenceData = true,
                    calibrationTarget = calibrationTarget)

                pupilTracker.onGazeReceived = when (pupilTracker.calibrationType) {

                    PupilEyeTracker.CalibrationType.WorldSpace -> { gaze ->
                        if (gaze.confidence > confidenceThreshold) {
                            val p = gaze.gazePoint()
                            referenceTarget.visible = true
                            // Pupil has mm units, so we divide by 1000 here to get to scenery units
                            referenceTarget.spatial().position = p
                            (cam.children.find { it.name == "debugBoard" } as? TextBoard)?.text = "${String.format("%.2f", p.x())}, ${String.format("%.2f", p.y())}, ${String.format("%.2f", p.z())}"

                            val headCenter = cam.spatial().viewportToWorld(Vector2f(0.0f, 0.0f))
                            val pointWorld = Matrix4f(cam.spatial().world).transform(p.xyzw()).xyz()
                            val direction = (pointWorld - headCenter).normalize()

                            if (eyeTrackingActive) {
                                addSpine(headCenter, direction, volume, gaze.confidence, volume.viewerState.currentTimepoint)
                            }
                        }
                    }
                }
                logger.info("Calibration routine done.")
            }
        }
    }

    private fun setupEyeTrackingMenu() {

        val calibrateButton = Button("Calibrate",
            command = { calibrateEyeTrackers() },
            byTouch = true, depressDelay = 500)

        val toggleHedgehogsBtn = ToggleButton(
            "Hedgehogs Off",
            "Hedgehogs On",
            command = {
                hedgehogVisibility = if (hedgehogVisibility == HedgehogVisibility.Hidden) {
                    HedgehogVisibility.PerTimePoint
                } else {
                    HedgehogVisibility.Hidden
                }
            },
            byTouch = true
        )

        val toggleTrackTypeBtn = ToggleButton(
            "Follow Cell",
            "Count Cells",
            command = {
                currentTrackingType = if (currentTrackingType == TrackingType.Follow) {
                    TrackingType.Pick
                } else {
                    TrackingType.Follow
                }
            },
            byTouch = true,
            color = Vector3f(0.65f, 1f, 0.22f),
            pressedColor = Vector3f(0.15f, 0.2f, 1f)
        )

        leftEyeTrackColumn =
            createWristMenuColumn(toggleTrackTypeBtn, toggleHedgehogsBtn, calibrateButton, name = "Eye Tracking Menu")
        leftEyeTrackColumn?.visible = false
    }

    /** Writes the accumulated gazes (hedgehog) to a file, analyzes it,
     * sends the track to Mastodon and writes the track to a file. */
    private fun analyzeEyeTrack() {
        val lastHedgehog =  hedgehogs.children.last() as InstancedNode
        val hedgehogId = hedgehogIds.incrementAndGet()

        writeHedgehogToFile(lastHedgehog, hedgehogId)

        val spines = getSpinesFromHedgehog(lastHedgehog)

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

        if (trackCreationCallback != null && rebuildGeometryCallback != null) {
            trackCreationCallback?.invoke(track.points, cursor.radius,false, null, null)
            rebuildGeometryCallback?.invoke()
        } else {
            logger.warn("Tried to send track data to Mastodon but couldn't find the callbacks!")
        }

        writeTrackToFile(track.points, hedgehogId)

    }

    private fun Vector3f.toDoubleArray(): DoubleArray {
        return this.toFloatArray().map { it.toDouble() }.toDoubleArray()
    }

    fun DoubleArray.toVector3f(): Vector3f {
        require(size == 3) { "DoubleArray must have exactly 3 elements" }
        return Vector3f(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())
    }

    private fun getSpinesFromHedgehog(hedgehog: InstancedNode): List<SpineMetadata> {
        return hedgehog.instances.mapNotNull { spine ->
            spine.metadata["spine"] as? SpineMetadata
        }
    }

    /** Performs an analysis of collected gazes (hedgehogs) by first calculating the rotational distance between
     * subsequent gazes, then discards all gazes larger than 0.3x median distance, clusters the remaining directions
     * and samples the volume using the cluster centers as directions. It then extracts the first local minima and sends
     * them as spots to Mastodon. */
    private fun analyzeGazeClusters() {
        logger.info("Starting analysis of gaze clusters...")
        val lastHedgehog =  hedgehogs.children.last() as InstancedNode
        val hedgehogId = hedgehogIds.incrementAndGet()

        writeHedgehogToFile(lastHedgehog, hedgehogId)
        // Get spines from the most recent hedgehog
        val spines = getSpinesFromHedgehog(lastHedgehog)
        logger.info("Starting with ${spines.size} spines")

        // Calculate the distance from each direction to its neighbor
        val speeds = spines.zipWithNext { a, b -> a.direction.distance(b.direction) }
        logger.info("Min speed: ${speeds.min()}, max speed: ${speeds.max()}")
        val medianSpeed = speeds.sorted()[speeds.size/2]
        logger.info("Median speed: $medianSpeed")

        // Clean the list of spines by removing the ones that are too far from their neighbors
        val cleanedSpines = spines.filterIndexed { index, _ -> speeds[index] < 0.3 * medianSpeed }
        logger.info("After cleaning: ${cleanedSpines.size} spines remain")

        var start = TimeSource.Monotonic.markNow()
        // Assuming ten times the median distance is a good clustering value...
        val clustering = DBSCANClusterer<Clusterable>((10 * medianSpeed).toDouble(), 3)

        // Create a map to efficiently find spine metadata by direction
        val spineByDirection = cleanedSpines.associateBy { it.direction.toDoubleArray().contentHashCode() }

        val clusters = clustering.cluster(cleanedSpines.map {
            Clusterable {
                // On the fly conversion of a Vector3f to a double array
                it.direction.toDoubleArray()
            }
        })
        logger.info("Clustering took ${TimeSource.Monotonic.markNow() - start}")
        logger.info("We got ${clusters.size} clusters")

        // Extract the mean direction for each cluster,
        // and find the corresponding start positions and average them too
        val clusterCenters = clusters.map { cluster ->
            var meanDir = Vector3f()
            var meanPos = Vector3f()

            // Each "point" in the cluster is actually the ray direction
            cluster.points.forEach { point ->
                // Accumulate the directions
                meanDir += point.point.toVector3f()
                // Now grab the spine itself so we can also access its origin
                val spine = spineByDirection[point.point.contentHashCode()]
                if (spine != null) {
                    meanPos += spine.origin
                } else {
                    logger.warn("Could not find spine for direction: ${point.point.contentToString()}")
                }
            }
            // Calculate means by dividing by cluster size
            meanDir /= cluster.points.size.toFloat()
            meanPos /= cluster.points.size.toFloat()

            logger.debug("MeanDir for cluster is $meanDir")
            logger.debug("MeanPos for cluster is $meanPos")

            (meanPos to meanDir)
        }

        // We only need the analyzer to access the smoothing and maxima search functions
        val analyzer = HedgehogAnalysis(cleanedSpines, Matrix4f(volume.spatial().world))

        start = TimeSource.Monotonic.markNow()
        val spots = clusterCenters.map { (origin, direction) ->
            val (samples, samplePos) = sampleRayThroughVolume(origin, direction, volume)
            var spotPos: Vector3f? = null
            if (samples != null && samplePos != null) {
                val smoothed = analyzer.gaussSmoothing(samples, 4)
                val rayMax = smoothed.max()
                // take the first local maximum that is at least 20% of the global maximum to prevent spot creation in noisy areas
                analyzer.localMaxima(smoothed).firstOrNull {it.second > 0.2 * rayMax}?.let { (index, sample) ->
                    spotPos = samplePos[index]
                }
            }
            spotPos
        }
        logger.info("Sampling volume and spot extraction took ${TimeSource.Monotonic.markNow() - start}")
        spots.filterNotNull().forEach { spot ->
            spotCreateDeleteCallback?.invoke(volume.currentTimepoint, spot, cursor.radius, false, false)
        }
    }

    /** Toggles the VR rendering off, cleans up eyetracking-related scene objects and removes the light tetrahedron
     * that was created for the calibration routine. */
    override fun stop() {
        pupilTracker.unsubscribeFrames()
        logger.info("Stopped volume and hedgehog updater thread.")
        val n = sciview.find("eyeFrames")
        n?.let { sciview.deleteNode(it) }
        // Delete definitely existing objects
        listOf(referenceTarget, calibrationTarget, laser, debugBoard, hedgehogs).forEach {
            sciview.deleteNode(it)
        }
        logger.info("Successfully cleaned up eye tracking environemt.")
        super.stop()
    }

}