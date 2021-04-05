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
package sc.iview.controls.behaviours

import graphics.scenery.Line
import graphics.scenery.Mesh
import graphics.scenery.TextBoard
import graphics.scenery.utils.LazyLogger
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.ui.behaviour.DragBehaviour
import sc.iview.SciView

/**
 * Draws a line on a mouse drag - just like when you draw a line in paint.
 *
 * @author Justin Buerger <burger@mpi-cbg.de>
 */
class Ruler(protected val sciView: SciView): DragBehaviour {

    //line which is to be drawn
    private val line = Line(simple = true)
    //position on the mouse click; start of the line
    private val origin = Vector3f()
    private val finalLength = Vector3f()
    private val logger by LazyLogger()
    private val cam = sciView.camera

    /** Setup the line and delete the old one */
    override fun init(p0: Int, p1: Int) {
        sciView.deleteNode(line)
        origin.set(getMousePositionIn3D(p0, p1))
        line.addPoint(origin)
        sciView.addNode(line)
        sciView.allSceneNodes.forEach {
            if(it.name == "DistanceTextBoard" && it is Mesh) {
                sciView.removeMesh(it)
            }
        }
    }

    /** Drag the line*/
    override fun drag(p0: Int, p1: Int) {
        val position = getMousePositionIn3D(p0, p1)
        line.clearPoints()
        line.addPoint(origin)
        line.addPoint(position)
    }

    /**Finish the line*/
    override fun end(p0: Int, p1: Int) {
        val endPosition = getMousePositionIn3D(p0, p1)
        line.clearPoints()
        line.addPoint(origin)
        line.addPoint(endPosition)
        endPosition.sub(origin, finalLength)
        logger.info("The line is ${finalLength.length()}")
        val board = TextBoard()
        board.text = "Distance: ${finalLength.length()}"
        board.name = "DistanceTextBoard"
        board.transparent = 0
        board.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        board.backgroundColor = Vector4f(1f, 1f, 1f, 1.0f)
        val boardPosition = Vector3f()
        origin.add(endPosition, boardPosition).mul(0.5f)
        board.position = boardPosition.mul(0.5f)
        board.scale = Vector3f(0.3f, 0.3f, 0.3f)
        sciView.addNode(board)
    }

    /** Get the position of your mouse in 3D world coordinates*/
    private fun getMousePositionIn3D(p0: Int, p1: Int): Vector3f {
        val width = cam!!.width
        val height = cam.height
        val posX = (p0 - width / 2.0f) / (width / 2.0f)
        val posY = -1.0f * (p1 - height / 2.0f) / (height / 2.0f)
        val mousePosition = cam.viewportToView(Vector2f(posX, posY))
        val position4D = cam.viewToWorld(mousePosition)
        return Vector3f(position4D.x(), position4D.y(), position4D.z())
    }
}