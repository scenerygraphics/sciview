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
import static sc.iview.commands.MenuWeights.PROCESS_ISOSURFACE;

import net.imagej.ImgPlus;
import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;

@Plugin(type = Command.class, menuRoot = "SciView", //
menu = {@Menu(label = "Process", weight = PROCESS), //
        @Menu(label = "Isosurface", weight = PROCESS_ISOSURFACE)})
public class Isosurface implements Command {

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter
    private ImgPlus<UnsignedByteType> image;

    @Parameter
    private int isoLevel;

    @Override
    public void run() {

        Img<BitType> bitImg = ( Img<BitType> ) ops.threshold().apply( image, new UnsignedByteType( isoLevel ) );

        Mesh m = ops.geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator() );

        sciView.addMesh( m );

    }

}
