package sc.iview.io;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class N5 {
    private static int[] vertexBlockSize = new int[]{600000,3};
    private static int[] triangleBlockSize = new int[]{600000,3};

    public static void save(Mesh mesh, N5Writer n5, String dataset) throws IOException {
        save(mesh, n5, dataset, vertexBlockSize, triangleBlockSize, new GzipCompression());
    }

    /**
     * Save an ImageJ mesh into n5
     * @param mesh
     * @param n5
     * @param dataset
     * @param vertexBlockSize
     * @param triangleBlockSize
     * @param compression
     * @throws IOException
     */
    public static void save(Mesh mesh, N5Writer n5, String dataset, int[] vertexBlockSize, int[] triangleBlockSize, Compression compression) throws IOException {
        Img<DoubleType> vertImg = ArrayImgs.doubles(mesh.vertices().size(), 3);
        Img<LongType> triImg = ArrayImgs.longs(mesh.triangles().size(), 3);

        RandomAccess<DoubleType> vAccess = vertImg.randomAccess();
        long[] vPos = new long[2];
        for( vPos[0] = 0; vPos[0] < mesh.vertices().size(); vPos[0]++ ) {
            for( vPos[1] = 0; vPos[1] < 3; vPos[1]++ ) {
                vAccess.setPosition(vPos);
                if( vPos[1] == 0) vAccess.get().set(mesh.vertices().x(vPos[0]));
                if( vPos[1] == 1) vAccess.get().set(mesh.vertices().y(vPos[0]));
                if( vPos[1] == 2) vAccess.get().set(mesh.vertices().z(vPos[0]));
            }
        }

        RandomAccess<LongType> tAccess = triImg.randomAccess();
        long[] tPos = new long[2];
        for( tPos[0] = 0; tPos[0] < mesh.triangles().size(); tPos[0]++ ) {
            for( tPos[1] = 0; tPos[1] < 3; tPos[1]++ ) {
                tAccess.setPosition(tPos);
                //System.out.println("tri vidx: " + mesh.triangles().vertex0(tPos[0]));
                if( tPos[1] == 0) tAccess.get().set(mesh.triangles().vertex0(tPos[0]));
                if( tPos[1] == 1) tAccess.get().set(mesh.triangles().vertex1(tPos[0]));
                if( tPos[1] == 2) tAccess.get().set(mesh.triangles().vertex2(tPos[0]));
            }
        }

        N5Utils.save(vertImg, n5, dataset + "/vertices", vertexBlockSize, compression);
        N5Utils.save(triImg, n5, dataset + "/triangles", triangleBlockSize, new GzipCompression());
        n5.setAttribute(dataset, "nodeType", "sciview-1.0.0 trimesh");
    }

    public static void save(List<RealLocalizable> points, N5Writer n5, String dataset, int[] vertexBlockSize, Compression compression) throws IOException {
        Img<DoubleType> vertImg = ArrayImgs.doubles(points.size(), points.get(0).numDimensions());

        RandomAccess<DoubleType> vAccess = vertImg.randomAccess();
        long[] vPos = new long[2];
        for( vPos[0] = 0; vPos[0] < points.size(); vPos[0]++ ) {
            for( vPos[1] = 0; vPos[1] < points.get(0).numDimensions(); vPos[1]++ ) {
                vAccess.setPosition(vPos);
                vAccess.get().set(points.get((int) vPos[0]).getDoublePosition((int) vPos[1]));
            }
        }

        N5Utils.save(vertImg, n5, dataset, vertexBlockSize, compression);
        n5.setAttribute(dataset, "nodeType", "sciview-1.0.0 points");
    }

    public static Mesh openMesh(N5Reader n5, String meshDataset) throws IOException {
        Mesh m = new NaiveDoubleMesh();

        // Now load vertices
        RandomAccessibleInterval<DoubleType> vRai = N5Utils.open(n5, meshDataset + "/vertices");
        RandomAccess<DoubleType> vAccess = vRai.randomAccess();
        long[] pos = new long[2];
        for( pos[0] = 0; pos[0] < vRai.dimension(0); pos[0]++ ) {
            double[] vert = new double[3];
            for( pos[1] = 0; pos[1] < 3; pos[1]++ ) {
                vAccess.setPosition(pos);
                vert[(int) pos[1]] = vAccess.get().get();
            }
            m.vertices().add( vert[0], vert[1], vert[2] );
        }

        // Now load triangles
        RandomAccessibleInterval<LongType> tRai = N5Utils.open(n5, meshDataset + "/triangles");

        RandomAccess<LongType> tAccess = tRai.randomAccess();
        for( pos[0] = 0; pos[0] < tRai.dimension(0); pos[0]++ ) {
            long[] tri = new long[3];
            for( pos[1] = 0; pos[1] < 3; pos[1]++ ) {
                tAccess.setPosition(pos);
                tri[(int) pos[1]] = tAccess.get().get();
            }
            m.triangles().add( tri[0], tri[1], tri[2] );
        }

        return m;
    }

    public static List<RealLocalizable> openPoints(N5Reader n5, String pointDataset) throws IOException {
        List<RealLocalizable> points = new ArrayList<>();

        // Now load vertices
        RandomAccessibleInterval<DoubleType> vRai = N5Utils.open(n5, pointDataset);
        RandomAccess<DoubleType> vAccess = vRai.randomAccess();
        long[] pos = new long[2];
        for( pos[0] = 0; pos[0] < vRai.dimension(0); pos[0]++ ) {
            double[] vert = new double[(int) vRai.dimension(1)];
            for( pos[1] = 0; pos[1] < (int) vRai.dimension(1); pos[1]++ ) {
                vAccess.setPosition(pos);
                vert[(int) pos[1]] = vAccess.get().get();
            }
            points.add(new RealPoint(vert));
        }

        return points;
    }
}
