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
import graphics.scenery.numerics.OpenSimplexNoise
import graphics.scenery.utils.extensions.plusAssign
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
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
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.io.IOService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.demo.ResourceLoader
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
                Menu(label = "Volume Timeseries, lol", weight = MenuWeights.DEMO_ANIMATION_VOLUMETIMESERIES)])
class VolumeTimeseriesDemo : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var io: IOService

    override fun run() {
        System.err.println("run")
        val dataset = makeDataset()

        System.err.println("load")
        //val cubeFile = ResourceLoader.createFile(javaClass, "/t1-head.tif")
        //val imp: ImagePlus = IJ.openImage(cubeFile.absolutePath)
        //val dataset: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)

        //val dataset = io.open("/HisYFP-SPIM.xml") as Dataset

        System.err.println("bdv")
        val bdv = BdvFunctions.show(dataset, "test")
        System.err.println("volume")
        sciView.addVolume(dataset, floatArrayOf(1f, 1f, 1f, 1f)) {
            pixelToWorldRatio = 1f
            name = "Volume Render Demo"
            dirty = true
            needsUpdate = true
            origin = Origin.Center
            //scale *= Vector3f(1f,1f,2f)


            val pivot = Node()
            //sciView.sceneNodes.first().addChild(pivot)
            //sciView.addChild(pivot)

            //pivot.position = Vector3f(384f,384f,128f);iukl


            val sliceP =  SlicingPlane()//Box(Vector3f(20f,20f,0.05f))
            sliceP.rotation = sliceP.rotation.rotateX(Math.PI.toFloat() * 0.5f)
            //raisliceP.addTargetVolume(this)
            //this.slicingMode = Volume.SlicingMode.Slicing
            sliceP.position = Vector3f(0f,0f,1f)
            sciView.addChild(sliceP)

            val sliceBox = Box(Vector3f(5f,5f,0.05f))
            sciView.addChild(sliceBox)
            sliceBox.position = Vector3f(0f,0f,1f)

            val volumePivot = sciView.find("volume pivot")
            val pivotBox = Box(Vector3f(1f))
            volumePivot?.addChild(pivotBox)



            //bdv.bdvHandle.viewerPanel.addTimePointListener { t ->
            //    goToTimepoint(t.coerceIn(0, timepointCount-1))
            //}
            //bdv.bdvHandle.viewerPanel.transform

            bdv.bdvHandle.viewerPanel.addTransformListener {

                val vt = bdv.bdvHandle.viewerPanel.state().viewerTransform


                val w = bdv.bdvHandle.viewerPanel.display.width //800
                val h = bdv.bdvHandle.viewerPanel.display.height //577
                // window center offset
                it.set(it.get(0, 3) - w + 0.5, 0, 3)
                it.set(it.get(1, 3) - h + 0.5, 1, 3)
                //it.scale(1/18.612903225806452)
                //it.scale(1/2.25390625)

                val scaleX = Affine3DHelpers.extractScale(it,0)
                val scaleY = Affine3DHelpers.extractScale(it,1)
                val scaleZ = Affine3DHelpers.extractScale(it,2)
                it.scale(1/scaleX)

                //println(it)

/*
                val rot = sciView.camera?.let { cam ->
                    cam.position = Vector3f(t[0].toFloat(), t[1].toFloat(), t[2].toFloat())
                    cam.rotation = Quaternionf().lookAlong(this.worldPosition() - cam.worldPosition(), cam.up)
                }
*/
                //sciView.camera?.position = Vector3f(t[0].toFloat(), t[1].toFloat(), t[2].toFloat())
                //println(this.position)


                val volumePivot = sciView.find("volume pivot")
                volumePivot?.wantsComposeModel = false


                val dar2 = DoubleArray(16)
                it.toArray(dar2)

                // *it* is column- major with the translation at the w position of the base vectors
                val dar = DoubleArray(16)
                it.inverse().toArray(dar) //inverse because we are rotating the object not view
                dar[15] = 1.0
                val far = dar.map { d -> d.toFloat() }.toFloatArray()

                val bb = this.boundingBox?.max ?: Vector3f(1f)
                val trans = Vector3f(dar2[3].toFloat(),dar2[7].toFloat(),dar2[11].toFloat())*0.1f
                //val trans = Vector3f(far[3] / bb.x,far[7] / bb.y,far[11] / bb.z)*-15f //TODO: pixel to world or something
                val trans2 = Vector3f(far[3] ,far[7] ,far[11] )
                //log.warn(trans)

                far[3+0*4] = 0.0f //-dar[2+0*4]
                far[3+1*4] = 0.0f //-dar[2+1*4]
                far[3+2*4] = 0.0f //-dar[2+2*4]

                val rotation = Matrix4f().set(far)
                val iRotation = Matrix3f()
                rotation.get3x3(iRotation)
                val transM =  trans.mul(iRotation.invert())
                val inverseViewerMatrix = Matrix4f().rotateZYX(Math.PI.toFloat(),Math.PI.toFloat(),0f).mul(rotation).translate(trans)

                volumePivot?.model = inverseViewerMatrix
                volumePivot?.needsUpdateWorld = true
                volumePivot?.updateWorld(true,false)
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
        val interval = FinalInterval(longArrayOf(0, 0, 0, 0), longArrayOf(60, 60, 30, 100))
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
