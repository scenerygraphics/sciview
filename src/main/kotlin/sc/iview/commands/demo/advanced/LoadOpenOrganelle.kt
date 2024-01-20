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
import graphics.scenery.Origin
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Volume
import net.imagej.lut.LUTService
import net.imagej.ops.OpService
import net.imglib2.FinalInterval
import net.imglib2.Interval
import net.imglib2.RandomAccessibleInterval
import net.imglib2.converter.Converters
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.img.cell.CellImgFactory
import net.imglib2.loops.LoopBuilder
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.real.FloatType
import net.imglib2.type.volatiles.VolatileUnsignedByteType
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
import net.imglib2.converter.Converter
import net.imglib2.type.numeric.integer.UnsignedIntType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.volatiles.VolatileUnsignedIntType
import net.imglib2.type.volatiles.VolatileUnsignedShortType
import org.embl.mobie.io.openorganelle.OpenOrganelleS3Opener

@Plugin(type = Command::class,
        label = "Load Open Organelle demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Load Open Organelle demo", weight = MenuWeights.DEMO_ADVANCED_CREMI)])
class LoadOpenOrganelle : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private lateinit var lut: LUTService

    override fun run() {
        val reader = OMEZarrS3Opener(
                "https://janelia-cosem.s3.amazonaws.com",
                "us-west-2",
                "jrc_hela-2")
        OpenOrganelleS3Opener.setLogging(true)
        val image = reader.readKey("jrc_hela-2.n5/em/fibsem-uint16")

        // Assume the data is of UnsignedIntType
        //
        val rai = image.getSequenceDescription().getImgLoader()

        //val colormapVolume = lut.loadLUT(lut.findLUTs().get("Grays.lut"))

        // Wrap the RandomAccessibleInterval with VolatileViews for tiled rendering
        //val volatileRai = VolatileViews.wrapAsVolatile(rai) as RandomAccessibleInterval<VolatileUnsignedShortType>

        // Create and configure the volume
//        val volume = Volume.fromRAI(
//                rai,
//                VolatileUnsignedShortType(),
//                name = "My image",
//                hub = sciview.hub
//        )

//        sciview.addNode(volume)
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(LoadOpenOrganelle::class.java, true, argmap)
        }
    }
}
