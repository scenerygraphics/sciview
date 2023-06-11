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
package sc.iview.ui

import graphics.scenery.utils.lazyLogger
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JComponent
import kotlin.math.roundToInt

class ProgressPie: JComponent() {
    class Slice(var value: Double, var color: Color)
    private val logger by lazyLogger()

    private var slices = arrayOf(
            Slice(0.0, Color.WHITE), Slice(100.0, Color.LIGHT_GRAY)
    )

    init {
        isVisible = true
    }

    var value = 0.0
        set(value) {
            slices[1].value = value
            slices[0].value = 100.0 - value
            field = value
        }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        drawPie(g, bounds, slices)
    }

    fun drawPie(graphics: Graphics, area: Rectangle, slices: Array<Slice>) {
        val g = graphics.create() as Graphics2D

        var total = 0.0
        for (i in slices.indices) {
            total += slices[i].value
        }
        var curValue = 0.0
        var startAngle: Int
        for (i in slices.indices) {
            // angles for fillArc start at East, so we move back 90 degrees to start at North
            startAngle = (curValue * 360 / total).toInt() + 90
            val arcAngle = (slices[i].value * 360 / total).toInt()
            g.color = slices[i].color

            val relativeSize = 0.75f
            val size = (area.height * relativeSize).roundToInt()
            val centerX = (size*(1.0f-relativeSize)/2.0f).roundToInt()
            val centerY = (size*(1.0f-relativeSize)/2.0f).roundToInt()
            g.fillArc(centerX, centerY, size, size, startAngle, arcAngle)
            curValue += slices[i].value
        }

        graphics.dispose()
    }
}
