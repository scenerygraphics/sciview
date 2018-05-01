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
package sc.iview.controls;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

import sc.iview.SciView;

import cleargl.GLMatrix;
import cleargl.GLVector;
import graphics.scenery.Node;

@Plugin(type = Command.class, menuPath = "SciView>Controls>Control Panel")
public class ControlPanel implements Command, Interactive, Initializable {

    @Parameter
    private SciView sciView;

    @Parameter(label = "Index of active object")
    private int activeIndex;

    @Parameter(callback = "cameraBillboard")
    private boolean billboard = true;

    @Parameter(callback = "cameraPos", min = "0", max = "1000", style = NumberWidget.SCROLL_BAR_STYLE)
    private int posX;
    @Parameter(callback = "cameraPos", min = "0", max = "1000", style = NumberWidget.SCROLL_BAR_STYLE)
    private int posY;
    @Parameter(callback = "cameraPos", min = "0", max = "1000", style = NumberWidget.SCROLL_BAR_STYLE)
    private int posZ;

    @Parameter(label = "Follow active object", callback = "focus")
    private boolean focusOnObject = true;

    @Parameter(callback = "initialize")
    private Button refresh;

    private static final float MIN_POS = -50;
    private static final float MAX_POS = 50;

    @Override
    public void initialize() {
        billboard = sciView.getCamera().isBillboard();

        GLVector pos = sciView.getCamera().getPosition();
        posX = toSlider(pos.x(), MIN_POS, MAX_POS);
        posY = toSlider(pos.y(), MIN_POS, MAX_POS);
        posZ = toSlider(pos.z(), MIN_POS, MAX_POS);
    }

    private static int toSlider( float value, float min, float max ) {
        return ( int ) ( 1000 * ( value - min ) / ( max - min ) );
    }

    private static float fromSlider( int value, float min, float max ) {
        return (max - min) * value / 1000 + min;
    }

    @Override
    public void run() {
        //
    }

    public void cameraBillboard() {
        sciView.getCamera().setBillboard( billboard );
    }
    public void cameraPos() {
        final float px = fromSlider(posX, MIN_POS, MAX_POS);
        final float py = fromSlider(posY, MIN_POS, MAX_POS);
        final float pz = fromSlider(posZ, MIN_POS, MAX_POS);
        GLVector position = new GLVector( px, py, pz );
        System.out.println( "position -> " + position );
        sciView.getCamera().setPosition( position );
        focus();
    }
    public void focus() {
        if (!focusOnObject) return;
        final Node node = sciView.getActiveNode();
        final GLVector obj = node.getPosition();
        final GLVector cam = sciView.getCamera().getPosition();
        System.out.println( "Active node = " + node + ", pos = " + obj );
        float fx = obj.x() - cam.x();
        float fy = obj.y() - cam.y();
        float fz = obj.z() - cam.z();
//        GLVector forward = new GLVector( fx, fy, fz ).normalize();
        // Point to center
//        GLVector forward = new GLVector ( -cam.x(), -cam.y(), -cam.z() ).normalize();
//        System.out.println( "Forward -> " + forward );
        System.out.println( "Targeted? " + sciView.getCamera().getTargeted() );
        sciView.getCamera().setTargeted( false );
//        sciView.getCamera().setForward( forward );
        sciView.getCamera().setPosition( sciView.getCamera().getPosition() );

        float[] matrix1 = { 1, 0, 0, 1,//
                           0, 0, 1, 1,//
                           1, 0, 1, 1, //
                           1, 1, 1, 1 };
        float[] matrix2 = { 0, 0, 1, 1,//
                           0, 1, 0, 1,//
                           1, 1, 1, 1, //
                           1, 1, 1, 1 };
        boolean go = System.currentTimeMillis() / 2000 % 2 == 0;
        GLMatrix view = new GLMatrix( go ? matrix1 : matrix2 );
        System.out.println( "View -> " + view );
        sciView.getCamera().setView( view );

//        sciView.getCamera().setTargeted( true );
//        sciView.getCamera().setTarget( obj );
    }
}
