package sc.iview.mandelbulb;

import bdv.BigDataViewer;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.listeners.Listeners;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MultiResolutionMandelbulb {

    public static void main(String[] args) throws Exception {
        // Define max scale level
        int maxScale = 4; // Adjust this value to test rendering at different scales

        // Desired grid size at the finest resolution level
        final int desiredFinestGridSize = 8; // Define as per your requirement

        // Compute the base grid size
        final int baseGridSize = desiredFinestGridSize * (int) Math.pow(2, maxScale - 1);

        // Generate resolutions and corresponding grid sizes
        final double[][] resolutions = new double[maxScale][3];
        final int[] gridSizes = new int[maxScale];

        for (int i = 0; i < maxScale; i++) {
            double scaleFactor = Math.pow(2, i);

            // Ensure resolution stays above a minimum value (0.5) to avoid zero scales
            resolutions[i][0] = Math.max(1.0 / scaleFactor, 0.5);
            resolutions[i][1] = Math.max(1.0 / scaleFactor, 0.5);
            resolutions[i][2] = Math.max(1.0 / scaleFactor, 0.5);

            gridSizes[i] = baseGridSize / (int) scaleFactor;

            System.out.println("Grid size for level " + i + ": " + gridSizes[i]);
            System.out.println("Resolution for level " + i + ": " + resolutions[i][0] + " / " + resolutions[i][1] + " / " + resolutions[i][2]);
        }

        MandelbulbCacheArrayLoader.gridSizes = gridSizes;
        MandelbulbCacheArrayLoader.baseGridSize = baseGridSize;
        MandelbulbCacheArrayLoader.desiredFinestGridSize = desiredFinestGridSize;

        // Mandelbulb parameters
        final int maxIter = 255;
        final int order = 8;

        // Create Mandelbulb ImgLoader
        MandelbulbImgLoader imgLoader = new MandelbulbImgLoader(gridSizes, maxIter, order);

        // Create a list of TimePoints (assuming single timepoint)
        final List<TimePoint> timepoints = Collections.singletonList(new TimePoint(0));

        // Create BasicViewSetup
        final BasicViewSetup viewSetup = new BasicViewSetup(0, "setup0", imgLoader.dimensions(), new DefaultVoxelDimensions(3));

        // Create SequenceDescriptionMinimal
        final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(new TimePoints(timepoints), Collections.singletonMap(viewSetup.getId(), viewSetup), imgLoader, null);

        // Define voxel size
        final double[] voxelSize = {1.0, 1.0, 1.0};

        // Create ViewRegistrations
        final HashMap<ViewId, ViewRegistration> registrations = new HashMap<>();
        for (final BasicViewSetup setup : seq.getViewSetupsOrdered()) {
            final int setupId = setup.getId();
            for (final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered()) {
                final int timepointId = timepoint.getId();
                for (int level = 0; level < resolutions.length; level++) {
                    AffineTransform3D transform = new AffineTransform3D();
                    // Apply level-specific resolutions
                    transform.scale(1 / resolutions[level][0], 1 / resolutions[level][1], 1 / resolutions[level][2]);
                    registrations.put(new ViewId(timepointId, setupId), new ViewRegistration(timepointId, setupId, transform));
                }
            }
        }

        // Create SpimDataMinimal
        final SpimDataMinimal spimData = new SpimDataMinimal(null, seq, new ViewRegistrations(registrations));

        // Obtain sources and setups
        List<SourceAndConverter<?>> sources = getSourceAndConverters(spimData);
        ArrayList<ConverterSetup> converterSetups = getConverterSetups(sources);

        // Define voxel dimensions as a float array
        float[] voxelDimensions = {10000.0f, 10000.0f, 10000.0f};

        @SuppressWarnings("unchecked")
        List<SourceAndConverter<RealType<?>>> typedSources = (List<SourceAndConverter<RealType<?>>>) (List<?>) sources;

        // Create and add volume to SciView
        SciView sciview = SciView.create();
        sciview.addSpimVolume(spimData, "test", voxelDimensions);
    }

    private static List<SourceAndConverter<?>> getSourceAndConverters(SpimDataMinimal spimData) {
        WrapBasicImgLoader.wrapImgLoaderIfNecessary(spimData);
        final ArrayList<SourceAndConverter<?>> sources = new ArrayList<>();
        BigDataViewer.initSetups(spimData, new ArrayList<>(), sources);
        WrapBasicImgLoader.removeWrapperIfPresent(spimData);
        return sources;
    }

    private static ArrayList<ConverterSetup> getConverterSetups(List<SourceAndConverter<?>> sources) {
        ArrayList<ConverterSetup> converterSetups = new ArrayList<>();
        for (SourceAndConverter<?> source : sources) {
            // Placeholder logic for converter setup; customize as needed
            converterSetups.add(new ConverterSetup() {
                @Override
                public void setDisplayRange(double min, double max) {
                    // Implement this method based on actual display range logic
                }

                @Override
                public void setColor(ARGBType color) {
                }

                @Override
                public boolean supportsColor() {
                    return false;
                }

                @Override
                public double getDisplayRangeMin() {
                    return 0;
                }

                @Override
                public double getDisplayRangeMax() {
                    return 0;
                }

                @Override
                public ARGBType getColor() {
                    return null;
                }

                @Override
                public Listeners<SetupChangeListener> setupChangeListeners() {
                    return null;
                }

                @Override
                public int getSetupId() {
                    return 0; // Implement unique ID retrieval logic
                }
            });
        }
        return converterSetups;
    }

}
