/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
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
package sc.iview.commands.demo.animation

import graphics.scenery.BoundingGrid
import graphics.scenery.volumes.Volume
import ij.gui.GenericDialog
import net.imglib2.IterableInterval
import net.imglib2.RandomAccess
import net.imglib2.RandomAccessibleInterval
import net.imglib2.Sampler
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.joml.Vector3f
import org.scijava.command.*
import org.scijava.event.EventHandler
import org.scijava.module.Module
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.Button
import org.scijava.widget.NumberWidget
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.event.NodeRemovedEvent
import sc.iview.ui.CustomPropertyUI
import java.util.*

/**
 * Conway's Game of Life in 3D!
 *
 * @author Curtis Rueden
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
                Menu(label = "Animation", weight = MenuWeights.DEMO_ANIMATION),
                Menu(label = "Game of Life 3D", weight = MenuWeights.DEMO_ANIMATION_GOL3D)])
class GameOfLife3D : InteractiveCommand() {
    @Parameter
    private lateinit var sciView: SciView

    @Volatile @Parameter(label = "Starvation threshold", min = "0", max = "26", persist = false, style = "group:Game of Life")
    private var starvation = 5

    @Volatile @Parameter(label = "Birth threshold", min = "0", max = "26", persist = false, style = "group:Game of Life")
    private var birth = 6

    @Volatile @Parameter(label = "Suffocation threshold", min = "0", max = "26", persist = false, style = "group:Game of Life")
    private var suffocation = 9

    @Volatile @Parameter(label = "Connectedness", choices = [SIX, EIGHTEEN, TWENTY_SIX], persist = false, style = "group:Game of Life")
    private var connectedness = TWENTY_SIX

    @Volatile @Parameter(label = "Initial saturation % when randomizing", min = "1", max = "99", style = NumberWidget.SCROLL_BAR_STYLE, persist = false)
    private var saturation = 10

    @Volatile @Parameter(label = "Play speed", min = "1", max="100", style = NumberWidget.SCROLL_BAR_STYLE + ",group:Game of Life", persist = false)
    private var playSpeed: Int = 10

    @Parameter(callback = "iterate")
    private lateinit var iterate: Button

    @Parameter(callback = "randomize")
    private lateinit var randomize: Button

    @Parameter(callback = "play", style = "group:Game of Life")
    private lateinit var play: Button

    @Parameter(callback = "pause", style = "group:Game of Life")
    private lateinit var pause: Button

    @Parameter(label = "Activate roflcopter", style = "group:Game of Life")
    private var roflcopter: Boolean = false

    private val w = 64
    private val h = 64
    private val d = 64

    /**
     * Returns the current Img
     */
    var img: Img<UnsignedByteType>? = null
        private set
    private var name: String = "Life Simulation"
    private var voxelDims: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f)

    /**
     * Returns the scenery volume node.
     */
    var volume: Volume? = null
        private set

    /** Temporary buffer for use while recomputing the image.  */
    private val bits = BooleanArray(w * h * d)
    private var dialog: GenericDialog? = null

    /** Repeatedly iterates the simulation until stopped  */
    fun play() {
        sciView.animate(playSpeed) { iterate() }
    }

    /** Stops the simulation  */
    fun pause() {
        sciView.stopAnimation()
    }

    /** Randomizes a new bit field.  */
    fun randomize() {
        if( img == null )
            img = ArrayImgs.unsignedBytes(w.toLong(), h.toLong(), d.toLong())
        val cursor = img!!.localizingCursor()
        val chance = saturation / 100.0
        while (cursor.hasNext()) {
            val alive = Math.random() <= chance
            cursor.next().set(if (alive) ALIVE else DEAD)
        }
        updateVolume()
    }

    /** Performs one iteration of the game.  */
    fun iterate() {
        val connected = when (connectedness) {
            SIX -> 6
            EIGHTEEN -> 18
            else -> 26
        }

        // compute the new image field
        val access = img!!.randomAccess()

        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val i = z * w * h + y * w + x
                    val n = neighbors(access, x, y, z, connected)
                    access.setPosition(x, 0)
                    access.setPosition(y, 1)
                    access.setPosition(y, 2)
                    if (alive(access)) {
                        // Living cell stays alive within (starvation, suffocation).
                        bits[i] = n in (starvation + 1) until suffocation
                    } else {
                        // New cell forms within [birth, suffocation).
                        bits[i] = n in birth until suffocation
                    }
                }
            }
        }

        // write the new bit field into the image
        val cursor = img!!.localizingCursor()
        while (cursor.hasNext()) {
            cursor.fwd()
            val x = cursor.getIntPosition(0)
            val y = cursor.getIntPosition(1)
            val z = cursor.getIntPosition(2)
            val alive = bits[z * w * h + y * w + x]
            cursor.get().set(if (alive) ALIVE else DEAD)
        }

        updateVolume()
    }

    override fun run() {
        randomize()
    }

    // -- Helper methods --
    private fun neighbors(access: RandomAccess<UnsignedByteType>, x: Int, y: Int, z: Int, connected: Int): Int {
        var n = 0
        // six-connected
        n += value(access, x - 1, y, z)
        n += value(access, x + 1, y, z)
        n += value(access, x, y - 1, z)
        n += value(access, x, y + 1, z)
        n += value(access, x, y, z - 1)
        n += value(access, x, y, z + 1)
        // eighteen-connected
        if (connected >= 18) {
            n += value(access, x - 1, y - 1, z)
            n += value(access, x + 1, y - 1, z)
            n += value(access, x - 1, y + 1, z)
            n += value(access, x + 1, y + 1, z)
            n += value(access, x - 1, y, z - 1)
            n += value(access, x + 1, y, z - 1)
            n += value(access, x - 1, y, z + 1)
            n += value(access, x + 1, y, z + 1)
            n += value(access, x, y - 1, z - 1)
            n += value(access, x, y + 1, z - 1)
            n += value(access, x, y - 1, z + 1)
            n += value(access, x, y + 1, z + 1)
        }
        // twenty-six-connected
        if (connected == 26) {
            n += value(access, x - 1, y - 1, z - 1)
            n += value(access, x + 1, y - 1, z - 1)
            n += value(access, x - 1, y + 1, z - 1)
            n += value(access, x + 1, y + 1, z - 1)
            n += value(access, x - 1, y - 1, z + 1)
            n += value(access, x + 1, y - 1, z + 1)
            n += value(access, x - 1, y + 1, z + 1)
            n += value(access, x + 1, y + 1, z + 1)
        }
        return n
    }

    private fun value(access: RandomAccess<UnsignedByteType>, x: Int, y: Int, z: Int): Int {
        if (x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d) return 0
        access.setPosition(x, 0)
        access.setPosition(y, 1)
        access.setPosition(z, 2)
        return if (alive(access)) 1 else 0
    }

    private fun alive(access: Sampler<UnsignedByteType>): Boolean {
        return access.get().get() == ALIVE
    }

    private var tick: Long = 0
    private fun updateVolume() {
        if (volume == null) {
            name = "Life Simulation"
            voxelDims = floatArrayOf(1f, 1f, 1f)
            volume = sciView.addVolume(img as RandomAccessibleInterval<UnsignedByteType>, name, *voxelDims) {
                val bg = BoundingGrid()
                bg.node = this

                transferFunction.addControlPoint(0.0f, 0.0f)
                transferFunction.addControlPoint(0.4f, 0.3f)
                scale = Vector3f(10.0f, 10.0f, 10.0f)
                name = "Game of Life 3D"
                sciView.centerOnNode(this)
            }

            // NB: Create dynamic metadata lazily.
            volume!!.metadata["sciview-inspector"] = CustomPropertyUI(
                this,
                listOf(
                    "starvation",
                    "suffocation",
                    "birth",
                    "connectedness",
                    "playSpeed",
                    "play",
                    "pause",
                    "roflcopter"
                )
            )
        } else {
            // NB: Name must be unique each time.
            sciView.updateVolume(img as IterableInterval<UnsignedByteType>, name + "-" + ++tick, voxelDims, volume!!)
        }
    }

    /**
     * Stops the animation when the volume node is removed.
     * @param event
     */
    @EventHandler
    private fun onNodeRemoved(event: NodeRemovedEvent) {
        if (event.node === volume) {
            sciView.stopAnimation()
        }
    }

    companion object {
        private const val ALIVE = 255
        private const val DEAD = 16
        private const val SIX = "6-connected"
        private const val EIGHTEEN = "18-connected"
        private const val TWENTY_SIX = "26-connected"
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            command.run(GameOfLife3D::class.java, true, argmap)
        }
    }
}
