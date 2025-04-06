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

/**
 * Extension functions for the Volume class to make data access more convenient.
 */

/**
 * Gets the original RandomAccessibleInterval from this Volume.
 * 
 * This extension function provides direct access to the underlying data without
 * needing to know the specific metadata key.
 * 
 * @return The original RandomAccessibleInterval or null if not found
 */
fun Volume.getOriginalRandomAccessibleInterval(): RandomAccessibleInterval<*>? {
    return VolumeData.getOriginalRandomAccessibleInterval(this)
}

/**
 * Gets the current view of this Volume based on the current timepoint.
 * 
 * This extension function returns a view of the original data at the current timepoint.
 * If the data is not time-dependent (doesn't have dimension 3 as time), 
 * it returns the original data.
 * 
 * @return The current view of the data or null if original data not found
 */
fun Volume.getCurrentView(): RandomAccessibleInterval<*>? {
    return VolumeData.getCurrentView(this)
}
