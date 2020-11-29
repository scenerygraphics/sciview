/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
package sc.iview.commands.demo.animation;

import graphics.scenery.*;
import graphics.scenery.backends.ShaderType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo of particle movement.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Particle Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Animation", weight = DEMO_ANIMATION), //
                 @Menu(label = "Particle", weight = DEMO_ANIMATION_PARTICLE) })
public class ParticleDemo implements Command {

    @Parameter
    private IOService io;

    @Parameter
    private LogService log;

    @Parameter
    private SciView sciView;

    @Parameter
    private CommandService commandService;

    @Parameter
    private int numAgents=10;

    @Override
    public void run() {
        List<Node> agents = new ArrayList<>();

        Random rng = new Random(17);

        float dt = 0.5f;

        float maxX = 10;
        float maxY = 10;
        float maxZ = 10;

        float maxL2 = maxX * maxX + maxY * maxY + maxZ * maxZ;

        Node master = new Cone(5, 10, 25, new Vector3f(0,0,1));
        //Material mat = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag");
        List<ShaderType> sList = new ArrayList<>();
        sList.add(ShaderType.VertexShader);
        sList.add(ShaderType.FragmentShader);
        //Material mat = ShaderMaterial.fromClass(ParticleDemo.class, sList);

        Material mat = ShaderMaterial.fromClass(ParticleDemo.class, sList);

        mat.setAmbient(new Vector3f(0.1f, 0f, 0f));
        mat.setDiffuse(new Vector3f(0.8f, 0.7f, 0.7f));
        mat.setDiffuse(new Vector3f(0.05f, 0f, 0f));
        mat.setMetallic(0.01f);
        mat.setRoughness(0.5f);
        master.setMaterial(mat);
        master.setName("Agent_Master");
        master.getInstancedProperties().put("ModelMatrix", master::getModel);
        master.getInstancedProperties().put("Color", () -> new Vector3f(0.5f, 0.5f, 0.5f));
        //master.getInstancedProperties().put("Material", master::getMaterial);
        sciView.addNode(master);

        for( int k = 0; k < numAgents; k++ ) {
            Node n = new graphics.scenery.Mesh();
            n.setName("agent_" + k);
            n.getInstancedProperties().put("ModelMatrix", n::getWorld);

            //n.getInstancedProperties().put("Material", n::getMaterial);

            float x = rng.nextFloat()*maxX;
            float y = rng.nextFloat()*maxY;
            float z = rng.nextFloat()*maxZ;

            Vector3f vel = new Vector3f(rng.nextFloat(),rng.nextFloat(),rng.nextFloat());

            final Vector3f col = new Vector3f(rng.nextFloat(),rng.nextFloat(), ((float) k) / ((float) numAgents));

            n.getInstancedProperties().put("Color", () -> col);
            n.setMaterial(master.getMaterial());

            n.setPosition(new Vector3f(x,y,z));
            faceNodeAlongVelocity(n, vel);

            master.getInstances().add(n);
            //sciView.addNode(n);
            agents.add(n);
        }

        sciView.animate(30, new Thread(() -> {
            Vector3f vel;

            Random threadRng = new Random();
            for( Node agent : agents ) {
                Vector3f pos = agent.getPosition();
                if( pos.lengthSquared() > maxL2 ) {
                    // Switch velocity to point toward center + some random perturbation
                    Vector3f perturb = new Vector3f(threadRng.nextFloat() - 0.5f, threadRng.nextFloat() - 0.5f, threadRng.nextFloat() - 0.5f);
                    vel = pos.mul(-1).add(perturb).normalize();
                    faceNodeAlongVelocity(agent, vel);

                } else {
                    vel = (Vector3f) agent.getMetadata().get("velocity");
                }

                agent.setPosition(pos.add(vel.mul(dt)));
                agent.setNeedsUpdate(true);
            }
        }));

        sciView.getFloor().setVisible(false);
        sciView.centerOnNode( agents.get(0) );
    }

    private void faceNodeAlongVelocity(Node n, Vector3f vel) {
        n.getMetadata().put("velocity",vel);

        Quaternionf newRot = new Quaternionf();
        Vector3f dir = new Vector3f(vel.x(), vel.y(), vel.z());
        Vector3f up = new Vector3f(0f, 1f, 0f);
        newRot.lookAlong(dir, up);
        n.setRotation(newRot);
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(ParticleDemo.class, true, argmap);
    }
}
