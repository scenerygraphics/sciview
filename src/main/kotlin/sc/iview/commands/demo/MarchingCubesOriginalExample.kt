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
package sc.iview.commands.demo

import graphics.scenery.Group
import graphics.scenery.volumes.Volume
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.IntervalView
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.DialogPrompt
import org.scijava.ui.UIService
import sc.iview.SciView
import sc.iview.getCurrentView

/**
 * Example command based on the original example from issue #607.
 * This version uses the new extension function getCurrentView() instead of accessing metadata directly.
 * 
 * Note: This class is for demonstration purposes only and may not compile without additional
 * classes referenced in the original example (MarchingCubesRealType, RemoveDuplicateVertices, etc.)
 * 
 * @author Claude-3.7-Sonnet (based on odinsbane's example)
 */
@Plugin(type = Command::class, label = "Marching Cubes Original Example", menuRoot = "SciView", menu = [Menu(label = "Demo"), Menu(label = "Marching Cubes Original Example")])
class MarchingCubesOriginalExample : Command {
    
    @Parameter
    private lateinit var sciView: SciView
    
    @Parameter
    private lateinit var ui: UIService
    
    override fun run() {
        /*
         * This is a commented version of the original example from issue #607
         * It won't compile without the MarchingCubesRealType and other classes.
         * It's included to demonstrate how the new extension functions would be used.
         */
        
        val active = sciView.activeNode
        
        if (active !is Volume) {
            ui.showDialog("The active node needs to be a volume.", DialogPrompt.MessageType.ERROR_MESSAGE)
            return
        }
        
        val volume = active
        
        // Using the new extension function instead of metadata.get("RandomAccessibleInterval")
        val currentView = volume.getCurrentView() as? IntervalView<UnsignedByteType>
        
        if (currentView == null) {
            ui.showDialog("Could not get current view from volume.", DialogPrompt.MessageType.ERROR_MESSAGE)
            return
        }
        
        // The rest of the implementation would follow as in the original example
        /*
        // Original example code for reference (won't compile here)
        val meshes = MarchingCubesRealType.calculate(currentView, 1)
        val processedMeshes = RemoveDuplicateVertices.calculate(meshes, 0)
        
        val group = Group()
        group.setName("meshes-at:${volume.currentTimepoint}")
        
        for (m in MeshConnectedComponents.iterable(processedMeshes)) {
            val ready = MarchingCubesCheck.convert(m)
            ready.material().setWireframe(true)
            ready.material().setDiffuse(Vector3f(1f, 0.7f, 0.5f))
            group.addChild(ready)
        }
        
        sciView.addNode(group, volume)
        */
        
        // For now, we'll just create an empty group to demonstrate
        val group = Group()
        group.name = "meshes-at:${volume.currentTimepoint}"
        
        // Add a comment to show we would normally process the mesh here
        println("// In a real implementation: Process the currentView with MarchingCubes")
        
        sciView.addNode(group, parent = volume)
    }
}
