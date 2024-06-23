package sc.iview.zebrahub;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Intervals;

import java.io.IOException;
import java.util.HashMap;

public class ZebrahubImgLoader implements ViewerImgLoader {
    private final RemoteZarrLoader zarrLoader;
    public int[] gridSizes;

    public MipmapInfo mipmapInfo;
    private long[][] mipmapDimensions;
    private VolatileGlobalCellCache cache;
    private HashMap<Integer, SetupImgLoader> setupImgLoaders;

    public ZebrahubImgLoader(String zarrUrl) throws IOException {
        this.zarrLoader = new RemoteZarrLoader(zarrUrl);
        this.setupImgLoaders = new HashMap<>();
        initialize();
    }

    private void initialize() throws IOException {
        // Fetch dimensions and chunk sizes from Zarr metadata
        int[][] zarrDimensions = zarrLoader.getDimensions();
        int[][] chunkSizes = zarrLoader.getChunkSizes();
        this.gridSizes = new int[zarrDimensions.length];

        mipmapDimensions = new long[gridSizes.length][];
        final double[][] resolutions = new double[gridSizes.length][];
        final int[][] subdivisions = new int[gridSizes.length][];
        final AffineTransform3D[] transforms = new AffineTransform3D[gridSizes.length];

        for (int level = 0; level < zarrDimensions.length; level++) {
            int[] dim = zarrDimensions[level];
            int[] chunkSize = chunkSizes[level];
            mipmapDimensions[level] = new long[]{dim[0], dim[1], dim[2]};
            gridSizes[level] = Math.max(dim[0], Math.max(dim[1], dim[2])); // Use the maximum dimension for grid size

            resolutions[level] = new double[]{
                    Math.max(1.0 / (1 << level), 0.5),
                    Math.max(1.0 / (1 << level), 0.5),
                    Math.max(1.0 / (1 << level), 0.5)
            };

            subdivisions[level] = new int[]{chunkSize[0], chunkSize[1], chunkSize[2]}; // Use chunk size for cell dimensions

            transforms[level] = new AffineTransform3D();
            transforms[level].scale(resolutions[level][0], resolutions[level][1], resolutions[level][2]);
        }

        mipmapInfo = new MipmapInfo(resolutions, transforms, subdivisions);
        cache = new VolatileGlobalCellCache(gridSizes.length, 1);

        for (int setupId = 0; setupId < 1; setupId++) {
            setupImgLoaders.put(setupId, new SetupImgLoader(setupId));
        }
    }

    protected <T extends NativeType<T>> VolatileCachedCellImg<T, VolatileShortArray> prepareCachedImage(final ViewLevelId id, final LoadingStrategy loadingStrategy, final T type) {
        final int level = id.getLevel();
        final long[] dimensions = mipmapDimensions[level];
        final int[] cellDimensions = mipmapInfo.getSubdivisions()[level];
        final CellGrid grid = new CellGrid(dimensions, cellDimensions);

        final int priority = mipmapInfo.getMaxLevel() - level;
        final CacheHints cacheHints = new CacheHints(loadingStrategy, priority, false);
        return cache.createImg(grid, id.getTimePointId(), id.getViewSetupId(), level, cacheHints, new CacheArrayLoader<VolatileShortArray>() {
            @Override
            public VolatileShortArray loadArray(int timepoint, int setup, int level, int[] cellDims, long[] cellMin) throws InterruptedException {
                try {
                    return zarrLoader.loadData(level, cellDims, cellMin);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int getBytesPerElement() {
                return 2; // Each element is 2 bytes (16 bits)
            }
        }, type);
    }

    @Override
    public CacheControl getCacheControl() {
        return cache;
    }

    @Override
    public SetupImgLoader getSetupImgLoader(final int setupId) {
        return setupImgLoaders.get(setupId);
    }

    public class SetupImgLoader extends AbstractViewerSetupImgLoader<UnsignedShortType, VolatileUnsignedShortType> {
        private final int setupId;

        protected SetupImgLoader(final int setupId) {
            super(new UnsignedShortType(), new VolatileUnsignedShortType());
            this.setupId = setupId;
        }

        @Override
        public RandomAccessibleInterval<UnsignedShortType> getImage(final int timepointId, final int level, final ImgLoaderHint... hints) {
            final ViewLevelId id = new ViewLevelId(timepointId, setupId, level);
            return prepareCachedImage(id, LoadingStrategy.BLOCKING, new UnsignedShortType());
        }

        @Override
        public RandomAccessibleInterval<VolatileUnsignedShortType> getVolatileImage(final int timepointId, final int level, final ImgLoaderHint... hints) {
            final ViewLevelId id = new ViewLevelId(timepointId, setupId, level);
            return prepareCachedImage(id, LoadingStrategy.BUDGETED, new VolatileUnsignedShortType());
        }

        @Override
        public double[][] getMipmapResolutions() {
            return mipmapInfo.getResolutions();
        }

        @Override
        public AffineTransform3D[] getMipmapTransforms() {
            return mipmapInfo.getTransforms();
        }

        @Override
        public int numMipmapLevels() {
            return mipmapInfo.getNumLevels();
        }
    }

    public Dimensions dimensions() {
        // Return dimensions of the highest resolution
        return Intervals.createMinSize(mipmapDimensions[0][0], mipmapDimensions[0][1], mipmapDimensions[0][2]);
    }
}
