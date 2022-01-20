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

import graphics.scenery.*
import graphics.scenery.primitives.TextBoard
import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.Volume
import org.joml.Vector3f
import java.awt.*
import java.awt.font.TextAttribute
import java.awt.geom.Line2D
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

/**
 * Class to render Node Property Tree with custom icons, depending on node type.
 *
 * @author Ulrik Guenther
 */
internal class SwingNodePropertyTreeCellRenderer : DefaultTreeCellRenderer() {
    private var nodeBackground: Color? = null
    private var overrideColor = false

    override fun getBackground(): Color {
        val bg = nodeBackground
        return if (overrideColor && bg != null) {
            bg
        } else {
            super.getBackground() ?: Color.WHITE
        }
    }

    override fun getBackgroundNonSelectionColor(): Color {
        val bg = nodeBackground
        return if (overrideColor && bg != null) {
            bg
        } else {
            super.getBackgroundNonSelectionColor() ?: Color.WHITE
        }
    }

    /**
     * Custom component renderer that puts icons on each cell dependent on Node type, and colors
     * the foreground and background of PointLights accordingly.
     */
    override fun getTreeCellRendererComponent(tree: JTree, value: Any,
                                              selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
        val component = super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus) as JLabel
        val node = value as DefaultMutableTreeNode
        val active = false
        val n = node.userObject as? Node
        overrideColor = false
        component.foreground = Color.BLACK
        var iconIndex = 0
        if (n != null && !n.visible) {
            iconIndex = 1
        }
        if (n is Camera) {
            icon = cameraIcon[iconIndex]
            setOpenIcon(cameraIcon[iconIndex])
            setClosedIcon(cameraIcon[iconIndex])
            text = if (active && n.getScene()!!.findObserver() === n) {
                n.name + " (active)"
            } else {
                n.name
            }
        } else if (n is Light) {
            icon = lightIcon[iconIndex]
            setOpenIcon(lightIcon[iconIndex])
            setClosedIcon(lightIcon[iconIndex])

            // Here, we set the background of the point light to its emission color.
            // First, we convert the emission color of the light to
            // HSL to determine whether a light or dark font color is needed:
            val emissionColor = n.emissionColor
            val awtEmissionColor = Color(
                emissionColor.x(),
                emissionColor.y(),
                emissionColor.z()
            )
            val hslEmissionColor = convertRGBtoHSL(emissionColor)
            isOpaque = true
            overrideColor = true
            nodeBackground = awtEmissionColor

            // if lightness is below 0.5, we use a light font,
            // if above, a dark font.
            if (hslEmissionColor.z() <= 0.5f) {
                component.foreground = Color.LIGHT_GRAY
            } else {
                component.foreground = Color.BLACK
            }
        } else if (n?.children?.firstOrNull() is SlicingPlane) {
            icon = meshIcon[iconIndex]
            setOpenIcon(meshIcon[iconIndex])
            setClosedIcon(meshIcon[iconIndex])

            val emissionColor = n.materialOrNull()?.diffuse ?: Vector3f(0.5f)
            val awtEmissionColor = Color(
                emissionColor.x(),
                emissionColor.y(),
                emissionColor.z()
            )
            val hslEmissionColor = convertRGBtoHSL(emissionColor)
            isOpaque = true
            overrideColor = true
            nodeBackground = awtEmissionColor

            // if lightness is below 0.5, we use a light font,
            // if above, a dark font.
            if (hslEmissionColor.z() <= 0.4f) {
                component.foreground = Color.LIGHT_GRAY
            } else {
                component.foreground = Color.BLACK
            }
        } else if (n is TextBoard) {
            icon = textIcon[iconIndex]
            setOpenIcon(textIcon[iconIndex])
            setClosedIcon(textIcon[iconIndex])
        } else if (n is Volume) {
            icon = volumeIcon[iconIndex]
            setOpenIcon(volumeIcon[iconIndex])
            setClosedIcon(volumeIcon[iconIndex])
        } else if (n is Mesh) {
            icon = meshIcon[iconIndex]
            setOpenIcon(meshIcon[iconIndex])
            setClosedIcon(meshIcon[iconIndex])
        } else if (n is Scene) {
            icon = sceneIcon[iconIndex]
            setOpenIcon(sceneIcon[iconIndex])
            setClosedIcon(sceneIcon[iconIndex])
        } else {
            if (!leaf && n == null) {
                icon = sceneIcon[iconIndex]
                setOpenIcon(sceneIcon[iconIndex])
                setClosedIcon(sceneIcon[iconIndex])
            } else {
                icon = nodeIcon[iconIndex]
                setOpenIcon(nodeIcon[iconIndex])
                setClosedIcon(nodeIcon[iconIndex])
            }
        }

