package sc.iview.mandelbulb;

import bdv.img.cache.CacheArrayLoader;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

public class MandelbulbCacheArrayLoader implements CacheArrayLoader<VolatileShortArray>
{
    private final int maxIter;
    private final int order;

    // Static variables for grid sizes, base grid size, and desired finest grid size
    public static int[] gridSizes;
    public static int baseGridSize;
    public static int desiredFinestGridSize;

    public MandelbulbCacheArrayLoader(int maxIter, int order)
    {
        this.maxIter = maxIter;
        this.order = order;
    }

    @Override
    public VolatileShortArray loadArray(final int timepoint, final int setup, final int level, final int[] cellDims, final long[] cellMin) throws InterruptedException
    {
        // Generate Mandelbulb for the specific cell region
        final RandomAccessibleInterval<UnsignedShortType> img = generateMandelbulbForCell(cellDims, cellMin, level, maxIter, order);

        // Create a VolatileShortArray to hold the generated data
        final VolatileShortArray shortArray = new VolatileShortArray(cellDims[0] * cellDims[1] * cellDims[2], true);

        // Extract the data into the short array
        final short[] data = shortArray.getCurrentStorageArray();
        Views.flatIterable(img).forEach(pixel -> data[(int) pixel.index().get()] = (short) pixel.get());

        return shortArray;
    }

    @Override
    public int getBytesPerElement()
    {
        return 2; // Each element is 2 bytes (16 bits)
    }

    public static RandomAccessibleInterval<UnsignedShortType> generateMandelbulbForCell(int[] cellDims, long[] cellMin, int level, int maxIter, int order)
    {
        final RandomAccessibleInterval<UnsignedShortType> img = ArrayImgs.unsignedShorts(new long[]{cellDims[0], cellDims[1], cellDims[2]});

        // Calculate the scaling factor based on the desired finest grid size
        double scale = (double) desiredFinestGridSize / gridSizes[level];

        // Calculate center offset for normalization
        double centerOffset = desiredFinestGridSize / 2.0;

        for (long z = 0; z < cellDims[2]; z++)
        {
            for (long y = 0; y < cellDims[1]; y++)
            {
                for (long x = 0; x < cellDims[0]; x++)
                {
                    // Normalize and center coordinates to range from -1 to 1
                    double[] coordinates = new double[]{
                            ((x + cellMin[0]) * scale - centerOffset) / centerOffset,
                            ((y + cellMin[1]) * scale - centerOffset) / centerOffset,
                            ((z + cellMin[2]) * scale - centerOffset) / centerOffset
                    };
                    // int iterations = (int) (( x + y + z ) % 2);
                    int iterations = mandelbulbIter(coordinates, maxIter, order);
                    img.getAt(x, y, z).set((int) (iterations * 65535.0 / maxIter)); // Scale to 16-bit range
                }
            }
        }
        return img;
    }

    private static int mandelbulbIter(double[] coord, int maxIter, int order)
    {
        double x = coord[0];
        double y = coord[1];
        double z = coord[2];
        double xn = 0, yn = 0, zn = 0;
        int iter = 0;
        while (iter < maxIter && xn * xn + yn * yn + zn * zn < 4)
        {
            double r = Math.sqrt(xn * xn + yn * yn + zn * zn);
            double theta = Math.atan2(Math.sqrt(xn * xn + yn * yn), zn);
            double phi = Math.atan2(yn, xn);

            double newR = Math.pow(r, order);
            double newTheta = theta * order;
            double newPhi = phi * order;

            xn = newR * Math.sin(newTheta) * Math.cos(newPhi) + x;
            yn = newR * Math.sin(newTheta) * Math.sin(newPhi) + y;
            zn = newR * Math.cos(newTheta) + z;

            iter++;
        }
        return iter;
    }

    public static ArrayImg<UnsignedShortType, ?> generateFullMandelbulb(int level, int maxIter, int order)
    {
        long[] dimensions = { (long) gridSizes[level], (long) gridSizes[level], (long) gridSizes[level] };

        ArrayImgFactory<UnsignedShortType> factory = new ArrayImgFactory<>(new UnsignedShortType());
        ArrayImg<UnsignedShortType, ?> img = factory.create(dimensions);

        RandomAccess<UnsignedShortType> imgRA = img.randomAccess();
        for (long z = 0; z < dimensions[2]; z++) {
            for (long y = 0; y < dimensions[1]; y++) {
                for (long x = 0; x < dimensions[0]; x++) {
                    double[] coordinates = new double[]{
                            ((double)x / dimensions[0] * 2 - 1),
                            ((double)y / dimensions[1] * 2 - 1),
                            ((double)z / dimensions[2] * 2 - 1)
                    };
                    int iterations = mandelbulbIter(coordinates, maxIter, order);
                    imgRA.setPosition(new long[]{x, y, z});
                    imgRA.get().set((int) (iterations * 65535.0 / maxIter));
                }
            }
        }

        return img;
    }
}
