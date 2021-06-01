/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
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

import graphics.scenery.HasGeometry;
import graphics.scenery.Node;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MethodCallException;
import org.scijava.module.MethodRef;
import org.scijava.module.ModuleItem;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.NumberWidget;
import sc.iview.SciView;

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
public class Isosurface<T extends RealType> extends DynamicCommand {

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter(persist = false, callback = "imageChanged")
    private IterableInterval<T> image;

    @Parameter(callback = "isoValueChanged", style = "slider", min = "0", max = "700", stepSize = "1")
    private double isoLevel;

    @Parameter
    private UIService uiService;

    private Node currentMesh;
    private T tmp;

    @Override
    public void run() {
        currentMesh = null;
    }

    public void isoValueChanged() {
        if(currentMesh != null) {
            sciView.deleteNode(currentMesh);
        }
        if(tmp == null) {
            tmp = (T) image.firstElement().createVariable();
        }
        tmp.setReal(isoLevel);
        Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply(image, tmp);

        Mesh m = Meshes.marchingCubes(bitImg);

        currentMesh = sciView.addMesh(m);
        ((HasGeometry)currentMesh).recalculateNormals();
        currentMesh.setScale(new Vector3f(0.001f, 0.001f, 0.001f));
    }

    public void imageChanged() {
        tmp = (T) image.firstElement().createVariable();
        isoValueChanged();
    }

    @Override
    public void cancel() {
        if(currentMesh != null) {
            sciView.deleteNode(currentMesh);
        }
    }
}
