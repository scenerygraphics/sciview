package sc.iview.commands.demo.advanced

import graphics.scenery.Group
import graphics.scenery.Icosphere
import graphics.scenery.InstancedNode
import graphics.scenery.ShaderMaterial
import graphics.scenery.utils.extensions.times
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.SciView.Companion.create
import sc.iview.commands.MenuWeights
import sc.iview.commands.demo.animation.ParticleDemo
import java.util.*
import kotlin.streams.asStream

/**
 * A benchmark that compares the conventional node publishing method with instanced geometry.
 * Nodes can either be published separately or cumulatively at the end.
 * Creating the instanced geometry can be run sequentially or in parallel (using a stream over an array).
 *
 * @author Samuel Pantze
 */

@Plugin(
    type = Command::class,
    label = "Instancing Benchmark",
    menuRoot = "SciView",
    menu = [Menu(label = "Demo", weight = MenuWeights.DEMO), Menu(
        label = "Advanced",
        weight = MenuWeights.DEMO_ADVANCED
    ), Menu(label = "Instance Benchmark", weight = MenuWeights.DEMO_ADVANCED_INSTANCE_BENCHMARK)]
)
class InstancingBenchmark : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var sciview: SciView

    @Parameter(label = "Number of instances", min = "1", max = "100000000", stepSize = "1")
    private var numAgents = 1000

    @Parameter(visibility = ItemVisibility.MESSAGE)
    private var message = "<html> <body>" +
            "    <div> The following execution times can be expected for each</div>" +
            "    <div> run type for 1000 instances: </div>" +
            "    <div> - addNode publish all:  ~ 20 seconds </div>" +
            "    <div> - addNode publish once: ~ 1 second </div>" +
            "    <div> - instance sequential:  ~ 0.1 seconds </div>" +
            "    <div> - instance parallel:    &lt; 0.1 seconds </div>" +
            "</body> </html>"

    @Parameter(choices = ["addNode publish all", "addNode publish once", "instanced sequential", "instanced parallel"], label = "Type of benchmark")
    private var benchmarkType = "addNode publish all"

    val rng = Random(17)
    @Parameter(label = "X range", min = "0.1", max = "1000", style = "format:0.0", stepSize = "0.1")
    private var maxX = 10f
    @Parameter(label = "Y range", min = "0.1", max = "1000", style = "format:0.0", stepSize = "0.1")
    private var maxY = 10f
    @Parameter(label = "Z range", min = "0.1", max = "1000", style = "format:0.0", stepSize = "0.1")
    private var maxZ = 10f


    override fun run() {

        log.info("Running instancing benchmark with $numAgents agents and type \"$benchmarkType\".")
        val startTime = System.currentTimeMillis()
        when (benchmarkType) {
            "addNode publish all" -> {runAddNodePublishAll()}
            "addNode publish once" -> {runAddNodePublishOnce()}
            "instanced sequential" -> {runInstancedSequential()}
            "instanced parallel" -> {runInstancedParallel()}
        }
        val endTime = System.currentTimeMillis()
        log.info("Execution time: ${endTime - startTime} milliseconds")
    }

    /** This runs the benchmark using [SciView.addNode] with `activePublish = true`.*/
    private fun runAddNodePublishAll() {

        val parent = Group()
        sciview.addNode(parent)
        for (i in 0 until numAgents) {
            val n = Icosphere(0.1f, 2)
            n.name = "agent_$i"
            val x = rng.nextFloat() * maxX - maxX/2
            val y = rng.nextFloat() * maxY - maxY/2
            val z = rng.nextFloat() * maxZ - maxZ/2
            n.material {
                diffuse = Vector3f(rng.nextFloat(), rng.nextFloat(), i.toFloat() / numAgents.toFloat())
            }
            n.spatial().position = Vector3f(x, y, z)
            sciview.addNode(n, activePublish = true, parent = parent)
        }
    }

    /** This runs the benchmark using [SciView.addNode] with `activePublish = false` and a single
     * scene update at the end.*/
    private fun runAddNodePublishOnce() {

        val parent = Group()
        sciview.addNode(parent)
        for (i in 0 until numAgents) {
            val n = Icosphere(0.1f, 2)
            n.name = "agent_$i"
            val x = rng.nextFloat() * maxX - maxX/2
            val y = rng.nextFloat() * maxY - maxY/2
            val z = rng.nextFloat() * maxZ - maxZ/2
            n.material {
                diffuse = Vector3f(rng.nextFloat(), rng.nextFloat(), i.toFloat() / numAgents.toFloat())
            }
            n.spatial().position = Vector3f(x, y, z)
            sciview.addNode(n, activePublish = false, parent = parent)
        }
        sciview.addNode(parent, activePublish = true)

    }

    /** This runs the benchmark using instanced geometry with a sequential scene population loop. */
    private fun runInstancedSequential() {

        // This creates a sphere with 5cm radius and 10cm height as our parent.
        // It's important to use the custom shaders provided by this class in the material,
        // as the default instanced material does not include color as property. The actual
        // shader types do not need to be given, as Vertex Shader and Fragment Shader are the
        // default shader types scenery is looking for.
        val parent = Icosphere(0.1f, 2)
        parent.setMaterial(ShaderMaterial.fromClass(ParticleDemo::class.java))
        parent.ifMaterial {
            ambient = Vector3f(0.1f, 0f, 0f)
            diffuse = Vector3f(0.8f, 0.7f, 0.7f)
            diffuse = Vector3f(0.05f, 0f, 0f)
            metallic = 0.01f
            roughness = 0.5f
        }
        parent.name = "Agent_Parent"

        // This now creates the template InstancedNode from the cone we've made.
        val sphere = InstancedNode(parent)
        // Here, we add the color as additional instanced property. Instanced properties
        // should be aligned to 4*32bit boundaries, hence the use of Vector4f instead of Vector3f here.
        sphere.instancedProperties["Color"] = { Vector4f(1.0f) }
        sciview.addNode(sphere)

        (0 until numAgents).forEach {
            val n = sphere.addInstance()
            n.name = "agent_$it"
            // This sets up the initial positions of the agents
            val x = rng.nextFloat() * maxX - maxX/2
            val y = rng.nextFloat() * maxY - maxY/2
            val z = rng.nextFloat() * maxZ - maxZ/2
            val col = Vector4f(rng.nextFloat(), rng.nextFloat(), it.toFloat() / numAgents.toFloat(), 1.0f)
            n.instancedProperties["Color"] = { col }
            n.spatial().position = Vector3f(x, y, z)
        }

    }

    /** This runs the benchmark using instanced geometry with a parallel scene population loop. */
    private fun runInstancedParallel() {

        val sphere = Icosphere(0.1f, 2)
        sphere.setMaterial(ShaderMaterial.fromClass(ParticleDemo::class.java))
        sphere.ifMaterial {
            ambient = Vector3f(0.1f, 0f, 0f)
            diffuse = Vector3f(0.8f, 0.7f, 0.7f)
            diffuse = Vector3f(0.05f, 0f, 0f)
            metallic = 0.01f
            roughness = 0.5f
        }
        sphere.name = "Agent_Parent"

        val parent = InstancedNode(sphere)
        parent.instancedProperties["Color"] = { Vector4f(1.0f) }
        sciview.addNode(parent)

        (0 until numAgents).asSequence().asStream().parallel().forEach {
            val n = parent.addInstance()
            n.name = "agent_$it"

            val x = rng.nextFloat() * maxX - maxX/2
            val y = rng.nextFloat() * maxY - maxY/2
            val z = rng.nextFloat() * maxZ - maxZ/2
            val vel = Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) * 0.5f
            val col = Vector4f(rng.nextFloat(), rng.nextFloat(), it.toFloat() / numAgents.toFloat(), 1.0f)
            n.instancedProperties["Color"] = { col }
            n.spatial().position = Vector3f(x, y, z)
        }
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(ParticleDemo::class.java, true, argmap)
        }
    }

}