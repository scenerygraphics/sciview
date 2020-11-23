/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
package sc.iview.commands.demo

import bdv.util.BdvFunctions
import graphics.scenery.numerics.OpenSimplexNoise
import graphics.scenery.volumes.Volume
import net.imglib2.FinalInterval
import net.imglib2.Localizable
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.position.FunctionRandomAccessible
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.Views
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.util.*
import java.util.function.BiConsumer

/**
 * A demo of volume rendering + time
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, label = "Volume Timeseries", menuRoot = "SciView", menu = [Menu(label = "Demo", weight = MenuWeights.DEMO), Menu(label = "Volume Timeseries", weight = MenuWeights.DEMO_VOLUME_RENDER)])
class VolumeTimeseriesDemo : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        val dataset = makeDataset()

        val bdv = BdvFunctions.show(dataset, "test")
        val v = sciView.addVolume(dataset, floatArrayOf(1f, 1f, 1f, 1f)) as Volume?
        v!!.pixelToWorldRatio = 0.1f // FIXME
        v.name = "Volume Render Demo"
        v.dirty = true
        v.needsUpdate = true

        bdv.bdvHandle.viewerPanel.addTimePointListener { t ->
            v.goToTimepoint(t.coerceIn(0, v.timepointCount-1))
        }

        sciView.setActiveNode(v)
        sciView.centerOnNode(sciView.activeNode)
    }

    fun makeDataset(): RandomAccessibleInterval<UnsignedByteType> {
        // Interval is 30x30x30 w/ 10 timepoints
        val interval = FinalInterval(longArrayOf(0, 0, 0, 0), longArrayOf(30, 30, 30, 100))
        val center = (interval.max(2) / 2).toDouble()
        val noise = OpenSimplexNoise()
        val rng = Random(System.nanoTime())
        val dx: Float
        val dy: Float
        val dz: Float
        dx = rng.nextFloat()
        dy = rng.nextFloat()
        dz = rng.nextFloat()
        val f = 3.0 / interval.max(2).toDouble()
        val dt = 0.618
        val radius = 0.35
        val pixelmapper = BiConsumer<Localizable, UnsignedByteType> { localizable, value ->
            val x = center - localizable.getDoublePosition(0)
            val y = center - localizable.getDoublePosition(1)
            val z = center - localizable.getDoublePosition(2)
            val t = localizable.getDoublePosition(3)
            val d = Math.sqrt(x * x + y * y + z * z) / interval.max(2).toDouble()
            val offset = Math.abs(
                    noise.random3D(
                            (x + t * dt * dx) * f,
                            (y + t * dt * dy) * f,
                            (z + t * dt * dz) * f))

            val v: Double
            v = if (d - offset < radius) d - offset else 0.0
            value.set((255.0 * v).toInt())
        }
        val fra = FunctionRandomAccessible(
                4, pixelmapper, { UnsignedByteType() })
        return hardCopy(Views.interval(fra, interval))
    }

    fun hardCopy(img: RandomAccessibleInterval<UnsignedByteType>): Img<UnsignedByteType> {
        val out: Img<UnsignedByteType> = ArrayImgs.unsignedBytes(
                img.dimension(0),
                img.dimension(1),
                img.dimension(2),
                img.dimension(3))
        val imgAccess = img.randomAccess()
        val outCur = out.localizingCursor()
        while (outCur.hasNext()) {
            outCur.fwd()
            imgAccess.setPosition(outCur)
            outCur.get().set(imgAccess.get())
        }
        return out
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()

            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(VolumeTimeseriesDemo::class.java, true, argmap)
        }
    }
}