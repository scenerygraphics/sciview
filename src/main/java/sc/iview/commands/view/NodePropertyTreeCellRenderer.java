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
package sc.iview.commands.view;

import cleargl.GLVector;
import graphics.scenery.*;
import graphics.scenery.volumes.Volume;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

/**
 * Class to render Node Property Tree with custom icons, depending on node type.
 *
 * @author Ulrik Guenther
 */
class NodePropertyTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final Icon[] cameraIcon = getImageIcons("camera.png");
    private static final Icon[] lightIcon = getImageIcons("light.png");
    private static final Icon[] meshIcon = getImageIcons("mesh.png");
    private static final Icon[] nodeIcon = getImageIcons("node.png");
    private static final Icon[] sceneIcon = getImageIcons("scene.png");
    private static final Icon[] textIcon = getImageIcons("text.png");
    private static final Icon[] volumeIcon = getImageIcons("volume.png");


    private Color nodeBackground = null;
    private boolean overrideColor = false;

    private static Icon[] getImageIcons(String name){
        ImageIcon icon;
        ImageIcon disabledIcon;

        try {
            BufferedImage iconImage = ImageIO.read(NodePropertyTreeCellRenderer.class.getResourceAsStream(name));
            BufferedImage disabledIconImage = ImageIO.read(NodePropertyTreeCellRenderer.class.getResourceAsStream(name));
            icon = new ImageIcon(iconImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH));

            int width = disabledIconImage.getWidth();
            int height = disabledIconImage.getHeight();

            final Graphics2D g2 = disabledIconImage.createGraphics();
            final Line2D l = new Line2D.Float(0.0f, height, width, 0.0f);
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(4));
            g2.draw(l);
            g2.dispose();

            disabledIcon = new ImageIcon(disabledIconImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        } catch (NullPointerException npe) {
            System.err.println("Could not load image " + name + " as it was not found, returning default.");
            icon = (ImageIcon) UIManager.get("Tree.leafIcon");
            disabledIcon = (ImageIcon) UIManager.get("Tree.leafIcon");
        } catch (IOException e) {
            System.err.println("Could not load image " + name + " because of IO error, returning default.");
            icon = (ImageIcon) UIManager.get("Tree.leafIcon");
            disabledIcon = (ImageIcon) UIManager.get("Tree.leafIcon");
        }

        return new Icon[]{icon, disabledIcon};
    }

    public NodePropertyTreeCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Color getBackground() {
        if(overrideColor && nodeBackground != null) {
            return nodeBackground;
        } else {
            return super.getBackground();
        }
    }

    @Override
    public Color getBackgroundNonSelectionColor() {
        if(overrideColor && nodeBackground != null) {
            return nodeBackground;
        } else {
            return super.getBackgroundNonSelectionColor();
        }
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
    public static Vector3f convertRGBtoHSL(Vector3f rgb) {
        float max = Math.max(rgb.x(), Math.max(rgb.y(), rgb.z()));
        float min = Math.min(rgb.x(), Math.min(rgb.y(), rgb.z()));
        float h;
        float s;
        float l = (max + min)/2.0f;

        if(max == min) {
            h = 0.0f;
            s = 0.0f;
        } else {
            float diff = max - min;
            if(l > 0.5f) {
                s = diff / (2 - max - min);
            } else {
                s = diff / (max + min);
            }

            if(max == rgb.x()) {
                h = (rgb.y() - rgb.z()) / diff + (rgb.y() < rgb.z() ? 6.0f : 0.0f);
            } else if(max == rgb.y()) {
                h = (rgb.z() - rgb.x()) / diff + 2.0f;
            } else {
                h = (rgb.x() - rgb.y()) / diff + 4.0f;
            }

            h /= 6.0f;
        }

        return new Vector3f(h, s, l);
    }

    /**
     * Custom component renderer that puts icons on each cell dependent on Node type, and colors
     * the foreground and background of PointLights accordingly.
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel component = (JLabel) super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        boolean active = false;

        Node n = (Node)node.getUserObject();
        overrideColor = false;

        int iconIndex = 0;
        if(n != null && !n.getVisible()) {
            iconIndex = 1;
        }

        if (n instanceof Camera) {
            setIcon(cameraIcon[iconIndex]);
            setOpenIcon(cameraIcon[iconIndex]);
            setClosedIcon(cameraIcon[iconIndex]);

            if(active && n.getScene().findObserver() == n) {
            	setText( n.getName() + " (active)" );
			} else {
            	setText( n.getName() );
			}
        } else if(n instanceof Light) {
            setIcon(lightIcon[iconIndex]);
            setOpenIcon(lightIcon[iconIndex]);
            setClosedIcon(lightIcon[iconIndex]);

            // Here, we set the background of the point light to its emission color.
            // First, we convert the emission color of the light to
            // HSL to determine whether a light or dark font color is needed:
            final Vector3f emissionColor = ((Light) n).getEmissionColor();
            final Color awtEmissionColor = new Color(
                    emissionColor.x(),
                    emissionColor.y(),
                    emissionColor.z());
            final Vector3f hslEmissionColor = convertRGBtoHSL(emissionColor);


            setOpaque(true);
            overrideColor = true;
            nodeBackground = awtEmissionColor;

            // if lightness is below 0.5, we use a light font,
            // if above, a dark font.
            if(hslEmissionColor.z() <= 0.5f) {
                component.setForeground(Color.LIGHT_GRAY);
            } else {
                component.setForeground(Color.BLACK);
            }
        } else if(n instanceof TextBoard) {
            setIcon(textIcon[iconIndex]);
            setOpenIcon(textIcon[iconIndex]);
            setClosedIcon(textIcon[iconIndex]);
        } else if(n instanceof Volume) {
            setIcon(volumeIcon[iconIndex]);
            setOpenIcon(volumeIcon[iconIndex]);
            setClosedIcon(volumeIcon[iconIndex]);
        } else if(n instanceof Mesh) {
            setIcon(meshIcon[iconIndex]);
            setOpenIcon(meshIcon[iconIndex]);
            setClosedIcon(meshIcon[iconIndex]);
        } else if(n instanceof Scene) {
            setIcon(sceneIcon[iconIndex]);
            setOpenIcon(sceneIcon[iconIndex]);
            setClosedIcon(sceneIcon[iconIndex]);
        } else {
            if(!leaf && n == null) {
                setIcon(sceneIcon[iconIndex]);
                setOpenIcon(sceneIcon[iconIndex]);
                setClosedIcon(sceneIcon[iconIndex]);
            } else {
                setIcon(nodeIcon[iconIndex]);
                setOpenIcon(nodeIcon[iconIndex]);
                setClosedIcon(nodeIcon[iconIndex]);
            }
        }

        Font font = component.getFont();
        Map map = font.getAttributes();

		map.put(TextAttribute.FONT, font);
		map.put(TextAttribute.UNDERLINE, -1);

        if(selected) {
            map.put(TextAttribute.FONT, font);
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
        }
        if(active) {
        	map.put(TextAttribute.FONT, font);
			map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		}

        font = Font.getFont(map);
        component.setFont(font);

        return this;
    }
}
