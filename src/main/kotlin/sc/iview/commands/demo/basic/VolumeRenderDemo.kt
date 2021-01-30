package sc.iview.commands.demo.basic

import graphics.scenery.volumes.Volume
import io.scif.services.DatasetIOService
import net.imagej.Dataset
import net.imagej.ops.OpService
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator
import net.imglib2.img.Img
import net.imglib2.type.logic.BitType
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.demo.ResourceLoader
import sc.iview.process.MeshConverter
import java.io.IOException
import java.util.HashMap

/**
 * A demo of volume rendering.
 *
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
@Plugin(type = Command::class,
        label = "Volume Render/Isosurface Demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Basic", weight = MenuWeights.DEMO_BASIC),
            Menu(label = "Volume Render/Isosurface", weight = MenuWeights.DEMO_BASIC_VOLUME)])
class VolumeRenderDemo : Command {
    @Parameter
    private lateinit var datasetIO: DatasetIOService

    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter(label = "Show isosurface")
    private var iso: Boolean = true

    override fun run() {
        val cube: Dataset
        cube = try {
            val cubeFile = ResourceLoader.createFile(javaClass, "/cored_cube_var2_8bit.tif")
            datasetIO.open(cubeFile.absolutePath)
        } catch (exc: IOException) {
            log.error(exc)
            return
        }
        val v = sciView.addVolume(cube, floatArrayOf(1f, 1f, 1f)) as Volume
        v.pixelToWorldRatio = 10f
        v.name = "Volume Render Demo"
        v.dirty = true
        v.needsUpdate = true
        if (iso) {
            val isoLevel = 1

            @Suppress("UNCHECKED_CAST")
            val cubeImg = cube.imgPlus.img as Img<UnsignedByteType>
            val bitImg = ops.threshold().apply(cubeImg, UnsignedByteType(isoLevel)) as Img<BitType>
            val m = ops.geom().marchingCubes(bitImg, isoLevel.toDouble(), BitTypeVertexInterpolator())
            val isoSurfaceMesh = MeshConverter.toScenery(m, false, flipWindingOrder = true)
            v.addChild(isoSurfaceMesh)
            isoSurfaceMesh.name = "Volume Render Demo Isosurface"
        }
        sciView.setActiveNode(v)
        sciView.centerOnNode(sciView.activeNode)
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            argmap["iso"] = true
            command.run(VolumeRenderDemo::class.java, true, argmap)
        }
    }
}