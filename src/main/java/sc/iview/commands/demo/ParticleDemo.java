/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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
package sc.iview.commands.demo;

import cleargl.GLVector;
import com.jogamp.opengl.math.Quaternion;
import graphics.scenery.*;
import net.imagej.mesh.Mesh;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_MESH;

/**
 * A demo of particle movement.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Particle Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Particle", weight = DEMO_MESH+100) })
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

        Node master = new Cone(5, 10, 25, new GLVector(0,0,1));
        Material mat = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag");
        mat.setAmbient(new GLVector(0.1f, 0f, 0f));
        mat.setDiffuse(new GLVector(0.8f, 0.7f, 0.7f));
        mat.setDiffuse(new GLVector(0.05f, 0f, 0f));
        mat.setMetallic(0.01f);
        mat.setRoughness(0.5f);
        master.setMaterial(mat);
        master.setName("Agent_Master");
        master.getInstancedProperties().put("ModelMatrix", master::getModel);
        sciView.addNode(master);

        for( int k = 0; k < numAgents; k++ ) {
            //Node n = new Cone(5, 10, 25, new GLVector(0,0,1));
            Node n = new graphics.scenery.Mesh();
            n.setName("agent_" + k);
            n.getInstancedProperties().put("ModelMatrix", n::getWorld);

            float x = rng.nextFloat()*maxX;
            float y = rng.nextFloat()*maxY;
            float z = rng.nextFloat()*maxZ;

            GLVector vel = new GLVector(rng.nextFloat(),rng.nextFloat(),rng.nextFloat());

            n.setPosition(new GLVector(x,y,z));
            n.getMetadata().put("velocity",vel);

            Quaternion newRot = new Quaternion();
            float[] dir = new float[]{vel.x(), vel.y(), vel.z()};
            float[] up = new float[]{0f, 1f, 0f};
            newRot.setLookAt(dir, up,
                    new float[3], new float[3], new float[3]).normalize();
            n.setRotation(newRot);

            master.getInstances().add(n);
            //sciView.addNode(n);
            agents.add(n);
        }

        sciView.animate(30, new Thread(() -> {
            GLVector vel;

            Random threadRng = new Random();
            for( Node agent : agents ) {
                GLVector pos = agent.getPosition();
                if( pos.length2() > maxL2 ) {
                    // Switch velocity to point toward center + some random perturbation
                    GLVector perturb = new GLVector(threadRng.nextFloat() - 0.5f, threadRng.nextFloat() - 0.5f, threadRng.nextFloat() - 0.5f);
                    vel = pos.times(-1).plus(perturb).normalize();
                    agent.getMetadata().put("velocity",vel);
                    Quaternion newRot = new Quaternion();
                    float[] dir = new float[]{vel.x(), vel.y(), vel.z()};
                    float[] up = new float[]{0f, 1f, 0f};
                    newRot.setLookAt(dir, up, new float[3], new float[3], new float[3]).normalize();
                    agent.setRotation(newRot);

                } else {
                    vel = (GLVector) agent.getMetadata().get("velocity");
                }

                agent.setPosition(pos.plus(vel.times(dt)));
                agent.setNeedsUpdate(true);
            }
        }));

        sciView.getFloor().setVisible(false);
        sciView.centerOnNode( agents.get(0) );
    }
}
