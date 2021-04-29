package sc.iview.test

import graphics.scenery.Mesh
import graphics.scenery.Sphere
import net.imagej.mesh.naive.NaiveDoubleMesh
import org.junit.Test
import sc.iview.process.MeshConverter
import kotlin.test.assertEquals

class MeshConverterTests {

    @Test
    fun testToScenery() {
        val mesh = Mesh("TestMe")
        val convertedMesh = MeshConverter.toImageJ(mesh)
        val reConvertedMesh = MeshConverter.toScenery(convertedMesh)

        assertEquals(mesh.children, reConvertedMesh.children)
    }
}
