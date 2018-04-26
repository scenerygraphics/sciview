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
package sc.iview.io;

import java.io.File;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import sc.iview.SciViewService;

import graphics.scenery.Mesh;

@Plugin(type = Command.class, 
		menuPath = "SciView>Export>STL_", label = "Export STL")
public class ExportSTL  implements Command {
	
	//@Parameter
	//private Mesh scMesh;
	
	@Parameter(style=FileWidget.SAVE_STYLE)
	private File stlFile = new File("");

	@Parameter
	private SciViewService sceneryService;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		if( sceneryService.getActiveSciView().getActiveNode() instanceof Mesh ) {
			Mesh mesh = (Mesh)sceneryService.getActiveSciView().getActiveNode();

			if (mesh != null) {
				try {
					sceneryService.getActiveSciView().writeSCMesh(stlFile.getAbsolutePath(), mesh);
					//ThreeDViewer.writeSCMesh( stlFilename, mesh );
				} catch (final Exception e) {
					logService.trace(e);
				}
			}
		}
	}

}
