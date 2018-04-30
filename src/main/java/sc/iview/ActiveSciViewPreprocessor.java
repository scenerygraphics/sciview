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
package sc.iview;

import net.imagej.display.process.SingleInputPreprocessor;

import org.scijava.Priority;
import org.scijava.display.DisplayService;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.swing.SciViewDisplay;

/**
 * Fills single, unresolved module inputs with the active active
 * {@link SciView}. Hence, rather than a dialog prompting the user to manually
 * select an input, the active {@link SciView} is used automatically.
 * <p>
 * In the case of more than one compatible parameter, the active {@link SciView}
 * is not used and instead the user must select.
 * </p>
 * 
 * @author Curtis Rueden
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH)
public class ActiveSciViewPreprocessor extends SingleInputPreprocessor<SciView> {

    @Parameter(required = false)
    private DisplayService displayService;

    public ActiveSciViewPreprocessor() {
        super( SciView.class );
    }

    // -- SingleInputProcessor methods --

    @Override
    public SciView getValue() {
        if( displayService == null ) return null;
        final SciViewDisplay display = displayService.getActiveDisplay( SciViewDisplay.class );
        return display == null ? null : display.get( 0 );
    }

}
