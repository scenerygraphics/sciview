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
import net.imagej.ops.OpService
import net.imglib2.Dimensions
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.roi.labeling.ImgLabeling
import net.imglib2.roi.labeling.LabelRegions
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.IntType
import net.imglib2.util.Util
import net.imglib2.view.Views
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.ADD
import sc.iview.commands.MenuWeights.EDIT_ADD_LABELIMAGE

/**
 * Adds a label image to the scene.
 *
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG Dresden
 */
@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Add", weight = ADD), Menu(
        label = "Label Image",
        weight = EDIT_ADD_LABELIMAGE
    )]
)
class AddLabelImage<T : RealType<T>?> : Command {
    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var currentImage: Dataset
    override fun run() {

        // interpret the current image as a label image and convert it to ImgLabeling
        val labelMap = currentImage.imgPlus as Img<T>
        val dims: Dimensions = labelMap
        val t = IntType()
        val img: RandomAccessibleInterval<IntType> = Util.getArrayOrCellImgFactory(dims, t).create(dims, t)
        val labeling = ImgLabeling<Int, IntType>(img)
        val labelCursor = Views.flatIterable(labeling).cursor()
        for (input in Views.flatIterable(labelMap)) {
            val element = labelCursor.next()
            if (input!!.realFloat != 0f) {
                element.add(input.realFloat.toInt())
            }
        }

        // take the regions, process them to meshes and put it in the viewer
        val labelRegions = LabelRegions(labeling)
        val regionsArr: Array<Any> = labelRegions.existingLabels.toTypedArray()
        for (i in labelRegions.existingLabels.indices) {
            val lr = labelRegions.getLabelRegion(regionsArr[i] as Int)
            val mesh = ops.geom().marchingCubes(lr)
            sciView.addMesh(mesh)
        }
    }
}