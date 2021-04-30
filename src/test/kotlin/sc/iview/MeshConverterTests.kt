package sc.iview

import graphics.scenery.Mesh
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
