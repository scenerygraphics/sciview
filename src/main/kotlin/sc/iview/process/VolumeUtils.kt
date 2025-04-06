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
package sc.iview.process

import graphics.scenery.volumes.Volume
import net.imglib2.RandomAccessibleInterval
import net.imglib2.view.IntervalView
import net.imglib2.view.Views
import org.scijava.log.LogService

/**
 * Utility methods for working with Volumes
 * 
 * @author You-Name-Here
 */
object VolumeUtils {
    /**
     * Gets the original data source of a Volume as a RandomAccessibleInterval
     *
     * @param volume The volume to get the original data from
     * @return The original RandomAccessibleInterval, or null if not available
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOriginalRandomAccessibleInterval(volume: Volume): RandomAccessibleInterval<T>? {
        return volume.metadata["RandomAccessibleInterval"] as? RandomAccessibleInterval<T>
    }

    /**
     * Gets the current view of a Volume based on the current timepoint
     *
     * @param volume The volume to get the current view from
     * @return An IntervalView representing the current timepoint of the volume, or null if not available
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCurrentView(volume: Volume): IntervalView<T>? {
        val originalRAI = getOriginalRandomAccessibleInterval<T>(volume) ?: return null
        
        // Check if the original data is time-series (has more than 3 dimensions)
        return if (originalRAI.numDimensions() > 3) {
            // Create a view for the current timepoint by fixing the 4th dimension (time)
            Views.hyperSlice(originalRAI, 3, volume.currentTimepoint.toLong()) as IntervalView<T>
        } else {
            // If it's not a time-series, just return the original data as an IntervalView
            Views.interval(originalRAI, originalRAI) as IntervalView<T>
        }
    }
}
