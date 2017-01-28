package sc.fiji.threed;

import cleargl.GLVector;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.command.Previewable;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.ColorRGB;
import org.scijava.widget.NumberWidget;

import java.util.ArrayList;

import graphics.scenery.Box;
import graphics.scenery.Node;
import graphics.scenery.Sphere;

import static org.scijava.widget.ChoiceWidget.LIST_BOX_STYLE;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: July 2016
 *
 * Todo: If the list of sceneNode changes while this dialog is open, it may not be notified and thus, may cause strange behaviours. Furthermore, refreshing the list of choises does not work. :(
 * Todo: Change the order of the property items. Scene node must be on top, as the user selects here which object to manipulate.
 * Todo: As soon as object selection in Scenery itself works, the node pulldown may be removed entirely.
 *
 */
@Plugin(type = Command.class,
        menuPath = "ThreeDViewer>Edit>Properties",
        initializer = "initValues")
public class SceneNodePropertiesEditor extends InteractiveCommand implements Command
{
    boolean initializing = true;

    @Parameter
    Context context;

    @Parameter
    private LogService log;

    // ToDo: I don't get this to work. Cheers, Robert
    //@Parameter(persist = false, initializer = "initThreeDViewer")
    private ThreeDViewer threeDViewer;

    @Parameter(required = false, style = LIST_BOX_STYLE, callback = "refreshSceneNodeInDialog")
    String sceneNode;

    Node currentSceneNode;

    @Parameter(required = false, callback = "refreshColourInSceneNode")
    ColorRGB colour;

    @Parameter(label = "Position X", style = NumberWidget.SLIDER_STYLE,
            min = "-1.0", max = "1.0", stepSize = "0.1", callback = "refreshPositionXInSceneNode")
    private double positionX = 1;

    @Parameter(label = "Position Y", style = NumberWidget.SLIDER_STYLE,
            min = "-1.0", max = "1.0", stepSize = "0.1", callback = "refreshPositionYInSceneNode")
    private double positionY = 1;

    @Parameter(label = "Position Z", style = NumberWidget.SLIDER_STYLE,
            min = "-1.0", max = "1.0", stepSize = "0.1", callback = "refreshPositionZInSceneNode")
    private double positionZ = 1;

    
    @Parameter
    private UIService uiSrv;

    ArrayList<String> sceneNodeChoises = new ArrayList<String>();
    //MutableModuleItem<String> sceneNodeSelector;


    protected void initValues() {
        System.out.println("called init values");

        threeDViewer = ThreeDViewer.getViewer();


        rebuildSceneObjectChoiseList();
        //formerColour = colour;

        refreshSceneNodeInDialog();
        refreshColourInDialog();

        initializing = false;

    }

    private void rebuildSceneObjectChoiseList()
    {
        initializing = true;
        System.out.println("rebuildSceneObjectChoiseList");
        sceneNodeChoises = new ArrayList<String>();
        int count = 0;
        for (Node sceneNode : ThreeDViewer.getSceneNodes()) {
            sceneNodeChoises.add(makeIdentifier(sceneNode, count));
            count++;
        }

        //sceneNodeSelector.setPersisted(false);
        //sceneNodeSelector.setLabel("Scene node");
        MutableModuleItem<String> sceneNodeSelector = getInfo().getMutableInput("sceneNode", String.class);
        sceneNodeSelector.setChoices(sceneNodeChoises);

        //todo: if currentSceneNode is set, put it here as current item
        sceneNodeSelector.setValue(this, sceneNodeChoises.get(sceneNodeChoises.size() - 1));
        refreshSceneNodeInDialog();

        initializing = false;
    }

    /**
     * find out, which node is currently selected in the dialog.
     */
    private void refreshSceneNodeInDialog()
    {
        String identifier = sceneNode; //sceneNodeSelector.getValue(this);
        currentSceneNode = null;

        int count = 0;
        for (Node sceneNode : ThreeDViewer.getSceneNodes()) {
            if (identifier.equals(makeIdentifier(sceneNode, count)))
            {
                currentSceneNode = sceneNode;
                System.out.println("current node found");
                break;
            }
            count ++;
        }

        // update property fields according to scene node properties
        refreshColourInDialog();

        if (sceneNodeChoises.size() != ThreeDViewer.getSceneNodes().length)
        {
            rebuildSceneObjectChoiseList();
        }

        System.out.println("current node changed");


    }

    private void refreshColourInDialog()
    {
        if (currentSceneNode == null || currentSceneNode.getMaterial() == null || currentSceneNode.getMaterial().getDiffuse() == null)
        {
            return;
        }

        initializing = true;
        GLVector colourVector = currentSceneNode.getMaterial().getDiffuse();
        System.out.println("change internal colour " + currentSceneNode.getName());
        colour = new ColorRGB((int)(colourVector.get(0) * 255), (int)(colourVector.get(1) * 255),(int)(colourVector.get(2) * 255));
        initializing = false;
    }

    // =======================================
    // push changes from the dialog to the scene
    private void refreshColourInSceneNode()
    {
        if (currentSceneNode == null || currentSceneNode.getMaterial() == null || currentSceneNode.getMaterial().getDiffuse() == null)
        {
            return;
        }
        currentSceneNode.getMaterial().setDiffuse(new GLVector((float)colour.getRed() / 255, (float)colour.getGreen() / 255, (float)colour.getBlue() / 255 ));
    }

    private void refreshPositionXInSceneNode()
    {
        if (currentSceneNode == null || initializing)
        {
            System.out.println("cancel move");
            return;
        }
        GLVector position = currentSceneNode.getPosition();
        System.out.println("move to " + positionX);


        position.set(0, (float)(positionX));
        currentSceneNode.setPosition(position);
    }
    
    private void refreshPositionYInSceneNode()
    {
        if (currentSceneNode == null || initializing)
        {
            System.out.println("cancel move");
            return;
        }
        GLVector position = currentSceneNode.getPosition();
        System.out.println("move to " + positionY);


        position.set(1, (float)(positionY));
        currentSceneNode.setPosition(position);
    }
    
    private void refreshPositionZInSceneNode()
    {
        if (currentSceneNode == null || initializing)
        {
            System.out.println("cancel move");
            return;
        }
        GLVector position = currentSceneNode.getPosition();
        System.out.println("move to " + positionZ);


        position.set(2, (float)(positionZ));
        currentSceneNode.setPosition(position);
    }

    private String makeIdentifier(Node sceneNode, int count)
    {
        return "" + sceneNode.getName() + "[" + count + "]";
    }

    /**
     * Nothing happens here, as cancelling the dialog is not possible.
     */
    @Override
    public void cancel()
    {

    }

    /**
     * Nothing is done here, as the refreshing of the objects properties works via the preview call.
     */
    @Override
    public void run()
    {

    }
}
