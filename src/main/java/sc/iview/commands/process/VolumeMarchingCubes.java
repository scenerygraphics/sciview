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

import graphics.scenery.Group;
import graphics.scenery.Mesh;
import graphics.scenery.Node;
import graphics.scenery.volumes.Volume;
import net.imagej.mesh.MeshConnectedComponents;
import net.imagej.mesh.RemoveDuplicateVertices;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.IntervalView;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.UIService;
import sc.iview.SciView;
import sc.iview.process.MeshConverter;
import sc.iview.process.VolumeExtensionsKt;

import static sc.iview.commands.MenuWeights.PROCESS;
import static sc.iview.commands.MenuWeights.PROCESS_ISOSURFACE;

/**
 * Command to create a mesh from the active Volume using VolumeUtils/VolumeExtensions
 * @param <T> a RealType
 *
 * @author You-Name-Here
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = {@Menu(label = "Process", weight = PROCESS), //
                @Menu(label = "Volume Marching Cubes", weight = PROCESS_ISOSURFACE)})
public class VolumeMarchingCubes implements Command {

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter
    private UIService ui;
    
    @Parameter
    private LogService log;

    @Parameter(label = "Threshold", min = "1", max = "255", stepSize = "1")
    private int threshold = 1;

    @Override
    public void run() {
        Node active = sciView.getActiveNode();
        
        if (!(active instanceof Volume)) {
            ui.showDialog("The active node needs to be a volume.", DialogPrompt.MessageType.ERROR_MESSAGE);
            return;
        }
        
        Volume volume = (Volume) active;
        
        // Use the new extension methods to get the current view of the volume
        IntervalView<UnsignedByteType> view = VolumeExtensionsKt.getCurrentView(volume);
        
        if (view == null) {
            ui.showDialog("Could not access volume data. Make sure this is a valid volume.", 
                    DialogPrompt.MessageType.ERROR_MESSAGE);
            return;
        }
        
        log.info("Creating mesh from volume at timepoint: " + volume.getCurrentTimepoint());
        
        // Use the threshold value to create a binary image
        Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply(view, new UnsignedByteType(threshold));
        
        // Apply marching cubes algorithm
        net.imagej.mesh.Mesh mesh = (net.imagej.mesh.Mesh) ops.geom().marchingCubes(
                bitImg, (double) threshold, new BitTypeVertexInterpolator());
        
        // Process the mesh
        net.imagej.mesh.Mesh meshes = RemoveDuplicateVertices.calculate(mesh, 0);
        
        // Create a group to hold the mesh components
        Group group = new Group();
        group.setName("Mesh from volume at timepoint: " + volume.getCurrentTimepoint());
        
        // Add each connected component as a separate mesh
        for (net.imagej.mesh.Mesh m : MeshConnectedComponents.iterable(meshes)) {
            Mesh ready = MeshConverter.toScenery(m);
            ready.material().setWireframe(true);
            ready.material().setDiffuse(new Vector3f(1f, 0.7f, 0.5f));
            group.addChild(ready);
        }
        
        // Add the group to the scene
        sciView.addNode(group, volume);
    }
}
