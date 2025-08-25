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
package sc.iview.commands.demo.basic

import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.util.ColorRGB
import sc.iview.SciView
import sc.iview.commands.MenuWeights

/**
 * Demo to test custom window sizing API.
 * Shows how to set custom window dimensions for VR or other specific display requirements.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class,
        label = "Custom Window Size Demo",
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
                Menu(label = "Basic", weight = MenuWeights.DEMO_BASIC),
                Menu(label = "Custom Window Size", weight = MenuWeights.DEMO_BASIC_CUSTOM_WINDOW)])
class CustomWindowSizeDemo : Command {
    @Parameter
    private lateinit var sciview: SciView

    @Parameter(label = "Window Width", min = "100", max = "3840", value = "1920")
    private var width: Int = 1920

    @Parameter(label = "Window Height", min = "100", max = "2160", value = "1080")
    private var height: Int = 1080

    override fun run() {
        // Get current window size
        val (currentWidth, currentHeight) = sciview.getWindowSize()
        println("Current window size: ${currentWidth}x${currentHeight}")
        
        // Set new window size
        println("Setting window size to ${width}x${height}...")
        val success = sciview.setWindowSize(width, height)
        
        if (success) {
            println("Window successfully resized to ${width}x${height}")
            
            // Add some demo content to visualize the new dimensions
            sciview.addSphere(
                position = Vector3f(0f, 0f, 0f),
                radius = 1f,
                color = ColorRGB(128, 255, 128)
            ) {
                name = "Center Sphere"
            }
            
            // Add corner markers to show the viewport
            val aspectRatio = width.toFloat() / height.toFloat()
            val markerSize = 0.2f
            
            // Top-left
            sciview.addBox(
                position = Vector3f(-aspectRatio * 2, 2f, -5f),
                size = Vector3f(markerSize, markerSize, markerSize),
                color = ColorRGB(255, 0, 0)
            ) {
                name = "Top-Left Marker"
            }
            
            // Top-right
            sciview.addBox(
                position = Vector3f(aspectRatio * 2, 2f, -5f),
                size = Vector3f(markerSize, markerSize, markerSize),
                color = ColorRGB(0, 255, 0)
            ) {
                name = "Top-Right Marker"
            }
            
            // Bottom-left
            sciview.addBox(
                position = Vector3f(-aspectRatio * 2, -2f, -5f),
                size = Vector3f(markerSize, markerSize, markerSize),
                color = ColorRGB(0, 0, 255)
            ) {
                name = "Bottom-Left Marker"
            }
            
            // Bottom-right
            sciview.addBox(
                position = Vector3f(aspectRatio * 2, -2f, -5f),
                size = Vector3f(markerSize, markerSize, markerSize),
                color = ColorRGB(255, 255, 0)
            ) {
                name = "Bottom-Right Marker"
            }
            
            // Center the camera
            sciview.centerOnScene()
        } else {
            println("Failed to resize window")
        }
    }
}
