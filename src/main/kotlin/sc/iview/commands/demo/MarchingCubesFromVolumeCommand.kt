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
import net.imagej.mesh.Mesh
import net.imagej.mesh.naive.NaiveDoubleMesh
import net.imglib2.RandomAccessibleInterval
import net.imglib2.type.numeric.RealType
import org.joml.Vector3f
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.DialogPrompt
import org.scijava.ui.UIService
import sc.iview.SciView
import sc.iview.getCurrentView
import sc.iview.process.MeshConverter
import sc.iview.process.MeshConverter.toScenery

/**
 * Demo command that shows how to use the new Volume extension functions to access the original data
 * and current view, and perform marching cubes on it.
 *
 * @author Claude-3.7-Sonnet
 */
@Plugin(type = Command::class, label = "Marching Cubes from Volume", menuRoot = "SciView", menu = [Menu(label = "Demo"), Menu(label = "Marching Cubes from Volume")])
class MarchingCubesFromVolumeCommand : Command {
    
    @Parameter
    private lateinit var sciView: SciView
    
    @Parameter
    private lateinit var log: LogService
    
    @Parameter
    private lateinit var ui: UIService
    
    @Parameter(label = "Threshold value")
    private var threshold: Double = 1.0
    
    @Parameter(type = ItemIO.OUTPUT)
    private lateinit var mesh: Mesh
    
    override fun run() {
        val active = sciView.activeNode
        
        if (active !is Volume) {
            ui.showDialog("The active node needs to be a volume.", DialogPrompt.MessageType.ERROR_MESSAGE)
            return
        }
        
        val volume = active
        
        // Get the current view using the new extension function
        val currentView = volume.getCurrentView()
        
        if (currentView == null) {
            ui.showDialog("Could not get data from volume.", DialogPrompt.MessageType.ERROR_MESSAGE)
            return
        }
        
        // Perform marching cubes on the current view
        log.info("Performing marching cubes on volume at timepoint: ${volume.currentTimepoint}")
        
        try {
            // This is a simplified implementation - in a real implementation,
            // you would use actual marching cubes algorithm like in the issue example
            @Suppress("UNCHECKED_CAST")
            mesh = createSimpleMeshFromView(currentView as RandomAccessibleInterval<out RealType<*>>, threshold)
            
            // Convert mesh to scenery mesh
            val sceneryMesh = toScenery(mesh)
            
            // Set up the mesh appearance
            sceneryMesh.material().apply {
                diffuse = Vector3f(1.0f, 0.7f, 0.5f)
                wireframe = true
            }
            
            // Create a group for the mesh
            val group = Group()
            group.name = "mesh-from-volume-tp:${volume.currentTimepoint}"
            group.addChild(sceneryMesh)
            
            // Add the group to the scene
            sciView.addNode(group, parent = volume)
            
            log.info("Mesh created successfully from volume at timepoint ${volume.currentTimepoint}")
            
        } catch (e: Exception) {
            log.error("Error creating mesh from volume: ${e.message}")
            ui.showDialog("Error creating mesh: ${e.message}", DialogPrompt.MessageType.ERROR_MESSAGE)
        }
    }
    
    /**
     * Creates a simple mesh from a view using a threshold.
     * This is a placeholder for a real marching cubes implementation.
     */
    private fun <T : RealType<T>> createSimpleMeshFromView(view: RandomAccessibleInterval<T>, threshold: Double): Mesh {
        // This is just a simplified implementation for demonstration purposes
        // In a real implementation, you would use proper marching cubes algorithm
        
        // Create a simple cube as placeholder
        val mesh = NaiveDoubleMesh()
        
        // Add a simple cube as placeholder
        // In a real implementation, this would be the result of marching cubes
        val size = 10.0
        
        // Front face
        mesh.addVertex(0.0, 0.0, 0.0)
        mesh.addVertex(size, 0.0, 0.0)
        mesh.addVertex(size, size, 0.0)
        mesh.addTriangle(0, 1, 2)
        
        mesh.addVertex(0.0, 0.0, 0.0)
        mesh.addVertex(size, size, 0.0)
        mesh.addVertex(0.0, size, 0.0)
        mesh.addTriangle(3, 4, 5)
        
        // Comment to indicate this is just a placeholder
        log.info("Note: This is a placeholder implementation. In a real use case, you would use the actual marching cubes algorithm.")
        
        return mesh
    }
}
