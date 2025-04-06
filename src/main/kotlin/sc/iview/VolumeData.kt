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

import graphics.scenery.volumes.Volume
import net.imglib2.RandomAccessibleInterval
import net.imglib2.view.Views

/**
 * Utility class for accessing and manipulating volume data in SciView.
 *
 * This class provides methods for accessing the original data source and the current view
 * of a volume in SciView, addressing the need to avoid using "magic" metadata keys.
 *
 * @author Claude-3.7-Sonnet
 */
object VolumeData {
    
    /**
     * Gets the original RandomAccessibleInterval from a Volume.
     * 
     * @param volume The volume to get the original data from
     * @return The original RandomAccessibleInterval or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun getOriginalRandomAccessibleInterval(volume: Volume): RandomAccessibleInterval<*>? {
        return volume.metadata["RandomAccessibleInterval"] as? RandomAccessibleInterval<*>
    }
    
    /**
     * Gets the current view of the Volume based on the current timepoint.
     * 
     * This method returns a view of the original data at the current timepoint.
     * If the data is not time-dependent (doesn't have dimension 3 as time), 
     * it returns the original data.
     * 
     * @param volume The volume to get the current view from
     * @return The current view of the data or null if original data not found
     */
    @Suppress("UNCHECKED_CAST")
    fun getCurrentView(volume: Volume): RandomAccessibleInterval<*>? {
        val rai = getOriginalRandomAccessibleInterval(volume) ?: return null
        
        // If the data has more than 3 dimensions, assume the 4th dimension is time
        val currentTimepoint = volume.currentTimepoint
        
        return if (rai.numDimensions() > 3) {
            Views.hyperSlice(rai, 3, currentTimepoint.toLong())
        } else {
            // If data doesn't have a time dimension, return the original data
            rai
        }
    }
}
