/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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
package sc.iview.edit;

import static org.scijava.widget.ChoiceWidget.LIST_BOX_STYLE;

import java.util.ArrayList;

import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.ColorRGB;
import org.scijava.widget.NumberWidget;

import sc.iview.SciViewService;

import cleargl.GLVector;
import graphics.scenery.Node;

/**
 * TODO: If the list of sceneNode changes while this dialog is open, it may not
 * be notified and thus, may cause strange behaviours. Furthermore, refreshing
 * the list of choises does not work. :( Todo: Change the order of the property
 * items. Scene node must be on top, as the user selects here which object to
 * manipulate. Todo: As soon as object selection in Scenery itself works, the
 * node pulldown may be removed entirely.
 *
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG Dresden
 */
@Plugin(type = Command.class, menuRoot = "SciView", menuPath = "Edit>Properties", initializer = "initValues")
public class SceneNodePropertiesEditor extends InteractiveCommand {
    boolean initializing = true;

    @Parameter
    private SciViewService sciViewService;

    @Parameter(required = false, style = LIST_BOX_STYLE, callback = "refreshSceneNodeInDialog")
    private String sceneNode;

    @Parameter(required = false, callback = "refreshColourInSceneNode")
    private ColorRGB colour;

    @Parameter(label = "Position X", style = NumberWidget.SLIDER_STYLE, min = "-1.0", max = "1.0", stepSize = "0.1", callback = "refreshPositionXInSceneNode")
    private double positionX = 1;

    @Parameter(label = "Position Y", style = NumberWidget.SLIDER_STYLE, min = "-1.0", max = "1.0", stepSize = "0.1", callback = "refreshPositionYInSceneNode")
    private double positionY = 1;

    @Parameter(label = "Position Z", style = NumberWidget.SLIDER_STYLE, min = "-1.0", max = "1.0", stepSize = "0.1", callback = "refreshPositionZInSceneNode")
    private double positionZ = 1;

    @Parameter
    private UIService uiSrv;

    ArrayList<String> sceneNodeChoices = new ArrayList<>();
    private Node currentSceneNode;

    protected void initValues() {
        rebuildSceneObjectChoiseList();
        //formerColour = colour;

        refreshSceneNodeInDialog();
        refreshColourInDialog();

        initializing = false;

    }

    private void rebuildSceneObjectChoiseList() {
        initializing = true;
        sceneNodeChoices = new ArrayList<>();
        int count = 0;
        for( Node node : sciViewService.getActiveSciView().getSceneNodes() ) {
            sceneNodeChoices.add( makeIdentifier( node, count ) );
            count++;
        }

        MutableModuleItem<String> sceneNodeSelector = getInfo().getMutableInput( "sceneNode", String.class );
        sceneNodeSelector.setChoices( sceneNodeChoices );

        //todo: if currentSceneNode is set, put it here as current item
        sceneNodeSelector.setValue( this, sceneNodeChoices.get( sceneNodeChoices.size() - 1 ) );
        refreshSceneNodeInDialog();

        initializing = false;
    }

    /**
     * find out, which node is currently selected in the dialog.
     */
    private void refreshSceneNodeInDialog() {
        String identifier = sceneNode; //sceneNodeSelector.getValue(this);
        currentSceneNode = null;

        int count = 0;
        for( Node node : sciViewService.getActiveSciView().getSceneNodes() ) {
            if( identifier.equals( makeIdentifier( node, count ) ) ) {
                currentSceneNode = node;
                //System.out.println("current node found");
                break;
            }
            count++;
        }

        // update property fields according to scene node properties
        refreshColourInDialog();

        if( sceneNodeChoices.size() != sciViewService.getActiveSciView().getSceneNodes().length ) {
            rebuildSceneObjectChoiseList();
        }
    }

    private void refreshColourInDialog() {
        if( currentSceneNode == null || currentSceneNode.getMaterial() == null ||
            currentSceneNode.getMaterial().getDiffuse() == null ) {
            return;
        }

        initializing = true;
        GLVector colourVector = currentSceneNode.getMaterial().getDiffuse();
        colour = new ColorRGB( ( int ) ( colourVector.get( 0 ) * 255 ), ( int ) ( colourVector.get( 1 ) * 255 ),
                               ( int ) ( colourVector.get( 2 ) * 255 ) );
        initializing = false;
    }

    // =======================================
    // push changes from the dialog to the scene
    private void refreshColourInSceneNode() {
        if( currentSceneNode == null || currentSceneNode.getMaterial() == null ||
            currentSceneNode.getMaterial().getDiffuse() == null ) {
            return;
        }
        currentSceneNode.getMaterial().setDiffuse( new GLVector( ( float ) colour.getRed() / 255,
                                                                 ( float ) colour.getGreen() / 255,
                                                                 ( float ) colour.getBlue() / 255 ) );
    }

    private void refreshPositionXInSceneNode() {
        if( currentSceneNode == null || initializing ) {
            //System.out.println("cancel move");
            return;
        }
        GLVector position = currentSceneNode.getPosition();
        //System.out.println("move to " + positionX);

        position.set( 0, ( float ) ( positionX ) );
        currentSceneNode.setPosition( position );
    }

    private void refreshPositionYInSceneNode() {
        if( currentSceneNode == null || initializing ) {
            //System.out.println("cancel move");
            return;
        }
        GLVector position = currentSceneNode.getPosition();
        //System.out.println("move to " + positionY);

        position.set( 1, ( float ) ( positionY ) );
        currentSceneNode.setPosition( position );
    }

    private void refreshPositionZInSceneNode() {
        if( currentSceneNode == null || initializing ) {
            //System.out.println("cancel move");
            return;
        }
        GLVector position = currentSceneNode.getPosition();
        //System.out.println("move to " + positionZ);

        position.set( 2, ( float ) ( positionZ ) );
        currentSceneNode.setPosition( position );
    }

    private String makeIdentifier( Node node, int count ) {
        return "" + node.getName() + "[" + count + "]";
    }

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    @Override
    public void cancel() {

    }

    /**
     * Nothing is done here, as the refreshing of the objects properties works via
     * the preview call.
     */
    @Override
    public void run() {

    }
}
