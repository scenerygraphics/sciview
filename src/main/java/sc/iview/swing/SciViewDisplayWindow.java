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
package sc.iview.swing;

import org.scijava.ui.viewer.DisplayPanel;
import org.scijava.ui.viewer.DisplayWindow;

import sc.iview.SciView;

/**
 * {@link DisplayWindow} wrapper implementation around a {@link SciView} object.
 * <p>
 * Note that {@link SciView} takes care of its own window internally; this class
 * exists only to adapt that window to the SciJava API.
 * </p>
 */
public class SciViewDisplayWindow implements DisplayWindow {

    private SciView sv;

    public SciViewDisplayWindow( SciView sv ) {
        this.sv = sv;
    }

    @Override
    public void setTitle( String s ) {
        // TODO Auto-generated method stub
        sv.getName();
    }

    @Override
    public void setContent( DisplayPanel panel ) {
        // Can do nothing
    }

    @Override
    public void pack() {
        // Can do nothing
    }

    @Override
    public void showDisplay( boolean visible ) {
        // TODO Auto-generated method stub
        if( !visible ) {
            // Probably should have a var inside graphics.scenery.backends.Renderer for visibility
        }
        if( visible ) {

        }
    }

    @Override
    public void requestFocus() {
        // Bring focus to the front
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public int findDisplayContentScreenX() {
        // Can do nothing
        return 0;
    }

    @Override
    public int findDisplayContentScreenY() {
        // Can do nothing
        return 0;
    }

}
