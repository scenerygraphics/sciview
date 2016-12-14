package sc.fiji.threed;

import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.ThreeDViewer;

import java.util.ArrayList;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: July 2016
 */
@Plugin(type = Command.class,
        menuPath = "ThreeDViewer>Add>Label image")
public  class AddLabelImage<T extends RealType<T>> implements Command {


    @Parameter
    private Dataset currentImage;

    @Parameter
    private OpService ops;

    @Override
    public  void run() {

        // interpret the current image as a label image and convert it to ImgLabeling

        Img<T> labelMap = (Img<T>) currentImage.getImgPlus();

        final Dimensions dims = labelMap;
        final IntType t = new IntType();
        final RandomAccessibleInterval<IntType> img = Util.getArrayOrCellImgFactory(dims, t).create(dims, t);
        ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>(img);

        final Cursor<LabelingType<Integer>> labelCursor = Views.flatIterable(labeling).cursor();

        for (final T input : Views.flatIterable(labelMap)) {
            final LabelingType<Integer> element = labelCursor.next();
            if (input.getRealFloat() != 0)
            {
                element.add((int) input.getRealFloat());
            }
        }

        // take the regions, process them to meshes and put it in the viewer
        LabelRegions<Integer> labelRegions = new LabelRegions<Integer>(labeling);

        ArrayList<RandomAccessibleInterval<BoolType>> regions = new ArrayList<RandomAccessibleInterval<BoolType>>();
        Object[] regionsArr = labelRegions.getExistingLabels().toArray();
        for (int i = 0; i < labelRegions.getExistingLabels().size(); i++)
        {
            LabelRegion<Integer> lr = labelRegions.getLabelRegion((Integer)regionsArr[i]);

            Mesh mesh = ops.geom().marchingCubes(lr);
            ThreeDViewer.addMesh(mesh);
        }
    }

}
