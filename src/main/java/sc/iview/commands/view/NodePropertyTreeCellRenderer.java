package sc.iview.commands.view;

import cleargl.GLVector;
import graphics.scenery.*;
import graphics.scenery.volumes.Volume;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.util.Map;

/**
 * Class to render Node Property Tree with custom icons, depending on node type.
 *
 * @author Ulrik Guenther
 */
class NodePropertyTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final Icon cameraIcon = getImageIcon("camera.png");
    private static final Icon lightIcon = getImageIcon("light.png");
    private static final Icon meshIcon = getImageIcon("mesh.png");
    private static final Icon nodeIcon = getImageIcon("node.png");
    private static final Icon sceneIcon = getImageIcon("scene.png");
    private static final Icon textIcon = getImageIcon("text.png");
    private static final Icon volumeIcon = getImageIcon("volume.png");

    private Color nodeForeground = null;
    private Color nodeBackground = null;
    private boolean overrideColor = false;

    private static Icon getImageIcon(String name){
        try {
            return new ImageIcon(ImageIO.read(NodePropertyTreeCellRenderer.class.getResourceAsStream(name)).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        } catch (NullPointerException npe) {
            System.err.println("Could not load image " + name + " as it was not found, returning default.");
        } catch (IOException e) {
            System.err.println("Could not load image " + name + " because of IO error, returning default.");
        }

        return (Icon) UIManager.get("Tree.leafIcon");
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
    public static GLVector RGBtoHSL(GLVector rgb) {
        float max = Math.max(rgb.x(), Math.max(rgb.y(), rgb.z()));
        float min = Math.min(rgb.x(), Math.min(rgb.y(), rgb.z()));
        float h = (max + min)/2.0f;
        float s = (max + min)/2.0f;
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

        return new GLVector(h, s, l);
    }

    /**
     * Custom component renderer that puts icons on each cell dependent on Node type, and colors
     * the foreground and background of PointLights accordingly.
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JComponent component = (JComponent) super.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

        Node n = (Node)node.getUserObject();
        overrideColor = false;

        if (n instanceof Camera) {
            setIcon(cameraIcon);
            setOpenIcon(cameraIcon);
            setClosedIcon(cameraIcon);
        } else if(n instanceof Light) {
            setIcon(lightIcon);
            setOpenIcon(lightIcon);
            setClosedIcon(lightIcon);

            // Here, we set the background of the point light to its emission color.
            // First, we convert the emission color of the light to
            // HSL to determine whether a light or dark font color is needed:
            final GLVector emissionColor = ((Light) n).getEmissionColor();
            final Color awtEmissionColor = new Color(
                    emissionColor.x(),
                    emissionColor.y(),
                    emissionColor.z());
            final GLVector hslEmissionColor = RGBtoHSL(emissionColor);


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
            setIcon(textIcon);
            setOpenIcon(textIcon);
            setClosedIcon(textIcon);
        } else if(n instanceof Volume) {
            setIcon(volumeIcon);
            setOpenIcon(volumeIcon);
            setClosedIcon(volumeIcon);
        } else if(n instanceof Mesh) {
            setIcon(meshIcon);
            setOpenIcon(meshIcon);
            setClosedIcon(meshIcon);
        } else if(n instanceof Scene) {
            setIcon(sceneIcon);
            setOpenIcon(sceneIcon);
            setClosedIcon(sceneIcon);
        } else {
            if(!leaf && n == null) {
                setIcon(sceneIcon);
                setOpenIcon(sceneIcon);
                setClosedIcon(sceneIcon);
            } else {
                setIcon(nodeIcon);
                setOpenIcon(nodeIcon);
                setClosedIcon(nodeIcon);
            }
        }

        Font font = component.getFont();
        Map map = font.getAttributes();

        if(selected) {
            map.put(TextAttribute.FONT, font);
            map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
        } else {
            map.put(TextAttribute.FONT, font);
            map.put(TextAttribute.UNDERLINE, -1);
        }

        font = Font.getFont(map);
        component.setFont(font);

        return this;
    }
}
