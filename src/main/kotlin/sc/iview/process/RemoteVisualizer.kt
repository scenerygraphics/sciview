package sc.iview.process

import com.fasterxml.jackson.databind.ObjectMapper
import graphics.scenery.*
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import graphics.scenery.utils.VideoDecoder
import graphics.scenery.volumes.Volume
import io.scif.services.DatasetIOService
import net.imagej.Dataset
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.array.ArrayImgs
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.joml.Vector3f
import org.joml.Vector3i
import org.msgpack.jackson.dataformat.MessagePackFactory
import org.scijava.command.Command
import org.scijava.event.EventHandler
import org.scijava.event.EventService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMQException
import sc.iview.SciView
import sc.iview.Utils
import sc.iview.commands.MenuWeights.PROCESS
import sc.iview.commands.MenuWeights.PROCESS_MESH_TO_IMAGE
import sc.iview.commands.demo.ResourceLoader
import sc.iview.event.NodeChangedEvent
import java.io.IOException
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
//    "udp://${InetAddress.getLocalHost().hostAddress}:3337"


    @Parameter
    private lateinit var logger: LogService
//
//    @Parameter
//    private lateinit var events: EventService

    private lateinit var volume: Volume

    var buffer: ByteBuffer = ByteBuffer.allocateDirect(0)

    lateinit var publisher: ZMQ.Socket

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

                val bytes = objectMapper.writeValueAsBytes(list)

                logger.info("The size of the byte array to be sent is: ${bytes.size}")

                publisher.send(bytes)
            }
        }
    }

    fun display () {
        val box = FullscreenObject()
        sciView.addNode(box)
//
//        var color = ColorRGB(0, 0, 0)
//        box.material.diffuse = Utils.convertToVector3f(color)
//
//        var cnt = 0
//
//        thread {
//            while(true) {
//                logger.info("Colour will be applied")
//                color = ColorRGB(1 * cnt, 1 * cnt, 1 * cnt)
//                box.material.diffuse = Utils.convertToVector3f(color)
//                cnt++
//                Thread.sleep(1000)
//            }
//        }

        val videoDecoder = VideoDecoder(ipAddress)

        val context: ZContext = ZContext(4)
        publisher = context.createSocket(ZMQ.PUB)

        val address: String = "tcp://0.0.0.0:6655"
        val port = try {
            publisher.bind(address)
            address.substringAfterLast(":").toInt()
        } catch (e: ZMQException) {
            logger.warn("Binding failed, trying random port: $e")
            publisher.bindToRandomPort(address.substringBeforeLast(":"))
        }

        val cam = sciView.camera as DetachedHeadCamera

        prevRot = floatArrayOf(cam.rotation.x, cam.rotation.y, cam.rotation.z, cam.rotation.w)
        prevPos = floatArrayOf(cam.position.x(), cam.position.y(), cam.position.z())

        var decodedFrameCount = 0
        thread {

            while (videoDecoder.nextFrameExists) {
                val image = videoDecoder.decodeFrame()

                if (image != null) { // image can be null, e.g. when the decoder encounters invalid information between frames
                    drawFrame(image, videoDecoder.videoWidth, videoDecoder.videoHeight, box, decodedFrameCount)
                    decodedFrameCount++
                    sendFeedback(cam, publisher)
                    Thread.sleep(10)
                }
            }

        }

        val cube: Dataset = try {
            val cubeFile = ResourceLoader.createFile(javaClass, "/cored_cube_var2_8bit.tif")
            datasetIO.open(cubeFile.absolutePath)
        } catch (exc: IOException) {
            logger.error(exc)
            return
        }
        volume = sciView.addVolume(cube, floatArrayOf(1f, 1f, 1f)) {
            pixelToWorldRatio = 10f
            name = "Volume Render Demo"
            dirty = true
            needsUpdate = true
        }

    }

    private fun drawFrame(tex: ByteArray, width: Int, height: Int, plane: FullscreenObject, frameIndex: Int) {

        if(frameIndex % 100 == 0) {
            logger.info("Displaying frame $frameIndex")
        }

        if(buffer.capacity() == 0) {
            buffer = BufferUtils.allocateByteAndPut(tex)
        } else {
            buffer.put(tex).flip()
        }
//        logger.info("displaying image")
        plane.material()
        {
            textures["diffuse"] = Texture(Vector3i(width, height, 1), 4, contents = buffer, mipmap = true)
// This is a temporary test command
        }
    }

    private fun sendFeedback(camera: DetachedHeadCamera, publisher: ZMQ.Socket) {

        val objectMapper = ObjectMapper(MessagePackFactory())

        val list: MutableList<Any> = ArrayList()
        val rotArray = floatArrayOf(camera.rotation.x, camera.rotation.y, camera.rotation.z, camera.rotation.w)
        val posArray = floatArrayOf(camera.position.x(), camera.position.y(), camera.position.z())

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