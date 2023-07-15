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
package sc.iview.commands.add

import net.imagej.Dataset
import net.imagej.axis.CalibratedAxis
import net.imagej.units.UnitService
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.type.numeric.RealType
import net.imglib2.view.Views
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import kotlin.math.max
import kotlin.math.min

/**
 * Command to add a volume to the scene.
 *
 * @author Kyle Harrington
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = MenuWeights.ADD), Menu(label = "Volume from ImageJ ...", weight = MenuWeights.EDIT_ADD_VOLUME)]
)
class AddVolume : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var unitService: UnitService

    @Parameter(autoFill = false)
    private lateinit var image: Dataset

    @Parameter(label = "Use voxel dimensions from image", callback = "setVoxelDimensions")
    private var inheritFromImage = true

    @Parameter(label = "Voxel Size X", stepSize = "0.01f")
    private var voxelWidth = 1.0f

    @Parameter(label = "Voxel Size Y", stepSize = "0.01f")
    private var voxelHeight = 1.0f

    @Parameter(label = "Voxel Size Z", stepSize = "0.01f")
    private var voxelDepth = 1.0f

    @Parameter(label = "Split channels")
    private var splitChannels = true

    private var splitChannelLuts = arrayOf("Red.lut", "Green.lut", "Blue.lut", "Magenta.lut", "Cyan.lut", "Yellow.lut")

    private fun <RealType> splitChannels(img: Img<RealType>, splitChannelDimension: Int): List<RandomAccessibleInterval<RealType>> {
        // Ensure we're safe in the dimension we choose
        var splitDim = max(min(img.numDimensions() - 1, splitChannelDimension), 0)

        val numChannels = img.dimension(splitDim)
        val channelIntervals = mutableListOf<RandomAccessibleInterval<RealType>>()

        for (channel in 0 until numChannels) {
            val interval = Views.hyperSlice(img, splitDim, channel.toLong()) as RandomAccessibleInterval<RealType>
            channelIntervals.add(interval)
        }

        return channelIntervals
    }

    override fun run() {
        if (inheritFromImage)
            setVoxelDimension()

        if (splitChannels && image.numDimensions() > 3) {
            var splitDim = ((0 until image.numDimensions()).filter { d -> (image.imgPlus.axis(d) as CalibratedAxis).type().label == "Channel" }).first()
            var channels = splitChannels(image, splitDim)

            for (ch in channels.indices) {
                var channel = channels[ch]
                var v = sciView.addVolume(channel as RandomAccessibleInterval<out RealType<*>>, name = image.name + "-ch$ch", voxelDimensions = floatArrayOf(voxelWidth, voxelHeight, voxelDepth), block = {})
                var lut = splitChannelLuts[ch % splitChannelLuts.size]
                sciView.setColormap(v, lut)
            }
        } else {
            sciView.addVolume(image, name = image.name, voxelDimensions = floatArrayOf(voxelWidth, voxelHeight, voxelDepth))
        }
    }

    private fun setVoxelDimension() {
        val voxelDims = sciView.getSciviewScale(image)

        voxelWidth = voxelDims.getOrElse(0) { 1.0f }
        voxelHeight = voxelDims.getOrElse(1) { 1.0f }
        voxelDepth = voxelDims.getOrElse(2) { 1.0f }
    }
}


