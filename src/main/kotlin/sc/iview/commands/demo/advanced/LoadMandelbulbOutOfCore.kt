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

import bdv.util.AxisOrder
import bdv.util.volatiles.VolatileViews
import graphics.scenery.*
import graphics.scenery.volumes.*
import net.imglib2.*
import net.imglib2.img.array.ArrayImgs
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.volatiles.VolatileUnsignedShortType
import net.imglib2.view.Views
import org.janelia.saalfeldlab.n5.GzipCompression
import org.janelia.saalfeldlab.n5.N5FSReader
import org.janelia.saalfeldlab.n5.N5FSWriter
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
import sc.iview.mandelbulb.MandelbulbCacheArrayLoader.*
import java.nio.file.Files

@Plugin(type = Command::class,
        label = "Load Mandelbulb Out-of-Core demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load Mandelbulb Out-of-Core demo", weight = MenuWeights.DEMO_ADVANCED)])
class LoadMandelbulbOutOfCore : Command {
    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var log: LogService

    override fun run() {
        gridSizes = intArrayOf(64, 128, 256, 512) // Example grid sizes for different levels
        baseGridSize = 64 // Example base grid size
        desiredFinestGridSize = 512 // Example desired finest grid size

        val level = 2 // Example level
        val maxIter = 32 // Example maximum iterations
        val order = 8 // Example Mandelbulb order

        log.info("Generating mandelbulb")
        val img = generateFullMandelbulb(level, maxIter, order)
        log.info("Generated")

        val datasetName = "mandelbulbDataset"
        val n5path = Files.createTempDirectory("scenery-mandelbulb-n5")
        val n5 = N5FSWriter(n5path.toString())
        N5Utils.save(img, n5, datasetName, intArrayOf(img.dimension(0).toInt(), img.dimension(1).toInt(), img.dimension(2).toInt()), GzipCompression())
        log.info("Dataset saved as N5")

        val ooc: RandomAccessibleInterval<UnsignedShortType> = N5Utils.openVolatile(N5FSReader(n5path.toString()), datasetName)
        val wrapped = VolatileViews.wrapAsVolatile(ooc)

        @Suppress("UNCHECKED_CAST")
        val volume = Volume.fromRAI(
                wrapped as RandomAccessibleInterval<VolatileUnsignedShortType>,
                VolatileUnsignedShortType(),
                AxisOrder.DEFAULT,
                "Mandelbulb OOC",
                sciview.hub
        )
        volume.converterSetups.first().setDisplayRange(25.0, 512.0)
        volume.transferFunction = TransferFunction.ramp(0.01f, 0.03f)
        volume.spatial().scale = Vector3f(20.0f)
        sciview.addNode(volume)

        val lights = (0 until 3).map {
            PointLight(radius = 15.0f)
        }

        lights.mapIndexed { i, light ->
            light.spatial().position = Vector3f(2.0f * i - 4.0f, i - 1.0f, 0.0f)
            light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            light.intensity = 50.0f
            sciview.addNode(light)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(LoadMandelbulbOutOfCore::class.java, true, argmap)
        }
    }
}
