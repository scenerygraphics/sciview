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

import com.intellij.ui.components.JBPanel;
import graphics.scenery.Camera;
import graphics.scenery.Node;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import graphics.scenery.Scene;
import net.miginfocom.swing.MigLayout;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.event.EventHandler;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.service.Service;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;
import org.scijava.util.DebugUtils;
import org.scijava.widget.UIComponent;
import sc.iview.SciView;
import sc.iview.commands.edit.Properties;
import sc.iview.commands.help.Help;
import sc.iview.event.NodeActivatedEvent;
import sc.iview.event.NodeAddedEvent;
import sc.iview.event.NodeChangedEvent;
import sc.iview.event.NodeRemovedEvent;
import sc.iview.ui.SwingGroupingInputHarvester;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

/**
 * Interactive UI for visualizing and editing the scene graph.
 *
 * @author Curtis Rueden
 */
public class NodePropertyEditor implements UIComponent<JPanel> {

    private final SciView sciView;

    @Parameter
    private PluginService pluginService;

    @Parameter
    private ModuleService moduleService;

    @Parameter
    private CommandService commandService;

    @Parameter
    private LogService log;

    private JPanel panel;
    private DefaultTreeModel treeModel;
    private JTree tree;
    private JBPanel props;

    public JPanel getProps() {
        return props;
    }

    public JTree getTree() {
        return tree;
    }

    public NodePropertyEditor(final SciView sciView ) {
        this.sciView = sciView;
        sciView.getScijavaContext().inject( this );
    }

    /** Creates and displays a window containing the scene editor. */
    public void show() {
        final JFrame frame = new JFrame( "Node Properties" );
        frame.setLocation(200, 200);

        frame.setContentPane( getComponent() );
        // FIXME: Why doesn't the frame disappear when closed?
        frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
        frame.setSize( 600, 400 );
        frame.setVisible( true );
    }

    @Override
    public JPanel getComponent() {
        if( panel == null ) initPanel();
        return panel;
    }

    @Override
    public Class<JPanel> getComponentType() {
        return JPanel.class;
    }

    @EventHandler
    private void onEvent( final NodeAddedEvent evt ) {
        final Node node = evt.getNode();
        //log.info( "Node added: " + node );
        rebuildTree();
    }

    @EventHandler
    private void onEvent( final NodeRemovedEvent evt ) {
        final Node node = evt.getNode();
        //log.info( "Node removed: " + node );
        rebuildTree();
    }

    @EventHandler
    private void onEvent( final NodeChangedEvent evt ) {
        final Node node = evt.getNode();
        if( node == sciView.getActiveNode() ) {
            updateProperties(sciView.getActiveNode());
        }
    }

    @EventHandler
    private void onEvent( final NodeActivatedEvent evt ) {
        final Node node = evt.getNode();
//        if(node != null) {
//            log.info("Node activated: " + node + " (" + node.getName() + ")");
//        } else {
//            log.info("Node activated: " + node);
//        }
        updateProperties( node );
    }

    /** Initializes {@link #panel}. */
    private synchronized void initPanel() {
        if( panel != null ) return;
        final JPanel p = new JPanel();
        p.setLayout( new BorderLayout() );

        createTree();

        props = new JBPanel();
        props.setLayout( new MigLayout( "inset 0", "[grow,fill]", "[grow,fill]" ) );
        updateProperties( null );

        final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, //
                                                     new JScrollPane( tree ), //
                                                     new JScrollPane( props ) );
        splitPane.setDividerLocation( 200 );
        p.add( splitPane, BorderLayout.CENTER );

