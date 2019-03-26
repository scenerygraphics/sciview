package sc.iview.commands.view;

import cleargl.GLVector;
import graphics.scenery.*;
import graphics.scenery.volumes.Volume;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.IOException;

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

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(
                tree, value, sel, exp, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Node n = (Node)node.getUserObject();
        if (n instanceof Camera) {
            setIcon(cameraIcon);
            setOpenIcon(cameraIcon);
            setClosedIcon(cameraIcon);
        } else if(n instanceof Light) {
            setIcon(lightIcon);
            setOpenIcon(lightIcon);
            setClosedIcon(lightIcon);

            final GLVector emissionColor = ((Light) n).getEmissionColor();
            setForeground(new Color(
                    emissionColor.x(),
                    emissionColor.y(),
                    emissionColor.z()));
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

        return this;
    }
}
