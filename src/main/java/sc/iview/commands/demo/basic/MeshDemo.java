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
package sc.iview.commands.demo.basic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import graphics.scenery.attribute.material.Material;
import net.imglib2.mesh.Mesh;

import net.imglib2.mesh.io.stl.STLMeshIO;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;

import graphics.scenery.Node;
import sc.iview.commands.demo.ResourceLoader;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo of meshes.
 *
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, label = "Mesh Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Basic", weight = DEMO_BASIC), //
                 @Menu(label = "Mesh", weight = DEMO_BASIC_MESH) })
public class MeshDemo implements Command {

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
            m = STLMeshIO.open(meshFile.getAbsolutePath());
        }
        catch (IOException exc) {
            log.error( exc );
            return;
        }

        Node msh = sciView.addMesh( m );
        msh.setName( "Mesh Demo" );

        //msh.fitInto( 15.0f, true );

        msh.ifMaterial( mat -> {
            mat.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
            mat.setDiffuse( new Vector3f( 0.8f, 0.5f, 0.4f ) );
            mat.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );
            return null;
        });


        msh.ifGeometry( geom -> { geom.setDirty(true); return null; });
        msh.ifSpatial( spatial -> { spatial.setNeedsUpdate(true); return null; });

        sciView.getFloor().ifSpatial(spatial -> {
            spatial.setPosition(new Vector3f(0, -25, 0));
            return null;
        });

        sciView.setActiveNode(msh);
        sciView.centerOnNode( sciView.getActiveNode() );
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(MeshDemo.class, true, argmap);
    }
}
