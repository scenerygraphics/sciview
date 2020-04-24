package sc.iview.commands.view;

import graphics.scenery.Node;
import sc.iview.SciView;

import javax.swing.tree.DefaultMutableTreeNode;

class SceneryTreeNode extends DefaultMutableTreeNode {
    private final Node node;

    public SceneryTreeNode(final SciView sciView ) {
        this( ( Node ) null );
        for( final Node sceneNode : sciView.getSceneNodes( n -> true ) ) {
            addNode( sceneNode );
        }
    }

    private SceneryTreeNode(final Node node ) {
        super( node );
        this.node = node;
    }

    @Override
    public String toString() {
        return node == null ? "Scene" : node.getName();
    }

    private void addNode( final Node child ) {
        final SceneryTreeNode treeNode = new SceneryTreeNode( child );
        add( treeNode );
        for( final Node n : child.getChildren() ) {
            treeNode.addNode( n );
        }
    }

    public Node getNode() {
        return node;
    }
}
