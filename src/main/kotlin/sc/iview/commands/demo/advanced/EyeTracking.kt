package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.controls.OpenVRHMD.OpenVRButton
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.eyetracking.PupilEyeTracker
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import graphics.scenery.textures.Texture
import graphics.scenery.ui.Button
import graphics.scenery.ui.Column
import graphics.scenery.ui.ToggleButton
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.joml.*
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.PI

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

    }


    private fun setupEyeTracking(
        keybindingCalibration: Pair<TrackerRole, OpenVRButton> = (TrackerRole.RightHand to OpenVRButton.Menu)
    ) {
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
                dumpHedgehog()
                playing = false
            } else {
                logger.info("activating tracking...")
                playing = false
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
        leftEyeTrackColumn = createWristMenuColumn(toggleHedgehogsBtn, calibrateButton, name = "Eye Tracking Menu")
        leftEyeTrackColumn?.visible = false
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