package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.eyetracking.PupilEyeTracker
import graphics.scenery.textures.Texture
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
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.PI
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Utilize Eye Tracker for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class EyeTrackingDemo: Command, CellTrackingBase() {

    @Parameter
    override var mastodonCallbackLinkCreate: ((HedgehogAnalysis.SpineGraphVertex) -> Unit)? = null

    @Parameter
    override var mastodonUpdateGraph: (() -> Unit)? = null

    val pupilTracker = PupilEyeTracker(calibrationType = PupilEyeTracker.CalibrationType.WorldSpace, port = System.getProperty("PupilPort", "50020").toInt())

    val calibrationTarget = Icosphere(0.02f, 2)
    val laser = Cylinder(0.005f, 0.2f, 10)

    val confidenceThreshold = 0.60f

//	var currentVolume = 0

    override fun run() {

        sciview.toggleVRRendering()
        logger.info("VR mode has been toggled")
        hmd = sciview.hub.getWorkingHMD() as? OpenVRHMD ?: throw IllegalStateException("Could not find headset")
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
            diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        calibrationTarget.name = "Calibration Target"
        sciview.camera?.addChild(calibrationTarget)

        laser.visible = false
        laser.ifMaterial{diffuse = Vector3f(1.0f, 1.0f, 1.0f)}
        laser.name = "Laser"
        sciview.addNode(laser)

        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }

        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        shell.name = "Shell"
        sciview.addNode(shell)

        val volnodes = sciview.findNodes { node -> Volume::class.java.isAssignableFrom(node.javaClass) }

        val v = (volnodes.firstOrNull() as? Volume)
        if(v == null) {
            logger.warn("No volume found, bailing")
            return
        } else {
            logger.info("found ${volnodes.size} volume nodes. Using the first one: ${volnodes.first()}")
            volume = v
        }
        volume.visible = false

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
        val debugBoard = TextBoard()
        debugBoard.name = "debugBoard"
        debugBoard.scale = Vector3f(0.05f, 0.05f, 0.05f)
        debugBoard.position = Vector3f(0.0f, -0.3f, -0.9f)
        debugBoard.text = ""
        debugBoard.visible = false
        sciview.camera?.addChild(debugBoard)

        val lights = Light.createLightTetrahedron<PointLight>(Vector3f(0.0f, 0.0f, 0.0f), spread = 5.0f, radius = 15.0f, intensity = 5.0f)
        lights.forEach { sciview.addNode(it) }

        thread {
            logger.info("Adding onDeviceConnect handlers")
            hmd.events.onDeviceConnect.add { hmd, device, timestamp ->
                logger.info("onDeviceConnect called, cam=${sciview.camera}")
                if(device.type == TrackedDeviceType.Controller) {
                    logger.info("Got device ${device.name} at $timestamp")
                    device.model?.let { hmd.attachToNode(device, it, sciview.camera) }
                }
            }
        }
        thread{
            logger.info("started thread for inputSetup")
            inputSetup()
            setupCalibration()
        }

        launchHedgehogThread()
    }


    private fun setupCalibration(keybindingCalibration: String = "N", keybindingTracking: String = "U") {
        val startCalibration = ClickBehaviour { _, _ ->
            thread {
                val cam = sciview.camera as? DetachedHeadCamera ?: return@thread
                pupilTracker.gazeConfidenceThreshold = confidenceThreshold
                if (!pupilTracker.isCalibrated) {
                    logger.info("pupil is currently uncalibrated")
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
                                logger.info("deactivating tracking...")
                                referenceTarget.ifMaterial { diffuse = Vector3f(0.5f, 0.5f, 0.5f) }
                                cam.showMessage("Tracking deactivated.",distance = 2f, size = 0.2f, centered = true)
                                dumpHedgehog()
                            } else {
                                logger.info("activating tracking...")
                                addHedgehog()
                                referenceTarget.ifMaterial { diffuse = Vector3f(1.0f, 0.0f, 0.0f) }
                                cam.showMessage("Tracking active.",distance = 2f, size = 0.2f, centered = true)
                            }
                            tracking = !tracking
                        }
                        hmd.addBehaviour("toggle_tracking", toggleTracking)
                        hmd.addKeyBinding("toggle_tracking", keybindingTracking)

                        volume.visible = true
//                        volume.runRecursive { it.visible = true }
                        playing = true
                    }

                    pupilTracker.unsubscribeFrames()
                    sciview.deleteNode(sciview.find("eyeFrames"))

                    logger.info("Starting eye tracker calibration")
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
                                referenceTarget.spatial().position = p
                                (cam.children.find { it.name == "debugBoard" } as? TextBoard)?.text = "${String.format("%.2f", p.x())}, ${String.format("%.2f", p.y())}, ${String.format("%.2f", p.z())}"

                                val headCenter = cam.spatial().viewportToWorld(Vector2f(0.0f, 0.0f))
                                val pointWorld = Matrix4f(cam.spatial().world).transform(p.xyzw()).xyz()
                                val direction = (pointWorld - headCenter).normalize()

                                if (tracking) {
//                                    log.info("Starting spine from $headCenter to $pointWorld")
                                    addSpine(headCenter, direction, volume, gaze.confidence, volume.viewerState.currentTimepoint)
                                }
                            }
                        }

//                        else -> {gaze-> }
                    }

                    logger.info("Calibration routine done.")
                }

                // bind calibration start to menu key on controller

            }
        }
        hmd.addBehaviour("start_calibration", startCalibration)
        hmd.addKeyBinding("start_calibration", keybindingCalibration)
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