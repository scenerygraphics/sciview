/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
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
package sc.iview.commands.demo.animation

import bdv.util.Affine3DHelpers
import bdv.util.BdvFunctions
import graphics.scenery.Box
import graphics.scenery.Node
import graphics.scenery.Origin
import graphics.scenery.backends.vulkan.VulkanNodeHelpers.rendererMetadata
import graphics.scenery.numerics.OpenSimplexNoise
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.plusAssign
import graphics.scenery.utils.extensions.times
import ij.IJ
import ij.ImagePlus
import io.scif.services.DatasetIOService
import net.imagej.Dataset
import net.imglib2.FinalInterval
import net.imglib2.Localizable
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.position.FunctionRandomAccessible
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.view.Views
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.demo.ResourceLoader
import java.io.IOException
import java.util.*
import java.util.function.BiConsumer

/**
 * A demo of volume rendering + time
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class,
        label = "Volume Timeseries",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
                Menu(label = "Animation", weight = MenuWeights.DEMO_ANIMATION),
                Menu(label = "Volume Timeseries", weight = MenuWeights.DEMO_ANIMATION_VOLUMETIMESERIES)])
class VolumeTimeseriesDemo : Command {
    @Parameter
    private lateinit var sciView: SciView
    @Parameter
    private lateinit var datasetIO: DatasetIOService
    @Parameter
    private lateinit var log: LogService

    override fun run() {
        //val dataset = makeDataset()

        val cubeFile = ResourceLoader.createFile(javaClass, "/t1-head.tif")
        val imp: ImagePlus = IJ.openImage(cubeFile.absolutePath)
        val dataset: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)

        val bdv = BdvFunctions.show(dataset, "test")
        sciView.addVolume(dataset, floatArrayOf(1f, 1f, 1f, 1f)) {
            pixelToWorldRatio = 1f
            name = "Volume Render Demo"
            dirty = true
            needsUpdate = true
            origin = Origin.Center
            //scale = Vector3f(1f)


            val pivot = Node()
            //sciView.sceneNodes.first().addChild(pivot)
            this.addChild(pivot)

            //pivot.position = Vector3f(384f,384f,128f);



            val sliceP = Box(Vector3f(200f,200f,0.05f)) //Box(Vector3f(5f))//SlicingPlane()
            //sliceP.addTargetVolume(this)
            //this.slicingMode = Volume.SlicingMode.Both
            pivot.addChild(sliceP)


            //bdv.bdvHandle.viewerPanel.addTimePointListener { t ->
            //    goToTimepoint(t.coerceIn(0, timepointCount-1))
            //}
            //bdv.bdvHandle.viewerPanel.transform

            bdv.bdvHandle.viewerPanel.addTransformListener {

                val vt = bdv.bdvHandle.viewerPanel.state().viewerTransform


                val w = 800
                val h = 577
                // window center offset
                it.set(it.get(0, 3) - w + 0.5, 0, 3)
                it.set(it.get(1, 3) - h + 0.5, 1, 3)
                //it.scale(1/18.612903225806452)

                it.scale(1/2.25390625)
                it.scale(0.3)

                //println(it)

                val m = DoubleArray(4)
                Affine3DHelpers.extractRotationAnisotropic(it,m)

                val t = it.translation
                val tv = Vector3f(t[0].toFloat(), t[1].toFloat() * -1f,t[2].toFloat())
                val fixedDistnace = Vector3f()
                tv.normalize(2f,fixedDistnace)

                //log.warn(tv)


                //sliceP.position = tv -fixedDistnace //* 0.05f
                //pivot.rotation = Quaternionf(m[1],m[2],m[3],m[0]).normalize()

/*
                val rot = sciView.camera?.let { cam ->
                    cam.position = Vector3f(t[0].toFloat(), t[1].toFloat(), t[2].toFloat())
                    cam.rotation = Quaternionf().lookAlong(this.worldPosition() - cam.worldPosition(), cam.up)
                }
*/
                //sciView.camera?.position = Vector3f(t[0].toFloat(), t[1].toFloat(), t[2].toFloat())
                //println(this.position)


                val volumePivot = sciView.find("volume pivot")


                val dar = DoubleArray(16)
                it.toArray(dar)
                val far = dar.map { d -> d.toFloat() }.toFloatArray()
                val viewerMatrix = Matrix4f().set(far).invertAffine()
                volumePivot?.world = viewerMatrix
                this.updateWorld(true,true)
                //sliceP.world = sliceP.world.set(far)//.transpose()

                /* other way around
                val w = AffineTransform3D()
                val arr = FloatArray(16)
                Matrix4f(this.world).transpose().get(it)

                w.set(*arr.map { it.toDouble() }.toDoubleArray())
                */
            }

            sciView.setActiveNode(this)
        }
        sciView.centerOnNode(sciView.activeNode)
        sciView.camera!!.position += Vector3f(0f,0f,50f)
    }

    fun makeDataset(): RandomAccessibleInterval<UnsignedByteType> {
        // Interval is 30x30x30 w/ 100 timepoints
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
