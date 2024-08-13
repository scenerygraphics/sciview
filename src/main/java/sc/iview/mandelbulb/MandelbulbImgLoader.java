package sc.iview.mandelbulb;

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
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Intervals;

import java.util.HashMap;

import static sc.iview.mandelbulb.MandelbulbCacheArrayLoader.baseGridSize;

public class MandelbulbImgLoader implements ViewerImgLoader {
    private final int[] gridSizes;
    private final int maxIter;
    private final int order;

    private MipmapInfo mipmapInfo;
    private long[][] mipmapDimensions;
    private VolatileGlobalCellCache cache;
    private CacheArrayLoader<VolatileShortArray> loader;
    private final HashMap<Integer, SetupImgLoader> setupImgLoaders;

    public MandelbulbImgLoader(int[] gridSizes, int maxIter, int order) {
        this.gridSizes = gridSizes;
        this.maxIter = maxIter;
        this.order = order;
        this.setupImgLoaders = new HashMap<>();
        initialize();
    }

    private void initialize()
    {
        // Set up mipmap dimensions and info
        mipmapDimensions = new long[gridSizes.length][];
        final double[][] resolutions = new double[gridSizes.length][];
        final int[][] subdivisions = new int[gridSizes.length][];
        final AffineTransform3D[] transforms = new AffineTransform3D[gridSizes.length];

        for (int level = 0; level < gridSizes.length; level++)
        {
            int gridSize = gridSizes[level];
            mipmapDimensions[level] = new long[]{gridSize, gridSize, gridSize};
            resolutions[level] = new double[]{1.0 / (1 << level), 1.0 / (1 << level), 1.0 / (1 << level)};
            subdivisions[level] = new int[]{128, 128, 128}; // arbitrary cell size
            transforms[level] = new AffineTransform3D();
            transforms[level].scale(resolutions[level][0], resolutions[level][1], resolutions[level][2]);
        }

        mipmapInfo = new MipmapInfo(resolutions, transforms, subdivisions);
        loader = new MandelbulbCacheArrayLoader(maxIter, order);
        cache = new VolatileGlobalCellCache(gridSizes.length, 1);

        for (int setupId = 0; setupId < 1; setupId++)
        {
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
        return cache.createImg(grid, id.getTimePointId(), id.getViewSetupId(), level, cacheHints, loader, type);
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
