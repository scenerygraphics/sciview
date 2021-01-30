package sc.iview.commands.view

import graphics.scenery.Mesh
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.table.DefaultGenericTable
import org.scijava.table.DoubleColumn
import org.scijava.table.GenericColumn
import org.scijava.table.Table
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SET_TRANSFER_FUNCTION
import sc.iview.process.MeshConverter

/**
 * Command to display the vertices of the currently active Node as a table.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Display Vertices", weight = VIEW_SET_TRANSFER_FUNCTION)])
class DisplayVertices : Command {
    @Parameter
    private lateinit var sciView: SciView

    // TODO: this should be the way to do this instead of using sciView.activeNode()
    //    @Parameter
    //    private Mesh mesh;

    @Parameter(type = ItemIO.OUTPUT)
    private lateinit var table: Table<*, *>

    override fun run() {
        if (sciView.activeNode is Mesh) {
            val scMesh = sciView.activeNode as Mesh
            val mesh = MeshConverter.toImageJ(scMesh)
            table = DefaultGenericTable()

            // we create two columns
            val idColumn = GenericColumn("ID")
            val xColumn = DoubleColumn("X")
            val yColumn = DoubleColumn("Y")
            val zColumn = DoubleColumn("Z")
            for (v in mesh.vertices()) {
                idColumn.add(v.index())
                xColumn.add(v.x())
                yColumn.add(v.y())
                zColumn.add(v.z())
            }
            (table as DefaultGenericTable).add(idColumn)
            (table as DefaultGenericTable).add(xColumn)
            (table as DefaultGenericTable).add(yColumn)
            (table as DefaultGenericTable).add(zColumn)
        }
    }
}