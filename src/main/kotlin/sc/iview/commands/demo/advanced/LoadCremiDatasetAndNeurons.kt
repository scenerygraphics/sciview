/*-
 * #%L
 * sciview 3D visualization tool.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.commands.demo.advanced

import bdv.util.DefaultInterpolators
import bdv.viewer.Interpolation
import ch.systemsx.cisd.hdf5.HDF5Factory
import dev.dirs.ProjectDirectories
import graphics.scenery.Origin
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import net.imagej.lut.LUTService
import net.imagej.mesh.Mesh
import net.imagej.mesh.Meshes
import net.imagej.ops.OpService
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.array.ArrayImgs
import net.imglib2.roi.labeling.ImgLabeling
import net.imglib2.roi.labeling.LabelRegions
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedLongType
import net.imglib2.view.Views
import org.apache.commons.io.FileUtils.copyURLToFile
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
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.process.MeshConverter
import java.io.FileFilter
import java.io.IOException
import java.net.URL
import java.io.File


typealias NeuronsAndImage = Triple<HashMap<Long, Long>, RandomAccessibleInterval<UnsignedLongType>, RandomAccessibleInterval<UnsignedByteType>>

@Plugin(type = Command::class,
        label = "Cremi Dataset rendering demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load Cremi dataset and neuron labels", weight = MenuWeights.DEMO_ADVANCED_CREMI)])
class LoadCremiDatasetAndNeurons : Command {
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
        val task = sciview.taskManager.newTask("Cremi", "Loading dataset")
        val filter = FileFilter { file ->
            val extension = file.name.substringAfterLast(".").toLowerCase()

            extension == "hdf5" || extension == "hdf"
        }

        // val files = ui.chooseFiles(null, emptyList(), filter, FileWidget.OPEN_STYLE)
        var projDirs = sciview.getProjectDirectories()

        val file = File(projDirs.cacheDir,"sample_A_20160501.hdf")
        if (file.exists() == false) {
            task.status = "Downloading dataset"
            log.info("Downloading dataset")
            // ensure this exists projDirs.cacheDir
            if (!File(projDirs.cacheDir).exists()) {
                File(projDirs.cacheDir).mkdirs()
            }
            copyURLToFile(URL("https://cremi.org/static/data/sample_A_20160501.hdf"), file)
        }

        task.status = "Reading dataset"
        val nai = readCremiHDF5(file.canonicalPath, 1.0)

        if (nai == null) {
            log.error("Could not get neuron IDs")
            return
        }
        val raiVolume = nai.third
        val cursor = Views.iterable(raiVolume).localizingCursor()
        while (cursor.hasNext() && cursor.getIntPosition(2) < 50) {
            cursor.fwd()
            cursor.get().set(0)
        }

        val colormapVolume = lut.loadLUT(lut.findLUTs().get("Grays.lut"))
        val colormapNeurons = lut.loadLUT(lut.findLUTs().get("Fire.lut"))

        val v = sciview.addVolume(nai.third, file.name) {
            origin = Origin.FrontBottomLeft
            this.spatialOrNull()?.scale = Vector3f(0.08f, 0.08f, 5.0f)
            transferFunction = TransferFunction.ramp(0.3f, 0.1f, 0.1f)
            // min 20, max 180, color map fire

            transferFunction.addControlPoint(0.3f, 0.5f)
            transferFunction.addControlPoint(0.8f, 0.01f)
            converterSetups.get(0).setDisplayRange(20.0, 220.0)
            colormap = Colormap.fromColorTable(colormapVolume)
        }


        task.status = "Creating labeling"
        task.completion = 10.0f
        val rai = nai.second
        log.info("Got ${nai.first.size} labels")

        // let's extract some neurons here
        log.info("Creating labeling ...")

        val labels = (0..(nai.first.keys.maxOrNull()?.toInt() ?: 1)).toList()
        val labeling = ImgLabeling.fromImageAndLabels(rai, labels)
        log.info("Creating regions...")
        val regions = LabelRegions(labeling)
        log.info("Created ${regions.count()} regions")

        val largestNeuronLabels = nai.first.entries.sortedByDescending { p -> p.value }.take(50).shuffled().take(10).map { kv -> kv.key }

        log.info("Largest neuron labels are ${largestNeuronLabels.joinToString(",")}")

        regions.filter { largestNeuronLabels.contains(it.label.toLong() + 1L) }.forEachIndexed { i, region ->
            log.info("Meshing neuron ${i + 1}/${largestNeuronLabels.size} with label ${region.label}...")
            task.status = "Meshing neuron ${i + 1}/${largestNeuronLabels.size}"

            // ui.show(region)
            // Generate the mesh with imagej-ops
            val m: Mesh = Meshes.marchingCubes(region);

            log.info("Converting neuron ${i + 1}/${largestNeuronLabels.size} to scenery format...")
            // Convert the mesh into a scenery mesh for visualization
            val mesh = MeshConverter.toScenery(m, false, flipWindingOrder = true)
            v.addChild(mesh)
            sciview.publishNode(mesh)
            mesh.ifMaterial {
                diffuse =
                        colormapNeurons.lookupARGB(0.0, 255.0, kotlin.random.Random.nextDouble(0.0, 255.0)).toRGBColor()
                                .xyz()
                roughness = 0.0f
            }
            mesh.name = "Neuron $i"

            val completion = 10.0f + ((i + 1) / largestNeuronLabels.size.toFloat()) * 90.0f
            task.completion = completion
        }

        task.completion = 100.0f
    }

    fun readCremiHDF5(path: String, scale: Double = 1.0): NeuronsAndImage? {
        log.info("Reading cremi HDF5 from $path")

        try {
            val hdf5Reader = HDF5Factory.openForReading(path)
            val n5Reader: N5HDF5Reader
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
            while (cursor.hasNext()) {
                cursor.fwd();
                dest.setPosition(cursor);
                val value = cursor.get()
                dest.get().set(value);
                neurons[value.get()] = (neurons[value.get()] ?: 0) + 1
            }

            log.info("Got RAI from HDF5, dimensions ${img.dimensionsAsLongArray().joinToString(",")}")

            return if (scale != 1.0) {
                Triple(neurons, ops.transform().scaleView(img, doubleArrayOf(scale, scale, scale), DefaultInterpolators<UnsignedLongType>().get(Interpolation.NEARESTNEIGHBOR)), volume)
            } else {
                Triple(neurons, img, volume)
            }
        } catch (e: IOException) {
            log.error("Could not read Cremi HDF5 file from $path")
            e.printStackTrace()
        } catch (e: hdf.hdf5lib.exceptions.HDF5FileInterfaceException) {
            log.error("Could not read Cremi HDF5 file from $path. It might be the wrong file or corrupted. Please delete the file and rerun.")
            e.printStackTrace()
        }

        return null
    }

    private fun Int.toRGBColor(): Vector4f {
        val a = ARGBType.alpha(this) / 255.0f
        val r = ARGBType.red(this) / 255.0f
        val g = ARGBType.green(this) / 255.0f
        val b = ARGBType.blue(this) / 255.0f

        return Vector4f(r, g, b, a)
    }
}
