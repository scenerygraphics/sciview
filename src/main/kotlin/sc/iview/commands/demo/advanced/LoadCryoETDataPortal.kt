/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
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

import graphics.scenery.Origin
import graphics.scenery.volumes.Colormap
import net.imagej.lut.LUTService
import net.imagej.ops.OpService
import net.imglib2.FinalInterval
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.img.cell.CellImgFactory
import net.imglib2.loops.LoopBuilder
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.real.FloatType
import net.imglib2.view.Views
import org.embl.mobie.io.ome.zarr.openers.OMEZarrS3Opener
import org.joml.Vector3f
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

@Plugin(type = Command::class,
        label = "Load CryoET Data Portal demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load CryoET Data Portal demo", weight = MenuWeights.DEMO_ADVANCED_CREMI)])
class LoadCryoETDataPortal : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var lut: LUTService

    private var zarrPath = "https://files.cryoetdataportal.cziscience.com/10000/TS_043/Tomograms/VoxelSpacing13.480/CanonicalTomogram/TS_043.zarr"

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
        val task = sciview.taskManager.newTask("CryoETDataPortal", "Loading dataset")

        task.status = "Reading dataset"
        val spimData = OMEZarrS3Opener.readURL(zarrPath)
        val setupId = 0
        val timepointId = 0
        val rawRaiVolume = (
                spimData.getSequenceDescription()
                        .getImgLoader()
                        .getSetupImgLoader(setupId)
                        .getImage(timepointId)
                ) as RandomAccessibleInterval<FloatType>

        // Calculate half of the z-dimension
        val halfZ = rawRaiVolume.dimension(2) / 2

        // Define the interval for half of the z-axis
        val minInterval = longArrayOf(0, 0, 0)
        val maxInterval = longArrayOf(rawRaiVolume.dimension(0) - 1, rawRaiVolume.dimension(1) - 1, halfZ)
        val halfZInterval = FinalInterval(minInterval, maxInterval)

        // Create a view over half of the z-axis
        val halfZView = Views.interval(rawRaiVolume, halfZInterval)

        // Create a CellImgFactory for UnsignedByteType with a specified cell size
        val cellSize = 64
        val factory = CellImgFactory<UnsignedByteType>(UnsignedByteType(), cellSize, cellSize, cellSize)

        // Create an empty RandomAccessibleInterval of UnsignedByteType with half z-dimension using the factory
        val halfDimensions = longArrayOf(rawRaiVolume.dimension(0), rawRaiVolume.dimension(1), halfZ + 1)
        val halfRaiVolume: Img<UnsignedByteType> = factory.create(*halfDimensions)


        // Copy and convert the data from the view into the new RAI
        LoopBuilder.setImages(halfZView as RandomAccessibleInterval<FloatType>, halfRaiVolume).forEachPixel { input, output ->
            output.setInteger(((input.get() + 26f) / 60 * 255.0).toInt().coerceIn(0, 255))
        }


        val colormapVolume = lut.loadLUT(lut.findLUTs().get("Grays.lut"))

        val v = sciview.addVolume(halfRaiVolume, "CryoET Data Portal") {
            origin = Origin.FrontBottomLeft
            this.spatialOrNull()?.scale = Vector3f(1f, 1f, 1f)
            // transferFunction = TransferFunction.ramp(0.3f, 0.1f, 0.1f)
            // min 20, max 180, color map fire

            // transferFunction.addControlPoint(0.3f, 0.5f)
            // transferFunction.addControlPoint(0.8f, 0.01f)
            converterSetups.get(0).setDisplayRange(0.0, 255.0)
            colormap = Colormap.fromColorTable(colormapVolume)
        }

        task.completion = 100.0f
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(LoadCryoETDataPortal::class.java, true, argmap)
        }
    }
}
