/*-
 * #%L
 * SciView-backed 3D visualization package for ImageJ.
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

import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.demo.animation.VolumeTimeseriesDemo
import java.util.HashMap

//Based on:
//- https://bsky.app/profile/did:plc:nkef4rvuqmuzudtr7dbnjcrl/post/3lbqegnrbjs2i
//and
//- https://gist.github.com/jni/14f9fbf388b4e129ba30128863e6b9d9


/**
 * SciView example rendering a 3D dataset: x ^ y ^ z
 */
@Plugin(
    type = Command::class,
    label = "XOR 3D Dataset Example",
    menuRoot = "SciView",
    menu = [Menu(label = "Examples"), Menu(label = "3D Dataset Example")]
)
class VolumeXORDemo : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        val dataset = createDataset()
        sciView.addVolume(dataset, "x^y^z Volume", floatArrayOf(0.01f, 0.01f, 0.01f)) {
            pixelToWorldRatio = 10f
            this.geometryOrNull()?.dirty = true
            this.spatialOrNull()?.needsUpdate = true
        }
        sciView.centerOnNode(sciView.activeNode)
    }

    /**
     * Generates a 3D dataset where each voxel value is calculated as x ^ y ^ z.
     */
    private fun createDataset(): Img<UnsignedByteType> {
        val size = 16L
        val dimensions = longArrayOf(size, size, size)
        val img = ArrayImgs.unsignedBytes(*dimensions)

        val cursor = img.localizingCursor()
        while (cursor.hasNext()) {
            cursor.fwd()
            val x = cursor.getLongPosition(0)
            val y = cursor.getLongPosition(1)
            val z = cursor.getLongPosition(2)
            val value = (x.toInt() xor y.toInt() xor z.toInt()) and 0xFF
            cursor.get().set(value)
        }

        return img
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()

            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(VolumeXORDemo::class.java, true, argmap)
        }
    }
}
