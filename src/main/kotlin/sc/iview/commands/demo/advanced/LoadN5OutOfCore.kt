package sc.iview.commands.demo.advanced

import bdv.util.volatiles.VolatileViews
import graphics.scenery.BoundingGrid
import graphics.scenery.PointLight
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import net.imglib2.RandomAccessibleInterval
import net.imglib2.type.volatiles.VolatileUnsignedShortType
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.janelia.saalfeldlab.n5.N5FSReader
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights

@Plugin(type = Command::class,
        label = "Load N5 Out-of-Core demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load Mandelbulb Out-of-Core demo", weight = MenuWeights.DEMO_ADVANCED)])
class LoadN5OutOfCore : Command {
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

    override fun run() {
        // Load data from N5
        val n5path = "/Users/kharrington/Library/CloudStorage/Dropbox/sciview_data/data/test_n5_error/input-multiscale.n5"
        val datasetName = "setup0/timepoint0/s0"

        log.info("Loading N5 data from $n5path with dataset $datasetName")

        val ooc: RandomAccessibleInterval<UnsignedShortType> = N5Utils.openVolatile(N5FSReader(n5path), datasetName)
        val wrapped = VolatileViews.wrapAsVolatile(ooc)

        // When loading datasets with multiple resolution levels, it's important to use Volatile types
        // here, such as VolatileUnsignedShortType, otherwise loading volume blocks will not work correctly.
        @Suppress("UNCHECKED_CAST")
        val volume = Volume.fromRAI(
                wrapped as RandomAccessibleInterval<VolatileUnsignedShortType>,
                VolatileUnsignedShortType(),
                name = "N5 OOC",
                hub = sciview.hub
        )
        volume.converterSetups.first().setDisplayRange(25.0, 512.0)
        volume.transferFunction = TransferFunction.ramp(0.01f, 0.03f)
        volume.spatial().scale = Vector3f(20.0f)
        sciview.addNode(volume)

        log.info("Volume added to the scene")

        val lights = (0 until 3).map {
            PointLight(radius = 15.0f)
        }

        lights.mapIndexed { i, light ->
            light.spatial().position = Vector3f(2.0f * i - 4.0f, i - 1.0f, 0.0f)
            light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            light.intensity = 50.0f
            sciview.addNode(light)
        }

        val newBg = BoundingGrid()
        newBg.node = volume
        sciview.publishNode(newBg)

        log.info("Lights added to the scene")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(LoadN5OutOfCore::class.java, true, argmap)
        }
    }
}