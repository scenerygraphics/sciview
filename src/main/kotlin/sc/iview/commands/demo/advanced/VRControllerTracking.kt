package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDevice
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Volume
import org.joml.Matrix4f
import org.joml.Vector3f
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread


/**
 * This class utilizes VR controllers to track cells in volumetric datasets in a sciview environment.
 */
class VRControllerTracking(
   sciview: SciView
): CellTrackingBase(sciview) {

    val testTarget1 = Icosphere(0.01f, 2)
    val testTarget2 = Icosphere(0.04f, 2)
    val laser = Cylinder(0.0025f, 1f, 20)

    lateinit var rightController: TrackedDevice

    var hedgehogsList =  mutableListOf<InstancedNode>()

    fun run() {

        sciview.toggleVRRendering()
        hmd = sciview.hub.getWorkingHMD() as? OpenVRHMD ?: throw IllegalStateException("Could not find headset")

        sessionId = "BionicTracking-generated-${SystemHelpers.formatDateTime()}"
        sessionDirectory = Files.createDirectory(Paths.get(System.getProperty("user.home"), "Desktop", sessionId))

        laser.material().diffuse = Vector3f(5.0f, 0.0f, 0.02f)
        laser.material().metallic = 0.0f
        laser.material().roughness = 1.0f
        laser.visible = false
        sciview.addNode(laser)

        referenceTarget.visible = false
        referenceTarget.ifMaterial{
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(0.8f, 0.8f, 0.8f)
        }
        sciview.addNode(referenceTarget)

        testTarget1.visible = false
        testTarget1.ifMaterial{
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(0.8f, 0.8f, 0.8f)
        }
        sciview.addNode(testTarget1)


        testTarget2.visible = false
        testTarget2.ifMaterial{
            roughness = 1.0f
            metallic = 0.0f
            diffuse = Vector3f(0.8f, 0.8f, 0.8f)
        }
        sciview.addNode(testTarget2)

        val shell = Box(Vector3f(20.0f, 20.0f, 20.0f), insideNormals = true)
        shell.ifMaterial{
            cullingMode = Material.CullingMode.Front
            diffuse = Vector3f(0.4f, 0.4f, 0.4f) }

        shell.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        sciview.addChild(shell)

        volume = sciview.find("volume") as Volume

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
            inputSetup()
            setupControllerforTracking()
        }

        launchUpdaterThread()
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
                /**
                 * the following code is added to detect right controller
                 */
                if(!hmd.getTrackedDevices(TrackedDeviceType.Controller).containsKey("Controller-2"))
                {
                    continue
                }
                else
                {
                    rightController = hmd.getTrackedDevices(TrackedDeviceType.Controller).get("Controller-2")!!

                    if (rightController.model?.spatialOrNull() == null) {
                        //println("spatial null")
                    }
                    else
                    {
                        val headCenter = Matrix4f(rightController.model?.spatialOrNull()?.world).transform(Vector3f(0.0f,0f,-0.1f).xyzw()).xyz()
                        val pointWorld = Matrix4f(rightController.model?.spatialOrNull()?.world).transform(Vector3f(0.0f,0f,-2f).xyzw()).xyz()

                        println(headCenter.toString())
                        println(pointWorld.toString())
                        testTarget1.visible = true
                        testTarget1.ifSpatial { position =  headCenter}

                        testTarget2.visible = true
                        testTarget2.ifSpatial { position =  pointWorld}

                        laser.visible = true
                        laser.spatial().orientBetweenPoints(headCenter, pointWorld,true,true)

                        referenceTarget.visible = true
                        referenceTarget.ifSpatial { position =  pointWorld}

                        val direction = (pointWorld - headCenter).normalize()
                        if (tracking) {
                            addSpine(headCenter, direction, volume,0.8f, volume.viewerState.currentTimepoint)
                        }
                    }
                }
            }
        }
    }
}