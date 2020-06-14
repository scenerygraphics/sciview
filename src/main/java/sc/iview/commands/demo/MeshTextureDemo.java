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

import graphics.scenery.BufferUtils;
import graphics.scenery.Node;
import graphics.scenery.textures.Texture;
import kotlin.Triple;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.joml.Vector3i;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.nio.ByteBuffer;
import java.util.HashMap;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_MESH_TEXTURE;

/**
 * A demo of mesh textures.
 *
 * @author Kyle Harrington
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, label = "Mesh Texture Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Mesh Texture", weight = DEMO_MESH_TEXTURE) })
public class MeshTextureDemo implements Command {

    @Parameter
    private SciView sciView;

    @Override
    public void run() {
        Node msh = sciView.addBox();
        msh.setName( "Mesh Texture Demo" );
        msh.fitInto( 10.0f, true );

        Texture texture = generateTexture();

        msh.getMaterial().getTextures().put( "diffuse", texture );
        //msh.getMaterial().setDoubleSided( true );
        //msh.getMaterial().setNeedsTextureReload( true );

        msh.setNeedsUpdate( true );
        msh.setDirty( true );

        sciView.centerOnNode( sciView.getActiveNode() );
    }

    private static Texture generateTexture() {
        int width = 64;
        int height = 128;

        Vector3i dims = new Vector3i( width, height, 1 );
        int nChannels = 1;

        ByteBuffer bb = BufferUtils.Companion.allocateByte( width * height * nChannels );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                bb.put( ( byte ) ( Math.random() * 255 ) );
            }
        }
        bb.flip();

        return new Texture( dims,
				nChannels,
				new UnsignedByteType(),
				bb,
				new Triple(Texture.RepeatMode.Repeat,
				    Texture.RepeatMode.Repeat,
				    Texture.RepeatMode.ClampToEdge));
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(MeshTextureDemo.class, true, argmap);
    }
}
