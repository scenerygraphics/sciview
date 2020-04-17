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
package sc.iview.commands.edit;

import graphics.scenery.*;
import graphics.scenery.volumes.Volume;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.event.EventService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.ColorRGB;
import org.scijava.widget.NumberWidget;
import sc.iview.SciView;
import sc.iview.event.NodeChangedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.scijava.widget.ChoiceWidget.LIST_BOX_STYLE;

/**
 * A command for interactively editing a node's properties.
 * <ul>
 * <li>TODO: If the list of sceneNode changes while this dialog is open, it may
 * not be notified and thus, may cause strange behaviours. Furthermore,
 * refreshing the list of choises does not work. :(</li>
 * <li>Todo: Change the order of the property items. Scene node must be on top,
 * as the user selects here which object to manipulate.</li>
 * <li>Todo: As soon as object selection in Scenery itself works, the node
 * pulldown may be removed entirely.</li>
 * </ul>
 *
 * @author Robert Haase, Scientific Computing Facility, MPI-CBG Dresden
 * @author Curtis Rueden
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
@Plugin(type = Command.class, initializer = "initValues", visible = false)
public class Properties extends InteractiveCommand {

    private static final String PI_NEG = "-3.142";
    private static final String PI_POS = "3.142";

    @Parameter
    private UIService uiSrv;

    @Parameter
    private SciView sciView;

    @Parameter
	private EventService events;

    @Parameter(required = false, style = LIST_BOX_STYLE, callback = "refreshSceneNodeInDialog")
    private String sceneNode;

    @Parameter(callback = "updateNodeProperties")
    private boolean visible;

    @Parameter(required = false, callback = "updateNodeProperties")
    private ColorRGB colour;

	@Parameter(required = false, callback = "updateNodeProperties")
	private boolean active;

    @Parameter(label = "Name", callback = "updateNodeProperties")
    private String name;

    @Parameter(label = "Timepoint", callback = "updateNodeProperties")
    private int timepoint = 0;

    @Parameter(label = "Intensity", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float intensity = 0;

    @Parameter(label = "Position X", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float positionX = 0;

    @Parameter(label = "Position Y", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float positionY = 0;

    @Parameter(label = "Position Z", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float positionZ = 0;

    @Parameter(label = "Scale X", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float scaleX = 1;

    @Parameter(label = "Scale Y", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float scaleY = 1;

    @Parameter(label = "Scale Z", style = NumberWidget.SPINNER_STYLE, //
            stepSize = "0.1", callback = "updateNodeProperties")
    private float scaleZ = 1;

    @Parameter(label = "Rotation Phi", style = NumberWidget.SPINNER_STYLE, //
            min = PI_NEG, max = PI_POS, stepSize = "0.01", callback = "updateNodeProperties")
    private float rotationPhi;

    @Parameter(label = "Rotation Theta", style = NumberWidget.SPINNER_STYLE, //
            min = PI_NEG, max = PI_POS, stepSize = "0.01", callback = "updateNodeProperties")
    private float rotationTheta;

    @Parameter(label = "Rotation Psi", style = NumberWidget.SPINNER_STYLE, //
            min = PI_NEG, max = PI_POS, stepSize = "0.01", callback = "updateNodeProperties")
    private float rotationPsi;

    /* Volume properties */
    @Parameter(label = "Rendering Mode", style = LIST_BOX_STYLE, callback = "updateNodeProperties")
    private String renderingMode;

    @Parameter(label = "AO steps", style = NumberWidget.SPINNER_STYLE, callback = "updateNodeProperties")
    private int occlusionSteps;

    /* Bounding Grid properties */
    @Parameter(label = "Grid Color", callback = "updateNodeProperties")
    private ColorRGB gridColor;

    @Parameter(label = "Ticks only", callback = "updateNodeProperties")
    private boolean ticksOnly;

    /* TextBoard properties */
    @Parameter(label = "Text", callback = "updateNodeProperties")
    private String text;

    @Parameter(label = "Text Color", callback = "updateNodeProperties")
    private ColorRGB fontColor;

    @Parameter(label = "Background Color", callback = "updateNodeProperties")
    private ColorRGB backgroundColor;

    @Parameter(label = "Transparent Background", callback = "updateNodeProperties")
    private boolean transparentBackground;

    private List<Volume.RenderingMethod> renderingModeChoices = Arrays.asList(Volume.RenderingMethod.values());

    boolean fieldsUpdating = true;

    ArrayList<String> sceneNodeChoices = new ArrayList<>();
    private Node currentSceneNode;

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    @Override
    public void cancel() {

    }

    /**
     * Nothing is done here, as the refreshing of the objects properties works via
     * callback methods.
     */
    @Override
    public void run() {

    }

    public void setSceneNode( final Node node ) {
        currentSceneNode = node;
        updateCommandFields();
    }

    protected void initValues() {
        rebuildSceneObjectChoiceList();
        refreshSceneNodeInDialog();
        updateCommandFields();
    }

    private void rebuildSceneObjectChoiceList() {
        fieldsUpdating = true;
        sceneNodeChoices = new ArrayList<>();
        int count = 0;
        // here, we want all nodes of the scene, not excluding PointLights and Cameras
        for( final Node node : sciView.getSceneNodes( n -> true ) ) {
            sceneNodeChoices.add( makeIdentifier( node, count ) );
            count++;
        }

        final MutableModuleItem<String> sceneNodeSelector = getInfo().getMutableInput( "sceneNode", String.class );
        sceneNodeSelector.setChoices( sceneNodeChoices );

        //todo: if currentSceneNode is set, put it here as current item
        sceneNodeSelector.setValue( this, sceneNodeChoices.get( sceneNodeChoices.size() - 1 ) );
        refreshSceneNodeInDialog();

        fieldsUpdating = false;
    }

    /**
     * find out, which node is currently selected in the dialog.
     */
    private void refreshSceneNodeInDialog() {
        final String identifier = sceneNode; //sceneNodeSelector.getValue(this);
        currentSceneNode = null;

        int count = 0;
        for( final Node node : sciView.getSceneNodes( n -> true ) ) {
            if( identifier.equals( makeIdentifier( node, count ) ) ) {
                currentSceneNode = node;
                //System.out.println("current node found");
                break;
            }
            count++;
        }

        // update property fields according to scene node properties
        updateCommandFields();

        if( sceneNodeChoices.size() != sciView.getSceneNodes( n -> true ).length ) {
            rebuildSceneObjectChoiceList();
        }
    }

    private <T> void maybeRemoveInput(final String name, final Class<T> type) {
        try {
            final MutableModuleItem item = getInfo().getMutableInput(name, type);
            if(item == null) {
                return;
            }

            getInfo().removeInput(item);
        } catch(NullPointerException npe) {
            return;
        }
    }

    /** Updates command fields to match current scene node properties. */
    public void updateCommandFields() {
        if( currentSceneNode == null ) return;

        fieldsUpdating = true;

        // update colour
        if( currentSceneNode.getMaterial() != null && currentSceneNode.getMaterial().getDiffuse() != null ) {
            Vector3f colourVector;
            if( currentSceneNode instanceof PointLight ) {
                colourVector = ( ( PointLight ) currentSceneNode ).getEmissionColor();
            } else {
                colourVector = currentSceneNode.getMaterial().getDiffuse();
            }
            colour = new ColorRGB( ( int ) ( colourVector.get( 0 ) * 255 ), //
                                   ( int ) ( colourVector.get( 1 ) * 255 ), //
                                   ( int ) ( colourVector.get( 2 ) * 255 ) );
        }

        // update visibility
        visible = currentSceneNode.getVisible();

        // update position
        final Vector3f position = currentSceneNode.getPosition();
        positionX = position.get( 0 );
        positionY = position.get( 1 );
        positionZ = position.get( 2 );

        // update rotation
        final Vector3f eulerAngles = currentSceneNode.getRotation().getEulerAnglesXYZ(new Vector3f());
        rotationPhi = eulerAngles.x();
        rotationTheta = eulerAngles.y();
        rotationPsi = eulerAngles.z();

        // update scale
        final Vector3f scale = currentSceneNode.getScale();
        scaleX = scale.x();
        scaleY = scale.y();
        scaleZ = scale.z();


        if(currentSceneNode instanceof Volume) {
            final MutableModuleItem<String> renderingModeInput = getInfo().getMutableInput( "renderingMode", String.class );
            final List<String> methods = Arrays.asList(Volume.RenderingMethod.values()).stream().map(method -> method.toString()).collect(Collectors.toList());
            renderingModeInput.setChoices(methods);

            renderingMode = renderingModeChoices.get(Arrays.asList(Volume.RenderingMethod.values()).indexOf(((Volume)currentSceneNode).getRenderingMethod())).toString();
            maybeRemoveInput("colour", ColorRGB.class);
        } else {
            maybeRemoveInput( "renderingMode", String.class );
        }

        if(currentSceneNode instanceof PointLight) {
            intensity = ((PointLight)currentSceneNode).getIntensity();
        } else {
            maybeRemoveInput( "intensity", Float.class );
        }

        if(currentSceneNode instanceof Camera) {
            maybeRemoveInput( "colour", ColorRGB.class );
            maybeRemoveInput( "visible", Boolean.class );
            final Scene scene = currentSceneNode.getScene();

            if(scene != null) {
                active = ( ( Camera ) currentSceneNode ).getActive() && scene.findObserver() == currentSceneNode;
            } else {
                active = ( ( Camera ) currentSceneNode ).getActive();
            }
        } else {
            maybeRemoveInput( "active", Boolean.class );
        }

        if(currentSceneNode instanceof BoundingGrid) {
            gridColor = new ColorRGB((int)((BoundingGrid)currentSceneNode).getGridColor().x() * 255,
                    (int)((BoundingGrid)currentSceneNode).getGridColor().y() * 255,
                    (int)((BoundingGrid)currentSceneNode).getGridColor().z() * 255);

            ticksOnly = ((BoundingGrid)currentSceneNode).getTicksOnly() > 0;
        } else {
            maybeRemoveInput( "gridColor", ColorRGB.class );
            maybeRemoveInput( "ticksOnly", Boolean.class );
        }

        if(currentSceneNode instanceof TextBoard) {
            text = ((TextBoard)currentSceneNode).getText();
            fontColor = new ColorRGB((int)((TextBoard)currentSceneNode).getFontColor().x() * 255,
                    (int)((TextBoard)currentSceneNode).getFontColor().y() * 255,
                    (int)((TextBoard)currentSceneNode).getFontColor().z() * 255);
            backgroundColor = new ColorRGB((int)((TextBoard)currentSceneNode).getBackgroundColor().x() * 255,
                    (int)((TextBoard)currentSceneNode).getBackgroundColor().y() * 255,
                    (int)((TextBoard)currentSceneNode).getBackgroundColor().z() * 255);
            transparentBackground = ((TextBoard)currentSceneNode).getTransparent() > 0;
        } else {
            maybeRemoveInput( "fontColor", ColorRGB.class );
            maybeRemoveInput( "backgroundColor", ColorRGB.class );
            maybeRemoveInput( "transparentBackground", Boolean.class );
            maybeRemoveInput( "text", String.class );
        }

        if(currentSceneNode instanceof graphics.scenery.volumes.Volume) {
            timepoint = ((graphics.scenery.volumes.Volume)currentSceneNode).getCurrentTimepoint();
            getInfo().getMutableInput("timepoint", Integer.class).setMinimumValue(0);
            getInfo().getMutableInput("timepoint", Integer.class).setMaximumValue(((graphics.scenery.volumes.Volume) currentSceneNode).getMaxTimepoint());
        } else {
            maybeRemoveInput("timepoint", Integer.class);
        }

        name = currentSceneNode.getName();

        fieldsUpdating = false;
    }

    /** Updates current scene node properties to match command fields. */
    protected void updateNodeProperties() {
        if( currentSceneNode == null || fieldsUpdating ) return;

        // update visibility
        currentSceneNode.setVisible( visible );

        // update rotation
        currentSceneNode.setRotation( new Quaternionf().rotateXYZ( rotationPhi, //
                                                                     rotationTheta, //
                                                                     rotationPsi ) );

        // update colour
        final Vector3f cVector = new Vector3f( colour.getRed() / 255f, //
                                               colour.getGreen() / 255f, //
                                               colour.getBlue() / 255f );
        if( currentSceneNode instanceof PointLight ) {
            ( ( PointLight ) currentSceneNode ).setEmissionColor( cVector );
        } else {
            final Material material = currentSceneNode.getMaterial();
            if (material != null) material.setDiffuse( cVector );
        }

        // update position
        final Vector3f position = currentSceneNode.getPosition();
        position.x = positionX;
        position.y = positionY;
        position.z = positionZ;
        currentSceneNode.setPosition( position );

        // update scale
        final Vector3f scale = currentSceneNode.getScale();
        scale.x = scaleX;
        scale.y = scaleY;
        scale.z = scaleZ;
        currentSceneNode.setScale(scale);

        currentSceneNode.setName(name);

        if(currentSceneNode instanceof PointLight) {
            ((PointLight) currentSceneNode).setIntensity(intensity);
        }

        if(currentSceneNode instanceof Volume) {
            final int mode = renderingModeChoices.indexOf(renderingMode);

            if(mode != -1) {
                ((Volume) currentSceneNode).setRenderingMethod(Volume.RenderingMethod.values()[mode]);
            }
        }

        if(currentSceneNode instanceof Camera) {
        	final Scene scene = currentSceneNode.getScene();

        	if(active && scene != null) {
				scene.setActiveObserver( ( Camera ) currentSceneNode );
				( ( Camera ) currentSceneNode ).setActive( true );
			}
		}

        if(currentSceneNode instanceof BoundingGrid) {
            int ticks;

            if(ticksOnly) {
                ticks = 1;
            } else {
                ticks = 0;
            }

            ((BoundingGrid)currentSceneNode).setTicksOnly(ticks);

            ((BoundingGrid)currentSceneNode).setGridColor(new Vector3f(gridColor.getRed()/255.0f, gridColor.getGreen()/255.0f, gridColor.getBlue()/255.0f));
        }

        if(currentSceneNode instanceof TextBoard) {
            int transparent;

            if(transparentBackground) {
                transparent = 1;
            } else {
                transparent = 0;
            }

            ((TextBoard)currentSceneNode).setTransparent(transparent);
            ((TextBoard)currentSceneNode).setText(text);
            ((TextBoard)currentSceneNode).setFontColor(new Vector4f(fontColor.getRed()/255.0f, fontColor.getGreen()/255.0f, fontColor.getBlue()/255.0f, 1f));
            ((TextBoard)currentSceneNode).setBackgroundColor(new Vector4f(backgroundColor.getRed()/255.0f, backgroundColor.getGreen()/255.0f, backgroundColor.getBlue()/255.0f, 1f));
        }

        if(currentSceneNode instanceof graphics.scenery.volumes.Volume) {
            ((graphics.scenery.volumes.Volume) currentSceneNode).goToTimePoint(timepoint);
        }

        events.publish( new NodeChangedEvent( currentSceneNode ) );
    }

    private String makeIdentifier( final Node node, final int count ) {
        return "" + node.getName() + "[" + count + "]";
    }

}
