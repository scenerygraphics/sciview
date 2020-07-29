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

/**
 * Command to display the vertices of the currently active Node as a table.
 *
 * @author Kyle Harrington
 *
 */
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
