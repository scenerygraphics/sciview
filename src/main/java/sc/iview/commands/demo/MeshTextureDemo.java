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

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_MESH_TEXTURE;

import java.nio.ByteBuffer;

import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import graphics.scenery.BufferUtils;
import graphics.scenery.GenericTexture;
import graphics.scenery.Node;

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
        msh.fitInto( 10.0f, true );

        GenericTexture texture = generateGenericTexture();

        msh.getMaterial().getTransferTextures().put( "diffuse", texture );
        msh.getMaterial().getTextures().put( "diffuse", "fromBuffer:diffuse" );
        msh.getMaterial().setDoubleSided( true );
        msh.getMaterial().setNeedsTextureReload( true );

        msh.setNeedsUpdate( true );
        msh.setDirty( true );
    }

    private static GenericTexture generateGenericTexture() {
        int width = 64;
        int height = 128;

        GLVector dims = new GLVector( width, height, 1 );
        int nChannels = 1;

        // TODO: Use BufferUtils, or ByteBuffer.allocateDirect?
        // Whatever we do, should we do the same everywhere?
        ByteBuffer bb = BufferUtils.BufferUtils.allocateByte( width * height * nChannels );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                bb.put( ( byte ) ( Math.random() * 255 ) );
            }
        }
        bb.flip();

        return new GenericTexture( "neverUsed", dims, nChannels, GLTypeEnum.UnsignedByte, bb, true, true, false );
    }
}
