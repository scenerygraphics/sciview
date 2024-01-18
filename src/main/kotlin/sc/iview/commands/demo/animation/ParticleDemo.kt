/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.commands.demo.animation

import graphics.scenery.InstancedNode
import graphics.scenery.Node
import graphics.scenery.ShaderMaterial
import graphics.scenery.primitives.Cone
import graphics.scenery.utils.extensions.plus
import graphics.scenery.utils.extensions.times
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.SciView.Companion.create
import sc.iview.commands.MenuWeights
import java.util.*

/**
 * A demo of particle movement.
 *
 * @author Kyle Harrington
 */
@Plugin(
    type = Command::class,
    label = "Particle Demo",
    menuRoot = "SciView",
    menu = [Menu(label = "Demo", weight = MenuWeights.DEMO), Menu(
        label = "Animation",
        weight = MenuWeights.DEMO_ANIMATION
    ), Menu(label = "Particle", weight = MenuWeights.DEMO_ANIMATION_PARTICLE)]
)
class ParticleDemo : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var sciview: SciView

    @Parameter
    private var numAgents = 1000

    override fun run() {
        val agents = ArrayList<InstancedNode.Instance>()
        val rng = Random(17)
        val dt = 0.5f
        val maxX = 10f
        val maxY = 10f
        val maxZ = 10f
        val maxL2 = maxX * maxX + maxY * maxY + maxZ * maxZ

        // This creates a Cone with 5cm radius and 10cm height as our master.
        // It's important to use the custom shaders provided by this class in the material,
        // as the default instanced material does not include color as property. The actual
        // shader types do not need to be given, as Vertex Shader and Fragment Shader are the
        // default shader types scenery is looking for.
        val cone = Cone(0.05f, 0.1f, 3, Vector3f(0.0f, 0.0f, 1.0f))
        cone.setMaterial(ShaderMaterial.fromClass(ParticleDemo::class.java))
        cone.ifMaterial {
            ambient = Vector3f(0.1f, 0f, 0f)
            diffuse = Vector3f(0.8f, 0.7f, 0.7f)
            diffuse = Vector3f(0.05f, 0f, 0f)
            metallic = 0.01f
            roughness = 0.5f
        }
        cone.name = "Agent_Master"

        // This now creates the template InstancedNode from the cone we've made.
        val master = InstancedNode(cone)
        // Here, we add the color as additional instanced property. Instanced properties
        // should be aligned to 4*32bit boundaries, hence the use of Vector4f instead of Vector3f here.
        master.instancedProperties["Color"] = { Vector4f(1.0f) }
        sciview.addNode(master)

        log.info("Running ParticleDemo with $numAgents agents.")
        for (k in 0 until numAgents) {
            val n = master.addInstance()
            n.name = "agent_$k"

            // This sets up the initial velocities and positions of the agents.
            val x = rng.nextFloat() * maxX
            val y = rng.nextFloat() * maxY
            val z = rng.nextFloat() * maxZ
            val vel = Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat()) * 0.5f
            val col = Vector4f(rng.nextFloat(), rng.nextFloat(), k.toFloat() / numAgents.toFloat(), 1.0f)
            n.instancedProperties["Color"] = { col }
            n.spatial().position = Vector3f(x, y, z)
            faceNodeAlongVelocity(n, vel)

            agents.add(n)
        }
        sciview.animate(30, Thread {
            var vel: Vector3f
            val threadRng = Random()
            for (agent in agents) {
                val pos = agent.spatial().position
                if (pos.lengthSquared() > maxL2) {
                    // Switch velocity to point toward center + some random perturbation
                    val perturb = Vector3f(
                        threadRng.nextFloat() - 0.5f,
                        threadRng.nextFloat() - 0.5f,
                        threadRng.nextFloat() - 0.5f
                    )
                    vel = (-1.0f * pos + perturb).normalize()
                    faceNodeAlongVelocity(agent, vel)
                } else {
                    vel = agent.metadata["velocity"] as Vector3f? ?: Vector3f(0.0f)
                }
                val finalVel = vel
                agent.spatial().position = pos + (Vector3f(finalVel) * dt)
            }
        })
        sciview.floor?.visible = false
        sciview.centerOnNode(agents[0])
    }

    /**
     * Faces a given [Node] [n] along the [velocity] vector.
     */
    private fun faceNodeAlongVelocity(n: Node, velocity: Vector3f) {
        n.metadata["velocity"] = velocity
        val up = Vector3f(0f, 1f, 0f)
        val newRotation = Quaternionf().lookAlong(velocity, up)
        n.spatialOrNull()?.rotation = newRotation
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