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
package sc.iview.commands.process;

import graphics.scenery.Node;
import graphics.scenery.attribute.geometry.HasGeometry;
import graphics.scenery.volumes.Volume;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import sc.iview.SciView;
import sc.iview.process.MeshConverter;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_ISOSURFACE;

/**
 * Command to create a mesh from the currently open Image
 * @param <T> a RealType
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = {@Menu(label = "Process", weight = PROCESS), //
                @Menu(label = "Isosurface", weight = PROCESS_ISOSURFACE)})
public class Isosurface<T extends RealType> implements Command {

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter(persist = false)
    private IterableInterval<T> image;

    @Parameter
    private double isoLevel;

    @Parameter
    private UIService uiService;

    @Override
    public void run() {
        T tmp = (T) image.firstElement().createVariable();
        tmp.setReal(isoLevel);

        Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply(image, tmp);

        Mesh m = Meshes.marchingCubes(bitImg);

        Volume v = sciView.getVolumeFromImage(image);

        Node scMesh = MeshConverter.toScenery(m);
        if( v != null ) {
            v.addChild(scMesh);
        } else {
            sciView.addNode(scMesh);
        }

        scMesh.ifGeometry( geom -> {
            geom.recalculateNormals();
            return null;
        });
//        scMesh.ifSpatial( spatial -> {
//            spatial.setScale(new Vector3f(0.001f, 0.001f, 0.001f));
//            return null;
//        });
    }

}