        panel = p;
    }

    private void createTree() {
        treeModel = new DefaultTreeModel( new SceneryTreeNode( sciView ) );
        tree = new JTree( treeModel );
        tree.setRootVisible( true );
        tree.setCellRenderer(new NodePropertyTreeCellRenderer());
//        tree.setCellEditor(new NodePropertyTreeCellEditor(sciView));

        tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
        tree.addTreeSelectionListener( e -> {
            final Node sceneNode = sceneNode( e.getNewLeadSelectionPath() );
            sciView.setActiveNode( sceneNode );
            updateProperties( sceneNode );
        } );

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if(e.getClickCount() == 2) {
                    SceneryTreeNode n = (SceneryTreeNode)tree.getLastSelectedPathComponent();
                    if(n == null) {
                        return;
                    }

                    Node node = (Node)n.getUserObject();
                    sciView.setActiveCenteredNode( node );
                } else if(e.getButton() == MouseEvent.BUTTON3) {
                    int x = e.getX();
                    int y = e.getY();
                    JTree tree = (JTree)e.getSource();
                    TreePath path = tree.getPathForLocation(x, y);
                    if (path == null)
                        return;

                    tree.setSelectionPath(path);

                    SceneryTreeNode obj = (SceneryTreeNode)path.getLastPathComponent();

                    JPopupMenu popup = new JPopupMenu();

                    JMenuItem labelItem = new JMenuItem(obj.getNode().getName());
                    labelItem.setEnabled(false);
                    popup.add(labelItem);

                    if(obj.getNode() instanceof Camera) {
                        JMenuItem resetItem = new JMenuItem("Reset camera");
                        resetItem.setForeground(Color.RED);
                        resetItem.addActionListener(l -> {
                            obj.getNode().setPosition(new Vector3f(0.0f));
                            obj.getNode().setRotation(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f));
                        });
                        popup.add(resetItem);
                    }
                    JMenuItem hideShow = new JMenuItem("Hide");
                    if(obj.getNode().getVisible()) {
                        hideShow.setText("Hide");
                    } else {
                        hideShow.setText("Show");
                    }
                    hideShow.addActionListener(l -> {
                        obj.getNode().setVisible(!obj.getNode().getVisible());
                    });
                    popup.add(hideShow);

                    JMenuItem removeItem = new JMenuItem("Remove");
                    removeItem.setForeground(Color.RED);
                    removeItem.addActionListener(l -> {
                        sciView.deleteNode(obj.getNode(), true);
                    });
                    popup.add(removeItem);

                    popup.show(tree, x, y);
                } else {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null && e.getX() / 1.2 < tree.getPathBounds(path).x + 16) {
                        SceneryTreeNode n = (SceneryTreeNode) path.getLastPathComponent();

                        if (n != null) {
                            Node node = n.getNode();
                            if (node != null && !(node instanceof Camera) && !(node instanceof Scene)) {
                                node.setVisible(!node.getVisible());
                                tree.repaint();
                            }
                        }
                        e.consume();
                    }
                }


            }
        });
    }

    private Node currentNode = null;
    private Properties currentProperties = null;

    public Node getCurrentNode() {
        return currentNode;
    }

    private SwingInputPanel inputPanel = null;

    private ReentrantLock updateLock = new ReentrantLock();

    /** Generates a properties panel for the given node. */
	public void updateProperties( final Node sceneNode ) {
		if(sceneNode == null) {
			return;
		}

		try {
			if(updateLock.tryLock() || updateLock.tryLock( 200, TimeUnit.MILLISECONDS )) {

				if(currentNode == sceneNode && currentProperties != null && inputPanel != null) {
					currentProperties.updateCommandFields();
					inputPanel.refresh();

					updateLock.unlock();
					return;
				}

				currentNode = sceneNode;
				// Prepare the Properties command module instance.
				final CommandInfo info = commandService.getCommand( Properties.class );
				final Module module = moduleService.createModule( info );
				resolveInjectedInputs( module );
				module.setInput( "sciView", sciView );
				module.setInput( "sceneNode", sceneNode );
				module.resolveInput( "sciView" );
				module.resolveInput( "sceneNode" );
				final Properties p = (Properties) module.getDelegateObject();
				currentProperties = p;
				p.setSceneNode( sceneNode );

				// Prepare the SwingInputHarvester.
				final PluginInfo<SciJavaPlugin> pluginInfo = pluginService.getPlugin( SwingGroupingInputHarvester.class );
				final SciJavaPlugin pluginInstance = pluginService.createInstance( pluginInfo );
				final SwingGroupingInputHarvester harvester = (SwingGroupingInputHarvester) pluginInstance;
				inputPanel = harvester.createInputPanel();

				// Build the panel.
				try {
					harvester.buildPanel( inputPanel, module );
					updatePropertiesPanel( inputPanel.getComponent() );
				} catch( final ModuleException exc ) {
					log.error( exc );
					final String stackTrace = DebugUtils.getStackTrace( exc );
					final JTextArea textArea = new JTextArea();
					textArea.setText( "<html><pre>" + stackTrace + "</pre>" );
					updatePropertiesPanel( textArea );
				}

				updateLock.unlock();
			}
		} catch ( InterruptedException e ) {
			e.printStackTrace();
		}
	}

    private void updatePropertiesPanel( final Component c ) {
        props.removeAll();

        if( c == null ) {
            final JLabel usageLabel = new JLabel( "<html><em>No node selected.</em><br><br>" +
                    Help.getBasicUsageText(sciView.publicGetInputHandler()) + "</html>" );
            usageLabel.setPreferredSize(new Dimension(300, 100));
            props.add( usageLabel );
        } else {
            props.add( c );
            props.setSize(c.getSize());
        }

        props.validate();
        props.repaint();
    }

    private TreePath find(DefaultMutableTreeNode root, Node n) {
        @SuppressWarnings("unchecked")
        Enumeration<TreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() == n) {
                return new TreePath(node.getPath());
            }
        }
        return null;
    }

    /** Rebuilds the tree to match the state of the scene. */
    public void rebuildTree() {
        final TreePath currentPath = tree.getSelectionPath();
        treeModel.setRoot( new SceneryTreeNode( sciView ) );

//        treeModel.reload();
//        // TODO: retain previously expanded nodes only
//        for( int i = 0; i < tree.getRowCount(); i++ ) {
//            tree.expandRow( i );
//        }
//        updateProperties( sciView.getActiveNode() );

        if(currentPath != null) {
            final Node selectedNode = ((SceneryTreeNode)currentPath.getLastPathComponent()).getNode();
            trySelectNode(selectedNode);
        }
    }

    public void trySelectNode(Node node) {
        final TreePath newPath = find((DefaultMutableTreeNode) treeModel.getRoot(), node);
        if(newPath != null) {
            tree.setSelectionPath(newPath);
            tree.scrollPathToVisible(newPath);
            if(node != sciView.getActiveNode()) {
                updateProperties(node);
            }
        }
    }

    /** Retrieves the scenery node of a given tree node. */
    private Node sceneNode( final TreePath treePath ) {
        if( treePath == null ) return null;
        final Object treeNode = treePath.getLastPathComponent();
        if( !( treeNode instanceof SceneryTreeNode ) ) return null;
        final SceneryTreeNode stn = ( SceneryTreeNode ) treeNode;
        final Object userObject = stn.getUserObject();
        if( !( userObject instanceof Node ) ) return null;
        return ( Node ) userObject;
    }

    /** HACK: Resolve injected {@link Context} and {@link Service} inputs. */
    private void resolveInjectedInputs( final Module module ) {
        for( final ModuleItem<?> input : module.getInfo().inputs() ) {
            final Class<?> type = input.getType();
            if( Context.class.isAssignableFrom( type ) || Service.class.isAssignableFrom( type ) ) {
                module.resolveInput( input.getName() );
            }
        }
    }

}
