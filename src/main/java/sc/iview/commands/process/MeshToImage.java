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
package sc.iview.commands.process;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_MESH_TO_IMAGE;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BitType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import sc.iview.SciView;
import sc.iview.process.MeshConverter;

import graphics.scenery.Mesh;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Process", weight = PROCESS), //
                 @Menu(label = "Mesh To Image", weight = PROCESS_MESH_TO_IMAGE) })
public class MeshToImage implements Command {

    @Parameter
    private OpService ops;

    @Parameter
    private LogService log;

    @Parameter
    private SciView sciView;

    @Parameter
    private int width;

    @Parameter
    private int height;

    @Parameter
    private int depth;

    @Parameter(type = ItemIO.OUTPUT)
    private RandomAccessibleInterval<BitType> img;

    @Override
    public void run() {
        if( sciView.getActiveNode() instanceof Mesh ) {
            Mesh currentMesh = ( Mesh ) sciView.getActiveNode();
            net.imagej.mesh.Mesh ijMesh = MeshConverter.toImageJ( currentMesh );

            img = ops.geom().voxelization( ijMesh, width, height, depth );

        } else {
            log.error( "No active node. Add a mesh to the scene and select it." );
        }

    }

}
