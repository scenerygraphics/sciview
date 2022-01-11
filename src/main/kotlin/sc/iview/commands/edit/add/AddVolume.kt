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
package sc.iview.commands.edit.add

import net.imagej.Dataset
import net.imagej.axis.DefaultAxisType
import net.imagej.axis.DefaultLinearAxis
import net.imagej.ops.OpService
import net.imagej.units.UnitService
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.MenuWeights.EDIT_ADD

/**
 * Command to add a volume to the scene.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Edit", weight = MenuWeights.EDIT), Menu(label = "Add", weight = EDIT_ADD), Menu(label = "Volume", weight = MenuWeights.EDIT_ADD_VOLUME)])
class AddVolume : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var unitService: UnitService

    @Parameter
    private lateinit var image: Dataset

    @Parameter(label = "Use voxel dimensions from image", callback = "setVoxelDimensions")
    private var inheritFromImage = true

    @Parameter(label = "Voxel Size X", stepSize = "0.01f")
    private var voxelWidth = 1.0f

    @Parameter(label = "Voxel Size Y", stepSize = "0.01f")
    private var voxelHeight = 1.0f

    @Parameter(label = "Voxel Size Z", stepSize = "0.01f")
    private var voxelDepth = 1.0f

    override fun run() {
        if (inheritFromImage) {
            sciView.addVolume(image) {
                name = image.name ?: "Volume"
            }
        } else {
            sciView.addVolume(image, floatArrayOf(voxelWidth, voxelHeight, voxelDepth)) {
                name = image.name ?: "Volume"
            }
        }
    }

    private fun setVoxelDimension() {
        val axis = arrayOf(
                DefaultLinearAxis(DefaultAxisType("X", true), "um", 1.0),
                DefaultLinearAxis(DefaultAxisType("Y", true), "um", 1.0),
                DefaultLinearAxis(DefaultAxisType("Z", true), "um", 1.0)
        )

        val voxelDims = FloatArray(minOf(image.numDimensions(), 3))

        for (d in voxelDims.indices) {
            val inValue = image.axis(d).averageScale(0.0, 1.0)
            if (image.axis(d).unit() == null) {
                voxelDims[d] = inValue.toFloat()
            } else {
                voxelDims[d] = unitService.value(inValue, image.axis(d).unit(), axis[d].unit()).toFloat()
            }
        }

        voxelWidth = voxelDims.getOrElse(0) { 1.0f }
        voxelHeight = voxelDims.getOrElse(1) { 1.0f }
        voxelDepth = voxelDims.getOrElse(2) { 1.0f }
    }
}
