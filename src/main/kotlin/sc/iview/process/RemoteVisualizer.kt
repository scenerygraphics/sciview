package sc.iview.process

import com.fasterxml.jackson.databind.ObjectMapper
import graphics.scenery.*
import graphics.scenery.textures.Texture
import graphics.scenery.utils.VideoDecoder
import graphics.scenery.volumes.DummyVolume
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import io.scif.services.DatasetIOService
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3i
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.scijava.command.Command
import org.scijava.event.EventHandler
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.zeromq.ZMQ
import sc.iview.SciView
import sc.iview.commands.MenuWeights.PROCESS
import sc.iview.commands.MenuWeights.PROCESS_MESH_TO_IMAGE
import sc.iview.event.NodeChangedEvent
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.concurrent.thread

@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Process", weight = PROCESS), Menu(label = "Visualize remote simulation", weight = PROCESS_MESH_TO_IMAGE)]
)
class RemoteVisualizer: Command {

    @Parameter
    private lateinit var sciView: SciView
    @Parameter
    private lateinit var datasetIO: DatasetIOService
    @Parameter(label = "IP Address")
    private  var ipAddress: String =
        "/home/aryaman/Repositories/scenery/src/test/resources/graphics/scenery/tests/examples/advanced/ViC_movie_600.mp4"
    @Parameter
    @Transient
    private lateinit var logger: LogService

    private lateinit var volume: Volume

    var buffer: ByteBuffer = ByteBuffer.allocateDirect(0)

    lateinit var publisher: ZMQ.Socket

    private val remoteRendering: Boolean = true
    private var decodedFrameCount: Int = 0

    var prevRot: FloatArray = FloatArray(4)
    var prevPos: FloatArray = FloatArray(3)

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeChanged(event: NodeChangedEvent?) {
        if (event != null) {
            if (event.node == volume) {
                logger.info(volume.transferFunction)
                val objectMapper = ObjectMapper(MessagePackFactory())

                val list: MutableList<Any> = ArrayList()
                val tmp = "transfer_fn"
                list.add(tmp)

                //val bytes = objectMapper.writeValueAsBytes(list)

                //logger.info("The size of the byte array to be sent is: ${bytes.size}")

                //publisher.send(bytes)
            }
        }
    }

    fun display () {

        val light = PointLight(radius = 15.0f)
        with(light) {
            spatial() {
                position = Vector3f(0.0f, 0.0f, 2.0f)
            }
            intensity = 5.0f
            emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            sciView.addNode(this)
        }

        val billBoard = Billboard(Vector2f(2.0f, 2.0f), Vector3f(0.0f, 0.0f, 0.0f), true)
        with(billBoard)
        {
            wantsSync = false
            material {
                //textures["diffuse"] = Texture.fromImage(Image.fromResource("textures/helix.png", TexturedCubeExample::class.java))
            }
            sciView.addNode(this)
        }

        val dummyVolume = DummyVolume()
        with(dummyVolume) {
            name = "DummyVolume"
            transferFunction = TransferFunction.ramp(0.001f, 0.5f, 0.3f)
            sciView.addNode(this)
        }

        val cam: Camera = DetachedHeadCamera()
        with(cam) {
            name = "ClientCamera"
            spatial {
                position = Vector3f(0.0f, 0.0f, 5.0f)
            }
            perspectiveCamera(70.0f, 512, 512)
            wantsSync = true
            sciView.addNode(this)
        }

        // the videoDecoder should only be created if a client connects to a server)

        if(remoteRendering) {
            val videoDecoder = VideoDecoder("udp://${InetAddress.getLocalHost().hostAddress}:3337")
            logger.info("video decoder object created")

            thread {
                while (!sciView.sceneInitialized()) {
                    Thread.sleep(200)
                }
                decodedFrameCount = 1

                while (videoDecoder.nextFrameExists) {
                    val image = videoDecoder.decodeFrame()  /* the decoded image is returned as a ByteArray, and can now be processed.
                                                            Here, it is simply displayed in fullscreen */
                    if (image != null) { // image can be null, e.g. when the decoder encounters invalid information between frames
                        drawFrame(
                            image,
                            videoDecoder.videoWidth,
                            videoDecoder.videoHeight,
                            billBoard,
                            decodedFrameCount
                        )
                    }
                }
            }
        }

        // transfer function manipulation
        thread {
            while (sciView.running) {
                dummyVolume.transferFunction = TransferFunction.ramp(0.001f + (dummyVolume.counter++%1000)/1000.0f, 0.5f, 0.3f)
                Thread.sleep(20)
            }
        }
    }

    private fun drawFrame(tex: ByteArray, width: Int, height: Int, plane: Billboard, frameIndex: Int) {

        if(frameIndex % 100 == 0) {
            logger.info("Displaying frame $frameIndex")
        }

        if(buffer.capacity() == 0) {
            buffer = BufferUtils.allocateByteAndPut(tex)
        } else {
            buffer.put(tex).flip()
        }
        plane.material()
        {
            textures["diffuse"] = Texture(Vector3i(width, height, 1), 4, contents = buffer, mipmap = true)
        }
    }

    private fun sendFeedback(camera: DetachedHeadCamera, publisher: ZMQ.Socket) {

        val objectMapper = ObjectMapper(MessagePackFactory())

        val list: MutableList<Any> = ArrayList()
        val rotArray = floatArrayOf(camera.spatial().rotation.x, camera.spatial().rotation.y, camera.spatial().rotation.z, camera.spatial().rotation.w)
        val posArray = floatArrayOf(camera.spatial().position.x(), camera.spatial().position.y(), camera.spatial().position.z())

        if(!((rotArray.contentEquals(prevRot)) && (posArray.contentEquals(prevPos)))) {
            list.add(rotArray)
            list.add(posArray)

            val bytes = objectMapper.writeValueAsBytes(list)

            publisher.send(bytes)
//            println("The size of the byte array to be sent is: ${bytes.size}")
            prevPos = posArray
            prevRot = rotArray
        } else {
//            logger.info("Not sending camera since there has been no change")
        }
    }

    override fun run() {
        display()
    }
}