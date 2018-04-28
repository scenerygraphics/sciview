/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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
package sc.iview.create;

import java.util.ArrayList;

import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciViewService;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden,
 * rhaase@mpi-cbg.de Date: July 2016
 */
@Plugin(type = Command.class, menuPath = "SciView>Add>Label image")
public class AddLabelImage<T extends RealType<T>> implements Command {

    @Parameter
    private Dataset currentImage;

    @Parameter
    private OpService ops;

    @Parameter
    private SciViewService sceneryService;

    @Override
    public void run() {

        // interpret the current image as a label image and convert it to ImgLabeling

        Img<T> labelMap = ( Img<T> ) currentImage.getImgPlus();

        final Dimensions dims = labelMap;
        final IntType t = new IntType();
        final RandomAccessibleInterval<IntType> img = Util.getArrayOrCellImgFactory( dims, t ).create( dims, t );
        ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>( img );

        final Cursor<LabelingType<Integer>> labelCursor = Views.flatIterable( labeling ).cursor();

        for( final T input : Views.flatIterable( labelMap ) ) {
            final LabelingType<Integer> element = labelCursor.next();
            if( input.getRealFloat() != 0 ) {
                element.add( ( int ) input.getRealFloat() );
            }
        }

        // take the regions, process them to meshes and put it in the viewer
        LabelRegions<Integer> labelRegions = new LabelRegions<Integer>( labeling );

        ArrayList<RandomAccessibleInterval<BoolType>> regions = new ArrayList<RandomAccessibleInterval<BoolType>>();
        Object[] regionsArr = labelRegions.getExistingLabels().toArray();
        for( int i = 0; i < labelRegions.getExistingLabels().size(); i++ ) {
            LabelRegion<Integer> lr = labelRegions.getLabelRegion( ( Integer ) regionsArr[i] );

            Mesh mesh = ops.geom().marchingCubes( lr );
            sceneryService.getActiveSciView().addMesh( mesh );
        }
    }

}
