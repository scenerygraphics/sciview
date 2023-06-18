package sc.iview.commands.demo.advanced

import bdv.cache.SharedQueue
import bdv.util.volatiles.VolatileTypeMatcher
import bdv.util.volatiles.VolatileViews
import net.imagej.lut.LUTService
import net.imagej.ops.OpService
import net.imglib2.RandomAccessibleInterval
import net.imglib2.Volatile
import net.imglib2.cache.img.CachedCellImg
import net.imglib2.cache.volatiles.CacheHints
import net.imglib2.cache.volatiles.LoadingStrategy
import net.imglib2.type.NativeType
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.Views
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer
import org.janelia.saalfeldlab.n5.N5Reader
import org.janelia.saalfeldlab.n5.bdv.MultiscaleDatasets
import org.janelia.saalfeldlab.n5.bdv.N5Source
import org.janelia.saalfeldlab.n5.ij.N5Factory
import org.janelia.saalfeldlab.n5.ij.N5Importer
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import org.janelia.saalfeldlab.n5.metadata.*
import org.joml.Vector4f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.io.File
import java.util.*
import java.util.concurrent.Executors


@Plugin(type = Command::class,
        label = "OpenOrganelle demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "OpenOrganelle Demo", weight = MenuWeights.DEMO_ADVANCED_OPENORGANELLE)])
class OpenOrganelle : Command {
    @Parameter
    private lateinit var ui: UIService

    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var lut: LUTService

    /*
     * TODO
     *
     * https://openorganelle.janelia.org/datasets/jrc_mus-kidney
     * s3://janelia-cosem-datasets/jrc_mus-kidney/jrc_mus-kidney.n5
     *
     */

    internal class DefaultLabelIterator : MutableIterator<Long> {
        private var i = 0L
        override fun hasNext(): Boolean {
            return i < Long.MAX_VALUE
        }

        override fun next(): Long {
            return i++
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    /**
     * When an object implementing interface `Runnable` is used
     * to create a thread, starting the thread causes the object's
     * `run` method to be called in that separately executing
     * thread.
     *
     *
     * The general contract of the method `run` is that it may
     * take any action whatsoever.
     *
     * @see Thread.run
     */

    override fun run() {
        val task = sciview.taskManager.newTask("OpenOrganelle", "Loading dataset")

        val file = File("s3://janelia-cosem-datasets/jrc_mus-kidney/jrc_mus-kidney.n5")

        // Read the EM volume
        task.status = "Reading image volume"
        //val nai = readMultiscaleN5(file.canonicalPath, "em/fibsem-uint8")
        log.info("Start Reading")
        val nai = readMultiscaleN5()

        log.info("Done Reading")

        if (nai == null) {
            log.error("Could not read data")
            return
        }
//        val raiVolume = nai.third
//        val cursor = Views.iterable(raiVolume).localizingCursor()
//        while (cursor.hasNext() && cursor.getIntPosition(2) < 50) {
//            cursor.fwd()
//            cursor.get().set(0)
//        }
//
        val colormapVolume = lut.loadLUT(lut.findLUTs().get("Grays.lut"))
//        val colormapNeurons = lut.loadLUT(lut.findLUTs().get("Fire.lut"))

//        sciview.addVolume(nai) {
//            origin = Origin.FrontBottomLeft
//            this.spatialOrNull()?.scale = Vector3f(0.08f, 0.08f, 5.0f)
//            transferFunction = TransferFunction.ramp(0.3f, 0.1f, 0.1f)
//            // min 20, max 180, color map fire
//
//            transferFunction.addControlPoint(0.3f, 0.5f)
//            transferFunction.addControlPoint(0.8f, 0.01f)
//            converterSetups.get(0).setDisplayRange(20.0, 220.0)
//            colormap = Colormap.fromColorTable(colormapVolume)
//        }

        // Read the labels volume
        task.status = "Reading labels volume"
        val labels = readMultiscaleN5()

        task.status = "Creating labeling"
        task.completion = 10.0f
//        val rai = nai.second
//        log.info("Got ${nai.first.size} labels")

        // let's extract some neurons here
        log.info("Creating labeling ...")

        //val labels = (0..(nai.first.keys.maxOrNull()?.toInt() ?: 1)).toList()
//        val labeling = ImgLabeling.fromImageAndLabels(rai, labels)
//        log.info("Creating regions...")
//        val regions = LabelRegions(labeling)
//        log.info("Created ${regions.count()} regions")
//
//        val largestNeuronLabels = nai.first.entries.sortedByDescending { p -> p.value }.take(50).shuffled().take(10).map { kv -> kv.key }
//
//        log.info("Largest neuron labels are ${largestNeuronLabels.joinToString(",")}")
//
//        regions.filter { largestNeuronLabels.contains(it.label.toLong() + 1L) }.forEachIndexed { i, region ->
//            log.info("Meshing neuron ${i + 1}/${largestNeuronLabels.size} with label ${region.label}...")
//            task.status = "Meshing neuron ${i + 1}/${largestNeuronLabels.size}"
//
//            // ui.show(region)
//            // Generate the mesh with imagej-ops
//            val m: Mesh = Meshes.marchingCubes(region);
//
//            log.info("Converting neuron ${i + 1}/${largestNeuronLabels.size} to scenery format...")
//            // Convert the mesh into a scenery mesh for visualization
//            val mesh = MeshConverter.toScenery(m, false, flipWindingOrder = true)
//            sciview.addNode(mesh) {
//                spatial().scale = Vector3f(0.01f, 0.01f, 0.06f)
//                ifMaterial {
//                    diffuse =
//                            colormapNeurons.lookupARGB(0.0, 255.0, kotlin.random.Random.nextDouble(0.0, 255.0)).toRGBColor()
//                                    .xyz()
//                    roughness = 0.0f
//                }
//                name = "Neuron $i"
//            }
//            val completion = 10.0f + ((i + 1) / largestNeuronLabels.size.toFloat()) * 90.0f
//            task.completion = completion
//        }
//
//        task.completion = 100.0f
    }



    fun readMultiscaleN5(n5root: String = "s3://janelia-cosem-datasets/jrc_mus-kidney/jrc_mus-kidney.n5",
                         multiscaleBaseDataset: String = "/labels/empanada-mito_seg",
                         scale: Double = 1.0): CachedCellImg<out NativeType<*>, *>? {
        // set up the n5 reader

        log.info("Opening reader")
        var n5: N5Reader = N5Factory().openReader(n5root)
        // parse the metadata so bdv knows how to display the scales
        val importers = Arrays.asList<N5MetadataParser<*>>(*N5Importer.PARSERS)
        val groupParsers = Arrays.asList<N5MetadataParser<*>>(*N5Importer.GROUP_PARSERS)
        log.info("Dataset discoverer")
        val parsers = N5DatasetDiscoverer(
                n5,
                Executors.newSingleThreadExecutor(),
                importers,
                groupParsers
        )

        log.info("Dataset discoverer done")

        // get the particular metadata we care above
        var root = parsers.discoverAndParseRecursive("")
        val multiscaleMetadata = root.childrenList()[0].childrenList()[0].metadata

        var datasetToOpen = multiscaleMetadata.path

        log.info("Opening image")

        val multiScaleDataset = multiscaleMetadata as N5CosemMultiScaleMetadata
        val msd = MultiscaleDatasets.sort(multiScaleDataset.paths, multiScaleDataset.spatialTransforms3d())
        var datasetsToOpen = msd.paths
        var transforms = msd.transforms

        var sharedQueue = SharedQueue(Math.max(1, Runtime.getRuntime().availableProcessors() / 2))

        // Open the images
        val images: Array<RandomAccessibleInterval<UnsignedByteType>?> = arrayOfNulls<RandomAccessibleInterval<UnsignedByteType>>(multiScaleDataset.paths.size)
        for (s in images.indices) {
            val vimg = N5Utils.openVolatile(n5, datasetsToOpen[s]) as RandomAccessibleInterval<UnsignedByteType>
            if (vimg.numDimensions() == 2) {
                images[s] = Views.addDimension(vimg, 0, 0)
            } else {
                images[s] = vimg
            }
        }

        // Open the images as volatiles
        val vimages = arrayOfNulls<RandomAccessibleInterval<Volatile<UnsignedByteType>>>(images.size)
        for (s in 0 until images.size) {
            val cacheHints = CacheHints(LoadingStrategy.VOLATILE, 0, true)
            vimages[s] = VolatileViews.wrapAsVolatile<UnsignedByteType, Volatile<UnsignedByteType>>(images[s], sharedQueue as bdv.util.volatiles.SharedQueue?, cacheHints)
        }

        // Make a source
        val source: N5Source<UnsignedByteType> = N5Source<UnsignedByteType>(
                UnsignedByteType(),
                "OpenOrganelle",
                images,
                transforms)

        // Make a volatile source
//        var volatileType = VolatileTypeMatcher.getVolatileTypeForType(UnsignedByteType())
//        val volatileSource = N5Source<UnsignedByteType>(
//                volatileType,
//                "Volatile OpenOrganelle",
//                vimages,
//                transforms)

        //var img = N5Utils.openVolatile(n5, datasetToOpen)

        log.info("Image opened")

        // run n5 viewer
        //N5Viewer.exec(new DataSelection( n5, Collections.singletonList( multiscaleMetadata )));


        return null
    }


    private fun Int.toRGBColor(): Vector4f {
        val a = ARGBType.alpha(this) / 255.0f
        val r = ARGBType.red(this) / 255.0f
        val g = ARGBType.green(this) / 255.0f
        val b = ARGBType.blue(this) / 255.0f

        return Vector4f(r, g, b, a)
    }


    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(OpenOrganelle::class.java, true, argmap)
        }
    }
}