/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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

import graphics.scenery.numerics.Random
import graphics.scenery.utils.RingBuffer
import graphics.scenery.utils.extensions.plus
import graphics.scenery.volumes.BufferedVolume
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.Volume
import mmcorej.CMMCore
import net.imagej.ops.OpService
import net.imagej.units.UnitService
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 */
@Plugin(type = Command::class,
    menuRoot = "SciView",
    menu = [
        Menu(label = "Demo", weight = MenuWeights.DEMO),
        Menu(label = "VolumeMM", weight = MenuWeights.EDIT_ADD_VOLUME)])
class AddVolumeMM : Command {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var ops: OpService

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var unitService: UnitService


    @Parameter(label = "Use voxel dimensions from image", callback = "setVoxelDimensions")
    private var inheritFromImage = true

    @Parameter(label = "Voxel Size X")
    private var voxelWidth = 1.0f

    @Parameter(label = "Voxel Size Y")
    private var voxelHeight = 1.0f

    @Parameter(label = "Voxel Size Z")
    private var voxelDepth = 1.0f

    private val core = CMMCore()

    override fun run() {
        val info = core.versionInfo
        println(info)
        core.loadSystemConfiguration ("C:/Program Files/Micro-Manager-2.0gamma/MMConfig_demo.cfg");
        core.snapImage();
        //val img1 = core.image as ShortArray// returned as a 1D array of signed integers in row-major order
        val width = core.imageWidth;
        val height = core.imageHeight;

        val volume = createVolume()


        sciView.addNode(volume)

        thread {
            var count = 0
            val volumeBuffer = RingBuffer<ByteBuffer>(2) { MemoryUtil.memAlloc((width*height*10*Short.SIZE_BYTES).toInt()) }

            while(sciView.running && !sciView.isClosed) {
                if(volume.metadata["animating"] == true) {
                    volume.lock.withLock {
                        val currentBuffer = volumeBuffer.get()
                        captureStack(currentBuffer.asShortBuffer())

                        volume.addTimepoint("t-${count}", currentBuffer)
                        volume.goToLastTimepoint()
                        volume.purgeFirst(10, 10)

                        count++
                    }
                }

                //Thread.sleep(33L)
            }
        }

    }

    private fun createVolume(): BufferedVolume {
        val volume = Volume.fromBuffer(emptyList(), 512, 512, 10, UnsignedShortType(), sciView.hub)

        volume.name = "volume"
        volume.position = Vector3f(0.0f, 0.0f, 0.0f)
        volume.scale = Vector3f(0.1f,0.1f,10f)
        volume.colormap = Colormap.get("hot")
        volume.pixelToWorldRatio = 0.03f

        with(volume.transferFunction) {
            addControlPoint(0.0f, 0.0f)
            addControlPoint(7000.0f, 1.0f)
        }

        volume.metadata["animating"] = true

        return volume
    }

    private fun captureStack(intoBuffer: ShortBuffer){
        var offset = 0
        (0..9).forEach {
            core.snapImage();
            val sa = core.image as ShortArray
            sa.forEach {
                intoBuffer.put(offset, it)
                offset += 1
            }
        }
    }
}