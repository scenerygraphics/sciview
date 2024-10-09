package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.TrackerRole
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.volumes.Volume
import org.joml.*
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap
import kotlin.concurrent.thread
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.behaviours.*
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.extensions.*
import org.scijava.event.EventService
import sc.iview.commands.file.OpenDirofTif
import sc.iview.event.NodeTaggedEvent

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Utilize VR Headset for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class VRHeadSetTrackingDemo: Command, CellTrackingBase() {

    @Parameter
    private lateinit var eventService: EventService

    var hedgehogsList =  mutableListOf<InstancedNode>()

    private var selectionStorage: Node? = null

//	var currentVolume = 0

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

//        val testtarget = Icosphere(2f, 2)
//        volume.addChild(testtarget)
//        testtarget.addAttribute(Grabable::class.java,Grabable())
//        testtarget.addAttribute(Selectable::class.java, Selectable(onSelect = {selectionStorage = testtarget}))

        val bb = BoundingGrid()
        bb.node = volume
        bb.visible = false

        val debugBoard = TextBoard()
        debugBoard.name = "debugBoard"
        debugBoard.spatial().scale = Vector3f(0.05f, 0.05f, 0.05f)
        debugBoard.spatial().position = Vector3f(0.0f, -0.3f, -0.9f)
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
            setupHeadsetTracking()
        }

        launchHedgehogThread()
    }

    private fun setupHeadsetTracking() {
        //VRGrab.createAndSet(scene = Scene(), hmd, listOf(OpenVRHMD.OpenVRButton.Trigger), listOf(TrackerRole.LeftHand))
        //left trigger button can validate or delete a track, the function should be arranged to two different button in the future
        VRSelect.createAndSet(sciview.currentScene,
            hmd,
            listOf(OpenVRHMD.OpenVRButton.Trigger),
            listOf(TrackerRole.LeftHand),
            { n ->
                println("the spot ${n.name} is selected")

                /**
                 * delete the selected node from volume
                 **/
//                volume.runRecursive{it.removeChild(n)}
//                eventService.publish(NodeRemovedEvent(n))


                /*
                    validate the selected node from volume, the tag event is designed specially for tag of Elephant
                 */
                eventService.publish(NodeTaggedEvent(n))

            },
            true)


        VRTouch.createAndSet(sciview.currentScene,hmd, listOf(TrackerRole.LeftHand,TrackerRole.RightHand),true)

        VRGrab.createAndSet(sciview.currentScene,hmd, listOf(OpenVRHMD.OpenVRButton.Side), listOf(TrackerRole.RightHand,TrackerRole.LeftHand))
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
                        thread {
                            dumpHedgehog()
                            println("before dumphedgehog: " + hedgehogsList.last().instances.size.toString())
                        }
                    } else {
                        addHedgehog()
                        println("after addhedgehog: "+ hedgehogsList.last().instances.size.toString())
                        referenceTarget.ifMaterial { diffuse = Vector3f(1.0f, 0.0f, 0.0f) }
                        cam.showMessage("Tracking active.",distance = 1.2f, size = 0.2f)
                        tracking = true
                    }
                }
                //RightController.trigger
                hmd.addBehaviour("toggle_tracking", toggleTracking)
                hmd.addKeyBinding("toggle_tracking", keybindingTracking)

                volume.visible = true
                volume.runRecursive { it.visible = true }

//                playing = false

                while(true)
                {

                    val headCenter = Matrix4f(cam.spatial().world).transform(Vector3f(0.0f,0f,-1f).xyzw()).xyz()
                    val pointWorld = Matrix4f(cam.spatial().world).transform(Vector3f(0.0f,0f,-2f).xyzw()).xyz()

                    referenceTarget.visible = true
                    referenceTarget.ifSpatial { position = Vector3f(0.0f,0f,-1f) }

                    val direction = (pointWorld - headCenter).normalize()
                    if (tracking) {
                        addSpine(headCenter, direction, volume,0.8f, volume.viewerState.currentTimepoint)
                    }

                    Thread.sleep(2)
                }
            }

    }

    companion object {
        //run function from here, it will automatically choose the volume for rendering, please give the correct location of volume
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)

            command.run(OpenDirofTif::class.java, true,
                hashMapOf<String,Any>(
                    "file" to File("E:\\dataset\\Pdu_H2BeGFP_CAAXmCherry_0123_20130312_192018.corrected-histone"),
                    "onlyFirst" to 10
                ))
                .get()

            val argmap = HashMap<String, Any>()
            command.run(VRHeadSetTrackingDemo::class.java, true, argmap)
                .get()
        }
    }
}