package sc.iview.test;

import net.imagej.ImageJ;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Triangle;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.ops.geom.geom3d.DefaultMarchingCubes;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.logic.BitType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.junit.BeforeClass;
import org.junit.Test;

import sc.iview.SciView;
import sc.iview.commands.demo.MeshDemo;
import sc.iview.commands.demo.ResourceLoader;
import sc.iview.io.N5;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class N5Test {
    private static final double EPSILON = 10e-12;
	private static Mesh mesh;

	@BeforeClass
	public static void setupBefore()
    {
        mesh = getMesh();
    }

    private static Mesh generateMesh() {
        ImageJ imagej = new ImageJ();

	    // Generate an image
        Img<BitType> img = ArrayImgs.bits(25, 25, 25);

        RandomAccess<BitType> imgAccess = img.randomAccess();
        imgAccess.setPosition(new long[]{12, 12, 12});
        imgAccess.get().set(true);

        // Run marching cubes
        //return imagej.op().geom().marchingCubes(img);


        return (new DefaultMarchingCubes()).calculate(img);
    }

    private static Mesh getMesh() {
        ImageJ imagej = new ImageJ();

        final Mesh m;
        try {
            m = new STLMeshIO()
                .open(ResourceLoader.createFile(
                        MeshDemo.class,
                        "/WieseRobert_simplified_Cip1.stl" ).getAbsolutePath());
            return m;
        }
        catch (IOException exc) {
            imagej.log().error( exc );
            return null;
        }
    }

    @Test
    public void writeReadRealLocalizableListTest() throws IOException {
        Path tmp = Files.createTempDirectory(null);
        tmp.toFile().deleteOnExit();

        N5FSWriter n5w = new N5FSWriter(tmp.toAbsolutePath().toString());
        N5FSReader n5r = new N5FSReader(tmp.toAbsolutePath().toString());

        int numPoints = 100;
        int numDimensions = 17;
        Random rng = new Random(171717);
        List<RealLocalizable> points = new ArrayList<>();
        for( int k = 0; k < numPoints; k++ ) {
            double[] pos = new double[numDimensions];
            for( int d = 0; d < numDimensions; d++ ) {
                pos[d] = rng.nextDouble();
            }
            points.add(new RealPoint(pos));
        }

        N5.save(points, n5w, "testPoints", new int[]{10000,17}, new GzipCompression());
        List<RealLocalizable> resultPoints = N5.openPoints(n5r, "testPoints");

        assertPointsEqual(points, resultPoints);
    }

    @Test
    public void writeReadMeshTest() throws IOException {
        Path tmp = Files.createTempDirectory(null);
        tmp.toFile().deleteOnExit();

        N5FSWriter n5w = new N5FSWriter(tmp.toAbsolutePath().toString());
        N5FSReader n5r = new N5FSReader(tmp.toAbsolutePath().toString());

        N5.save(mesh, n5w, "testMesh");
        Mesh nextMesh = N5.openMesh(n5r, "testMesh");

        assertMeshesEqual(mesh, nextMesh);
    }

    private void assertMeshesEqual(Mesh mesh, Mesh result) {
	    assertEquals(mesh.triangles().size(), result.triangles().size());
		final Iterator<Triangle> expectedFacets = mesh.triangles().iterator();
		final Iterator<Triangle> actualFacets = result.triangles().iterator();
		while (expectedFacets.hasNext() && actualFacets.hasNext()) {
			final Triangle expected = expectedFacets.next();
			final Triangle actual = actualFacets.next();
			assertEquals(expected.v0x(), actual.v0x(), EPSILON);
			assertEquals(expected.v0y(), actual.v0y(), EPSILON);
			assertEquals(expected.v0z(), actual.v0z(), EPSILON);
			assertEquals(expected.v1x(), actual.v1x(), EPSILON);
			assertEquals(expected.v1y(), actual.v1y(), EPSILON);
			assertEquals(expected.v1z(), actual.v1z(), EPSILON);
			assertEquals(expected.v2x(), actual.v2x(), EPSILON);
			assertEquals(expected.v2y(), actual.v2y(), EPSILON);
			assertEquals(expected.v2z(), actual.v2z(), EPSILON);
		}
		assertTrue(!expectedFacets.hasNext() && !actualFacets.hasNext());
    }

    private void assertPointsEqual(List<RealLocalizable> points, List<RealLocalizable> result) {
	    assertEquals(points.size(), result.size());
        for( int k = 0; k < points.size(); k++ ) {
            RealLocalizable p = points.get(k);
            RealLocalizable r = result.get(k);
            for( int d = 0; d < p.numDimensions(); d++ ) {
                assertEquals(p.getDoublePosition(d), r.getDoublePosition(d), EPSILON);
            }
        }
    }
}
