package sc.iview.commands.view;

import graphics.scenery.Mesh;
import net.imagej.mesh.Vertex;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.*;
import sc.iview.SciView;
import sc.iview.process.MeshConverter;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_SET_TRANSFER_FUNCTION;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = {@Menu(label = "View", weight = VIEW), //
                @Menu(label = "Display Vertices", weight = VIEW_SET_TRANSFER_FUNCTION)})
public class DisplayVertices implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    // TODO: this should be the way to do this instead of using sciView.activeNode()
//    @Parameter
//    private Mesh mesh;

    @Parameter(type = ItemIO.OUTPUT)
    private Table table;

    @Override
    public void run() {
        if( sciView.getActiveNode() instanceof Mesh ) {
            Mesh scMesh = (Mesh) sciView.getActiveNode();
            net.imagej.mesh.Mesh mesh = MeshConverter.toImageJ(scMesh);

            table = new DefaultGenericTable();

            // we create two columns
            GenericColumn idColumn = new GenericColumn("ID");
            DoubleColumn xColumn = new DoubleColumn("X");
            DoubleColumn yColumn = new DoubleColumn("Y");
            DoubleColumn zColumn = new DoubleColumn("Z");

            for (Vertex v : mesh.vertices()) {
                idColumn.add(v.index());
                xColumn.add(v.x());
                yColumn.add(v.y());
                zColumn.add(v.z());
            }

            table.add(idColumn);
            table.add(xColumn);
            table.add(yColumn);
            table.add(zColumn);
        }
    }
}