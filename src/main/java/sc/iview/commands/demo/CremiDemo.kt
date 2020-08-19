package sc.iview.commands.demo

import bdv.util.DefaultInterpolators
import bdv.viewer.Interpolation
import ch.systemsx.cisd.hdf5.HDF5Factory
import graphics.scenery.Material
import graphics.scenery.Origin
import graphics.scenery.numerics.Random
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Colormap.Companion.fromColorTable
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import net.imagej.lut.LUTService
import net.imagej.mesh.Mesh
import net.imagej.ops.OpService
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.array.ArrayImgs
import net.imglib2.roi.labeling.ImgLabeling
import net.imglib2.roi.labeling.LabelRegions
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedLongType
import net.imglib2.view.Views
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.widget.FileWidget
import sc.iview.SciViewService
import sc.iview.commands.MenuWeights
import sc.iview.process.MeshConverter
import java.io.FileFilter
import java.io.IOException

typealias NeuronsAndImage = Triple<HashMap<Long, Long>, RandomAccessibleInterval<UnsignedLongType>, RandomAccessibleInterval<UnsignedByteType>>

@Plugin(type = Command::class, label = "Cremi Dataset rendering demo", menuRoot = "SciView", menu = [Menu(label = "Demo", weight = MenuWeights.DEMO), Menu(label = "Cremi", weight = MenuWeights.DEMO_VOLUME_RENDER)])
class CremiDemo : Command {
    @Parameter
    private lateinit var ui: UIService

    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciview: SciViewService

    @Parameter
    private lateinit var lut: LUTService

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
        val filter = FileFilter { file ->
            val extension = file.name.substringAfterLast(".").toLowerCase()

            extension == "hdf5" || extension == "hdf"
        }
        val files = ui.chooseFiles(null, emptyList(), filter, FileWidget.OPEN_STYLE)

        val nai = readCremiHDF5(files.first().canonicalPath, 0.5)

        if(nai == null) {
            log.error("Could not get neuron IDs")
            return
        }
        val raiVolume = nai.third
        val cursor = Views.iterable(raiVolume).localizingCursor()
        while(cursor.hasNext() && cursor.getIntPosition(2) < 50) {
            cursor.fwd()
            cursor.get().set(0)
        }

        val volume = sciview.activeSciView.addVolume(nai.third, files.first().name) as? Volume
        volume?.origin = Origin.FrontBottomLeft
        volume?.scale = Vector3f(0.04f, 0.04f, 2.5f)
        volume?.transferFunction = TransferFunction.ramp(0.3f, 0.1f, 0.1f)
        // min 20, max 180, color map fire
        volume?.converterSetups?.get(0)?.setDisplayRange(20.0, 180.0)
        val colormap = lut.loadLUT(lut.findLUTs().get("Fire.lut"))
        volume?.colormap = Colormap.fromColorTable(colormap)

        val rai = nai.second
        log.info("Got ${nai.first.size} labels")

        // let's extract some neurons here
        log.info("Creating labeling ...")

        val labels = (0 .. (nai.first.keys.maxOrNull()?.toInt() ?: 1)).toList()
        val labeling = ImgLabeling.fromImageAndLabels(rai, labels)
        log.info("Creating regions...")
        val regions = LabelRegions(labeling)
        log.info("Created ${regions.count()} regions")

        val largestNeuronLabels = nai.first.entries.sortedByDescending { p -> p.value }.take(10).map { kv -> kv.key }

        log.info("Largest neuron labels are ${largestNeuronLabels.joinToString(",")}")

        regions.filter { largestNeuronLabels.contains(it.label.toLong() + 1L) }.forEachIndexed { i, region ->
            log.info("Meshing neuron ${i + 1}/${largestNeuronLabels.size} with label ${region.label}...")
            // ui.show(region)
            // Generate the mesh with imagej-ops
            val m: Mesh = ops.geom().marchingCubes(region, 1.0, BitTypeVertexInterpolator())

            log.info("Converting neuron ${i + 1}/${largestNeuronLabels.size} to scenery format...")
            // Convert the mesh into a scenery mesh for visualization
            val mesh = MeshConverter.toScenery(m, false)
            mesh.scale = Vector3f(0.01f, 0.01f, 0.06f)
            mesh.material.diffuse = colormap.lookupARGB(0.0, 255.0, kotlin.random.Random.nextDouble(0.0, 255.0)).toRGBColor().xyz()
            mesh.material.roughness = 0.0f

            // marching cubes produces CW meshes, not CCW as expected by default
            mesh.material.cullingMode = Material.CullingMode.Front
            mesh.name = "Neuron $i"
            sciview.activeSciView.addNode(mesh)
        }
    }

    fun readCremiHDF5(path: String, scale: Double = 1.0): NeuronsAndImage? {
        log.info("Reading cremi HDF5 from $path")
        val hdf5Reader = HDF5Factory.openForReading(path)
        val n5Reader: N5HDF5Reader
        try {
            n5Reader = N5HDF5Reader(hdf5Reader, *intArrayOf(128, 128, 128))
            val neuronIds: RandomAccessibleInterval<UnsignedLongType> = N5Utils.open(n5Reader,
                    "/volumes/labels/neuron_ids",
                    UnsignedLongType())
            val volume: RandomAccessibleInterval<UnsignedByteType> = N5Utils.open(n5Reader,
                    "/volumes/raw",
                    UnsignedByteType())
            val img = ArrayImgs.unsignedLongs(*neuronIds.dimensionsAsLongArray())
            val cursor = Views.iterable(neuronIds).localizingCursor()
            val dest = img.randomAccess();
            val neurons = HashMap<Long, Long>()
            while( cursor.hasNext() ) {
                cursor.fwd();
                dest.setPosition(cursor);
                val value = cursor.get()
                dest.get().set(value);
                neurons[value.get()] = (neurons[value.get()] ?: 0) + 1
            }

            log.info("Got RAI from HDF5, dimensions ${img.dimensionsAsLongArray().joinToString(",")}")

            return if(scale != 1.0) {
                Triple(neurons, ops.transform().scaleView(img, doubleArrayOf(scale, scale, scale), DefaultInterpolators<UnsignedLongType>().get(Interpolation.NEARESTNEIGHBOR)), volume)
            } else {
                Triple(neurons, img, volume)
            }
        } catch (e: IOException) {
            log.error("Could not read Cremi HDF5 file from $path")
            e.printStackTrace()
        }

        return null
    }

    private fun Int.toRGBColor(): Vector4f {
        val a = ARGBType.alpha(this)/255.0f
        val r = ARGBType.red(this)/255.0f
        val g = ARGBType.green(this)/255.0f
        val b = ARGBType.blue(this)/255.0f

        return Vector4f(r, g, b, a)
    }
}