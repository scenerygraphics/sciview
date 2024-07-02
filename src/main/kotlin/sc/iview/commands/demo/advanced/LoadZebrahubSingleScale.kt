package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import net.imglib2.RandomAccessibleInterval
import net.imglib2.converter.Converter
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.util.Intervals
import net.imglib2.view.Views
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.zebrahub.ZebrahubImgLoader

@Plugin(type = Command::class,
        label = "Load Zebrahub Single Scale",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load Zebrahub Single Scale", weight = MenuWeights.DEMO_ADVANCED_FLYBRAIN)])
class LoadZebrahubSingleScale : Command {
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

    override fun run() {
        log.info("Loading Zarr dataset...")

        // URL of the remote Zarr dataset
        val zarrUrl = "https://public.czbiohub.org/royerlab/zebrahub/imaging/single-objective/ZSNS001.ome.zarr/"

        // Create Zarr ImgLoader
        val imgLoader = ZebrahubImgLoader(zarrUrl)

        // Fetch a specific chunk by specifying a region
        val chunkMin = longArrayOf(100, 250, 250) // Minimum coordinates of the chunk
        // val chunkMax = longArrayOf(99, 99, 99) // Maximum coordinates of the chunk (example values)
        val chunkMax = longArrayOf(299, 1750, 1750) // Maximum coordinates of the chunk (example values)

        // s: 448, 2174, 2423

        // Load the dataset from the first scale and crop it to the specific chunk
        val fullDataset: RandomAccessibleInterval<UnsignedShortType> = imgLoader.getSetupImgLoader(0)
                .getImage(250, 0) // Single timepoint (0) and level (0)

        // Crop the dataset to the desired chunk
        val croppedDataset = Views.interval(fullDataset, chunkMin, chunkMax)
        // val croppedDataset = fullDataset


        println("Pixel check: " + croppedDataset.getAt(50, 50, 50).get())

        // Display cropped dataset
        val dimensions = Intervals.dimensionsAsLongArray(croppedDataset)
        log.info("Cropped dataset dimensions: ${dimensions.joinToString(", ")}")

        // Apply any desired transformations (e.g., scaling)
        val transform = AffineTransform3D()
        val scaleFactor = 40.0
        transform.scale(scaleFactor, scaleFactor, scaleFactor)

        // Create the volume node
        val volume = Volume.fromRAI(
                croppedDataset,
                UnsignedShortType(), // Use UnsignedShortType directly
                name = "Zarr Dataset (Cropped)",
                hub = sciview.hub
        )

        // Apply the transfer function to the volume
        volume.transferFunction = TransferFunction.ramp(0.001f, 0.4f)
        volume.converterSetups.first().setDisplayRange(0.0, 65535.0) // Adjust range to 16-bit
        volume.spatial().position = Vector3f(0f, 0f, 0.0f)
        volume.spatial().scale = Vector3f(scaleFactor.toFloat(), scaleFactor.toFloat(), scaleFactor.toFloat())

        // Add the volume to the scene
        sciview.addNode(volume)

        log.info("Zarr dataset loaded and cropped successfully.")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(LoadZebrahubSingleScale::class.java, true, argmap)
        }
    }
}
