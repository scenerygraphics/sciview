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
package sc.iview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the window sizing API.
 *
 * @author Kyle Harrington
 */
class WindowSizingTest {
    
    /**
     * Tests the window sizing functionality including:
     * - Setting initial window size via constructor
     * - Getting current window dimensions
     * - Resizing the window to new dimensions
     * - Handling invalid dimensions appropriately
     * 
     * Note: This test requires a display environment and may be skipped
     * in headless CI environments.
     */
    @Test
    fun testWindowSizing() {
        // Note: This test requires a display environment to run
        // It may fail in headless CI environments
        try {
            val sciview = SciView.create(800, 600)
            
            // Test initial size
            val (initialWidth, initialHeight) = sciview.getWindowSize()
            assertEquals(800, initialWidth, "Initial width should be 800")
            assertEquals(600, initialHeight, "Initial height should be 600")
            
            // Test resizing
            val resizeSuccess = sciview.setWindowSize(1920, 1080)
            assertTrue(resizeSuccess, "Window resize should succeed")
            
            val (newWidth, newHeight) = sciview.getWindowSize()
            assertEquals(1920, newWidth, "New width should be 1920")
            assertEquals(1080, newHeight, "New height should be 1080")
            
            // Test invalid dimensions
            val invalidResize = sciview.setWindowSize(-100, 0)
            assertFalse(invalidResize, "Resize with invalid dimensions should fail")
            
            // Dimensions should remain unchanged after failed resize
            val (unchangedWidth, unchangedHeight) = sciview.getWindowSize()
            assertEquals(1920, unchangedWidth, "Width should remain unchanged after failed resize")
            assertEquals(1080, unchangedHeight, "Height should remain unchanged after failed resize")
            
            sciview.closeWindow()
        } catch (e: java.awt.HeadlessException) {
            // Specific exception for headless environments
            println("Test skipped due to headless environment: ${e.message}")
        } catch (e: UnsupportedOperationException) {
            // May be thrown if display is not available
            println("Test skipped - display not available: ${e.message}")
        }
    }
}