        var font = component.font

        val attributes =  font.attributes
        val map = HashMap<TextAttribute, Any>(attributes.size)
        attributes.forEach { (t, v: Any?) -> if(v != null) { map[t] = v }}

        map[TextAttribute.FONT] = font
        map[TextAttribute.UNDERLINE] = -1
        if (selected) {
            map[TextAttribute.FONT] = font
            map[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_LOW_DOTTED
        }
        if (active) {
            map[TextAttribute.FONT] = font
            map[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_BOLD
        }

        font = Font.getFont(map)
        component.font = font
        return this
    }

    companion object {
        private val cameraIcon = getImageIcons("camera.png")
        private val lightIcon = getImageIcons("light.png")
        private val meshIcon = getImageIcons("mesh.png")
        private val nodeIcon = getImageIcons("node.png")
        private val sceneIcon = getImageIcons("scene.png")
        private val textIcon = getImageIcons("text.png")
        private val volumeIcon = getImageIcons("volume.png")
        private fun getImageIcons(name: String): Array<Icon> {
            var icon: ImageIcon
            var disabledIcon: ImageIcon
            try {
                val iconImage = ImageIO.read(SwingNodePropertyTreeCellRenderer::class.java.getResourceAsStream(name))
                val disabledIconImage = ImageIO.read(SwingNodePropertyTreeCellRenderer::class.java.getResourceAsStream(name))
                icon = ImageIcon(iconImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH))
                val width = disabledIconImage.width
                val height = disabledIconImage.height
                val g2 = disabledIconImage.createGraphics()
                val l: Line2D = Line2D.Float(0.0f, height.toFloat(), width.toFloat(), 0.0f)
                g2.color = Color.RED
                g2.stroke = BasicStroke(4.0f)
                g2.draw(l)
                g2.dispose()
                disabledIcon = ImageIcon(disabledIconImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH))
            } catch (npe: NullPointerException) {
                System.err.println("Could not load image $name as it was not found, returning default.")
                icon = UIManager.get("Tree.leafIcon") as ImageIcon
                disabledIcon = UIManager.get("Tree.leafIcon") as ImageIcon
            } catch (e: IOException) {
                System.err.println("Could not load image $name because of IO error, returning default.")
                icon = UIManager.get("Tree.leafIcon") as ImageIcon
                disabledIcon = UIManager.get("Tree.leafIcon") as ImageIcon
            }
            return arrayOf(icon, disabledIcon)
        }

        /**
         * Converts a GLVector containing an RGB color to a GLVector containing
         * the color converted to HSL space. The RGB colors are assumed to be within [0, 1],
         * which is scenery's convention. Divide by 255 before otherwise.
         *
         * The conversion algorithm follows https://en.wikipedia.org/wiki/HSL_and_HSV#Conversion_RGB_to_HSL/HSV_used_commonly_in_software_programming
         *
         * @param rgb RGB color, with each channel in [0, 1].
         * @return converted color in HSL space
         */
        fun convertRGBtoHSL(rgb: Vector3f): Vector3f {
            val max = Math.max(rgb.x(), Math.max(rgb.y(), rgb.z()))
            val min = Math.min(rgb.x(), Math.min(rgb.y(), rgb.z()))
            var h: Float
            val s: Float
            val l = (max + min) / 2.0f
            if (max == min) {
                h = 0.0f
                s = 0.0f
            } else {
                val diff = max - min
                s = if (l > 0.5f) {
                    diff / (2 - max - min)
                } else {
                    diff / (max + min)
                }
                h = if (max == rgb.x()) {
                    (rgb.y() - rgb.z()) / diff + if (rgb.y() < rgb.z()) 6.0f else 0.0f
                } else if (max == rgb.y()) {
                    (rgb.z() - rgb.x()) / diff + 2.0f
                } else {
                    (rgb.x() - rgb.y()) / diff + 4.0f
                }
                h /= 6.0f
            }
            return Vector3f(h, s, l)
        }
    }

    init {
        isOpaque = true
    }
}
