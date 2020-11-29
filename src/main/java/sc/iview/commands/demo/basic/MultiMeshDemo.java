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
package sc.iview.commands.demo.basic;

import graphics.scenery.Material;
import graphics.scenery.Node;
import net.imagej.mesh.Mesh;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;
import sc.iview.commands.demo.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo of meshes.
 *
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, label = "Multi Mesh Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Basic", weight = DEMO_BASIC), //
                 @Menu(label = "MultiMesh", weight = DEMO_BASIC_MULTIMESH) })
public class MultiMeshDemo implements Command {

    @Parameter
    private int numMeshes;

    @Parameter
    private IOService io;

    @Parameter
    private LogService log;

    @Parameter
    private SciView sciView;

    @Parameter
    private CommandService commandService;

    @Override
    public void run() {
        final Mesh m;
        try {
            File meshFile = ResourceLoader.createFile( getClass(), "/WieseRobert_simplified_Cip1.stl" );
            m = (Mesh) io.open( meshFile.getAbsolutePath() );
        }
        catch (IOException exc) {
            log.error( exc );
            return;
        }

        Material mat = new Material();
        mat.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        mat.setDiffuse( new Vector3f( 0.8f, 0.5f, 0.4f ) );
        mat.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        //mat.setDoubleSided( true );

        Random RNG = new Random();

        float shellR = 100;

        ArrayList<Node> meshes = new ArrayList<>();

        for( int k = 0; k < numMeshes; k++ ) {

            Node msh = sciView.addMesh(m);
            msh.setName("Mesh_" + k);

            msh.setPosition( new Vector3f( ( RNG.nextFloat() * shellR - shellR ), ( RNG.nextFloat() * shellR - shellR ), ( RNG.nextFloat() * shellR - shellR ) ) );

            //msh.fitInto( 15.0f, true );

            msh.setMaterial(mat);

            msh.setNeedsUpdate(true);
            msh.setDirty(true);

            meshes.add(msh);
        }

        // Wait for everything to settle before framing the view
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sciView.getFloor().setVisible(false);
        sciView.surroundLighting();

        sciView.centerOnScene();
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(MultiMeshDemo.class, true, argmap);
    }
}
