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

import graphics.scenery.volumes.RAIVolume
import mmcorej.CMMCore
import net.imagej.Dataset
import net.imagej.DefaultDataset
import net.imagej.axis.DefaultAxisType
import net.imagej.axis.DefaultLinearAxis
import net.imagej.ops.OpService
import net.imagej.units.UnitService
import net.imglib2.img.array.ArrayImg
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.img.array.ArrayImgs
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.type.numeric.integer.ShortType
import net.imglib2.util.Fraction
import org.scijava.Context
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.MenuWeights.EDIT_ADD

/**
 */
@Plugin(type = Command::class,
    menuRoot = "SciView",
    menu = [
        Menu(label = "Demo", weight = MenuWeights.DEMO),
        Menu(label = "VolumeMM", weight = MenuWeights.EDIT_ADD_VOLUME)])
class AddVolumeMM : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var unitService: UnitService


    @Parameter(label = "Use voxel dimensions from image", callback = "setVoxelDimensions")
    private var inheritFromImage = true

    @Parameter(label = "Voxel Size X")
    private var voxelWidth = 1.0f

    @Parameter(label = "Voxel Size Y")
    private var voxelHeight = 1.0f

    @Parameter(label = "Voxel Size Z")
    private var voxelDepth = 1.0f

    override fun run() {
        val core = CMMCore()
        val info = core.versionInfo
        println(info)
        core.loadSystemConfiguration ("C:/Program Files/Micro-Manager-2.0gamma/MMConfig_demo.cfg");
        core.snapImage();
        //val img1 = core.image as ShortArray// returned as a 1D array of signed integers in row-major order
        val width = core.imageWidth;
        val height = core.imageHeight;

        val images = mutableListOf<ShortArray>()

        (0..10).forEach {
            core.snapImage();
            images.add(core.image as ShortArray)
        }

        val img = images.flatMap { it.toList() }.toShortArray()
        val ai2 = ArrayImgs.unsignedShorts(img,width,height,10)

        ImageJFunctions.show(ai2)

        val vol = sciView.addVolume(ai2,floatArrayOf(1f, 1f, 1000f))

        vol.let {
            val v = it as RAIVolume

        }

        println("${img.toString()}, ${width}, $height")
    /*if (inheritFromImage) {
            val n = sciView.addVolume(image)
            n?.name = image.name ?: "Volume"
        } else {
            val n = sciView.addVolume(image, floatArrayOf(voxelWidth, voxelHeight, voxelDepth))
            n?.name = image.name ?: "Volume"
        }*/
    }
}