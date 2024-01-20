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

import bdv.util.volatiles.VolatileViews
import graphics.scenery.*
import graphics.scenery.volumes.*
import ij.IJ
import net.imagej.lut.LUTService
import net.imglib2.*
import net.imglib2.algorithm.gauss3.Gauss3
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions
import net.imglib2.cache.img.SingleCellArrayImg
import net.imglib2.converter.Converters
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.loops.LoopBuilder
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.volatiles.VolatileUnsignedShortType
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

/*
Based on Ulrik and Tobi's flybrain example from scenery
 */
@Plugin(type = Command::class,
        label = "Load Flybrain Out-of-Core demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load Flybrain Out-of-Core demo", weight = MenuWeights.DEMO_ADVANCED_CREMI)])
class LoadFlybrainOutOfCore : Command {
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var lut: LUTService

    override fun run() {
        log.info("Downloading flybrain stack ...")
        // "https://imagej.nih.gov/ij/images/flybrain.zip"
        // val url = "/Users/kharrington/Downloads/flybrain.zip"
        val url = "https://imagej.net/images/flybrain.zip"
        val imp = IJ.openImage(url)
        val flybrain: RandomAccessibleInterval<ARGBType> = ImageJFunctions.wrapRGBA(imp)
        log.info("Done.")

        val transform = AffineTransform3D()
        val sx = imp.calibration.pixelWidth
        val sy = imp.calibration.pixelHeight
        val sz = imp.calibration.pixelDepth  * 10
        transform[sx, 0.0, 0.0, 0.0, 0.0, sy, 0.0, 0.0, 0.0, 0.0, sz] = 0.0
        val type = UnsignedShortType()

        val width = imp.width / 10
        val height = imp.height / 10

        // extract the red channel
        val red = Converters.convert(flybrain, { i: ARGBType, o: UnsignedShortType -> o.set(ARGBType.red(i.get())) }, type)

        // set up two cached CellImages that (lazily) convolve red with different sigmas
        val source: RandomAccessible<UnsignedShortType> = Views.extendBorder(red)
        val sigma1 = doubleArrayOf(5.0, 5.0, 5.0)
        val sigma2 = doubleArrayOf(4.0, 4.0, 4.0)
        val dimensions = Intervals.dimensionsAsLongArray(flybrain)
        val factory = ReadOnlyCachedCellImgFactory(
                ReadOnlyCachedCellImgOptions.options().cellDimensions(32, 32, 32))
        val gauss1: Img<UnsignedShortType> = factory.create(dimensions, type, { cell: SingleCellArrayImg<UnsignedShortType, *>? ->
            Gauss3.gauss(sigma1, source, cell, 1)
        })
        val gauss2: Img<UnsignedShortType> = factory.create(dimensions, type, { cell: SingleCellArrayImg<UnsignedShortType, *>? ->
            Gauss3.gauss(sigma2, source, cell, 1)
        })

        // set up another cached CellImages that (lazily) computes the absolute difference of gauss1 and gauss2
        val diff: Img<UnsignedShortType> = factory.create(dimensions, type, { cell: SingleCellArrayImg<UnsignedShortType, *>? ->
            LoopBuilder.setImages(
                    Views.interval(gauss1, cell),
                    Views.interval(gauss2, cell),
                    cell)
                    .forEachPixel(LoopBuilder.TriConsumer { in1: UnsignedShortType, in2: UnsignedShortType, out: UnsignedShortType -> out.set(10 * Math.abs(in1.get() - in2.get())) })
        })

        val brain = Volume.fromRAI(
                red as RandomAccessibleInterval<UnsignedShortType>,
                UnsignedShortType(),
                name = "flybrain",
                hub = sciview.hub
        )
        brain.transferFunction = TransferFunction.ramp(0.001f, 0.4f)
        brain.converterSetups.first().setDisplayRange(0.0, 255.0)
        brain.spatial().position = Vector3f(0f, 0f, 0.0f)
        brain.spatial().scale = Vector3f(sx.toFloat(), sy.toFloat(), sz.toFloat())

        @Suppress("UNCHECKED_CAST")
        val gaussDiff = Volume.fromRAI(
                VolatileViews.wrapAsVolatile(diff) as RandomAccessibleInterval<VolatileUnsignedShortType>,
                VolatileUnsignedShortType(),
                name = "diff of gauss1 and gauss2",
                hub = sciview.hub
        )
        gaussDiff.transferFunction = TransferFunction.ramp(0.1f, 0.1f)
        gaussDiff.converterSetups.first().setDisplayRange(0.0, 255.0)
        gaussDiff.spatial().position = Vector3f(width.toFloat(), height.toFloat(), 0.0f)
        gaussDiff.spatial().scale = Vector3f(sx.toFloat(), sy.toFloat(), sz.toFloat())

        @Suppress("UNCHECKED_CAST")
        val brainGauss1 = Volume.fromRAI(
                VolatileViews.wrapAsVolatile(gauss1) as RandomAccessibleInterval<VolatileUnsignedShortType>,
                VolatileUnsignedShortType(),
                name = "gauss1",
                hub = sciview.hub
        )
        brainGauss1.transferFunction = TransferFunction.ramp(0.1f, 0.1f)
        brainGauss1.converterSetups.first().setDisplayRange(0.0, 60.0)
        brainGauss1.spatial().position = Vector3f(width.toFloat(), 0f, 0.0f)
        brainGauss1.spatial().scale = Vector3f(sx.toFloat(), sy.toFloat(), sz.toFloat())

        @Suppress("UNCHECKED_CAST")
        val brainGauss2 = Volume.fromRAI(
                VolatileViews.wrapAsVolatile(gauss2) as RandomAccessibleInterval<VolatileUnsignedShortType>,
                VolatileUnsignedShortType(),
                name = "gauss2",
                hub = sciview.hub
        )
        brainGauss2.transferFunction = TransferFunction.ramp(0.1f, 0.1f)
        brainGauss2.converterSetups.first().setDisplayRange(0.0, 60.0)
        brainGauss2.spatial().position = Vector3f(0f, height.toFloat(), 0.0f)
        brainGauss2.spatial().scale = Vector3f(sx.toFloat(), sy.toFloat(), sz.toFloat())

        sciview.addNode(brain)
        sciview.addNode(gaussDiff)
        sciview.addNode(brainGauss1)
        sciview.addNode(brainGauss2)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(LoadFlybrainOutOfCore::class.java, true, argmap)
        }
    }
}