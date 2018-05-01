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

package sc.iview.javafx;

import java.io.IOException;
import java.net.URL;

import org.scijava.input.Accelerator;
import org.scijava.input.KeyCode;
import org.scijava.menu.AbstractMenuCreator;
import org.scijava.menu.ShadowMenu;
import org.scijava.module.ModuleInfo;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Menu creator for a JavaFX menu structure.
 * 
 * @author Curtis Rueden
 */
@SuppressWarnings("restriction")
public class JavaFXMenuCreator extends AbstractMenuCreator<MenuBar, Menu> {

    // -- Internal methods --

    @Override
    protected void addLeafToMenu( final ShadowMenu shadow, final Menu target ) {
        final String name = shadow.getMenuEntry().getName();
        final MenuItem menuItem = new MenuItem( name );
        assignProperties( shadow, menuItem );
        linkAction( shadow, menuItem );
        target.getItems().add( menuItem );
    }

    @Override
    protected void addLeafToTop( ShadowMenu shadow, MenuBar target ) {
        final Menu menu = addNonLeafToTop( shadow, target );
        linkAction( shadow, menu );
    }

    @Override
    protected Menu addNonLeafToMenu( final ShadowMenu shadow, final Menu target ) {
        final Menu menu = new Menu( shadow.getMenuEntry().getName() );
        assignProperties( shadow, menu );
        target.getItems().add( menu );
        return menu;
    }

    @Override
    protected Menu addNonLeafToTop( ShadowMenu shadow, MenuBar target ) {
        final Menu menu = new Menu( shadow.getMenuEntry().getName() );
        assignProperties( shadow, menu );
        target.getMenus().add( menu );
        return menu;
    }

    @Override
    protected void addSeparatorToTop( MenuBar target ) {
        // NB: Ignore top-level separator.
    }

    @Override
    protected void addSeparatorToMenu( final Menu target ) {
        target.getItems().add( new SeparatorMenuItem() );
    }

    // -- Helper methods --

    private void assignProperties( final ShadowMenu shadow, final MenuItem menuItem ) {
        // Assign keyboard shortcut.
        final Accelerator accelerator = shadow.getMenuEntry().getAccelerator();
        if( accelerator != null && accelerator.getKeyCode() != KeyCode.UNDEFINED ) {
            menuItem.setAccelerator( JavaFXKeys.keyCombination( accelerator ) );
        }

        // Assign menu icon.
        final URL iconURL = shadow.getIconURL();
        if (iconURL != null) {
            try {
                menuItem.setGraphic( new ImageView(new Image(iconURL.openStream())));
            } catch( IOException exc ) {
                // TODO: log exception.
            }
        }

        // Enable or disable menu item.
        final ModuleInfo info = shadow.getModuleInfo();
        if( info != null ) menuItem.setDisable( !info.isEnabled() );
    }

    private void linkAction( final ShadowMenu shadow, final MenuItem menuItem ) {
        menuItem.setOnAction(t -> shadow.run());
    }

}
