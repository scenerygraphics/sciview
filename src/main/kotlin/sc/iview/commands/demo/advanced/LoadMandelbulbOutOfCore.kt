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
import graphics.scenery.PointLight
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import net.imglib2.RandomAccessibleInterval
import net.imglib2.type.volatiles.VolatileUnsignedShortType
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
import sc.iview.mandelbulb.MandelbulbCacheArrayLoader
import sc.iview.mandelbulb.MandelbulbImgLoader

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
        val maxIter = 32 // Example maximum iterations
        val order = 8 // Example Mandelbulb order

        // Define max scale level
        val maxScale = 8 // Adjust this value to test rendering at different scales

        // Desired grid size at the finest resolution level
        val desiredFinestGridSize = 8

        // Compute the base grid size
        val baseGridSize = desiredFinestGridSize * Math.pow(2.0, (maxScale - 1).toDouble()).toInt()

        // Generate resolutions and corresponding grid sizes
        val resolutions = Array(maxScale) { DoubleArray(3) }
        val gridSizes = IntArray(maxScale)

        for (i in 0 until maxScale) {
            val scaleFactor = Math.pow(2.0, i.toDouble())

            // Ensure resolution stays above a minimum value (0.5) to avoid zero scales
            resolutions[i][0] = 1.0 / scaleFactor
            resolutions[i][1] = 1.0 / scaleFactor
            resolutions[i][2] = 1.0 / scaleFactor
            gridSizes[i] = baseGridSize / scaleFactor.toInt()
            println("Grid size for level " + i + ": " + gridSizes[i])
            println("Resolution for level " + i + ": " + resolutions[i][0] + " / " + resolutions[i][1] + " / " + resolutions[i][2])
        }

        MandelbulbCacheArrayLoader.gridSizes = gridSizes
        MandelbulbCacheArrayLoader.baseGridSize = baseGridSize
        MandelbulbCacheArrayLoader.desiredFinestGridSize = desiredFinestGridSize

        log.info("Generating mandelbulb")

        val imgLoader = MandelbulbImgLoader(gridSizes, maxIter, order)

        // val img = imgLoader.getSetupImgLoader(0).getImage(0, maxScale - 1)
        val img = imgLoader.getSetupImgLoader(0).getVolatileImage(0, 0)

        log.info("Generated")

        // val wrapped = VolatileViews.wrapAsVolatile(img)
        // val wrapped = VolatileViews.wrapAsVolatile(Views.extendZero(img))
        val wrapped = img

        @Suppress("UNCHECKED_CAST")
        val volume = Volume.fromRAI(
                wrapped as RandomAccessibleInterval<VolatileUnsignedShortType>,
                VolatileUnsignedShortType(),
                name ="Mandelbulb OOC",
                hub = sciview.hub
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
