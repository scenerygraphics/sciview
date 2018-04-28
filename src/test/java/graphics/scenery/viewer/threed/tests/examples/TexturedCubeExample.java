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
package graphics.scenery.viewer.threed.tests.examples;

import org.junit.Test;

import cleargl.GLVector;
import graphics.scenery.Box;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Material;
import graphics.scenery.PointLight;
import graphics.scenery.SceneryBase;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;

/**
 * Created by kharrington on 7/6/16.
 *
 * This is a copy of the TextureCubeJavaExample from graphics/scenery for the
 * purpose of sanity checking
 */
public class TexturedCubeExample {
    @Test
    public void testExample() throws Exception {
        TexturedCubeJavaApplication viewer = new TexturedCubeJavaApplication( "scenery - TexturedCubeExample", 800,
                                                                              600 );
        viewer.main();
    }

    private class TexturedCubeJavaApplication extends SceneryBase {
        public TexturedCubeJavaApplication( String applicationName, int windowWidth, int windowHeight ) {
            super( applicationName, windowWidth, windowHeight, true );
        }

        @Override
        public void init() {

            setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(), 512, 512 ) );
            getHub().add( SceneryElement.Renderer, getRenderer() );

            Material boxmaterial = new Material();
            boxmaterial.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
            boxmaterial.setDiffuse( new GLVector( 0.0f, 1.0f, 0.0f ) );
            boxmaterial.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
            boxmaterial.getTextures().put( "diffuse", TexturedCubeJavaApplication.class.getResource(
                                                                                                     "textures/helix.png" ).getFile() );

            final Box box = new Box( new GLVector( 1.0f, 1.0f, 1.0f ), false );
            box.setMaterial( boxmaterial );
            box.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );

            getScene().addChild( box );

            PointLight[] lights = new PointLight[2];

            for( int i = 0; i < lights.length; i++ ) {
                lights[i] = new PointLight();
                lights[i].setPosition( new GLVector( 2.0f * i, 2.0f * i, 2.0f * i ) );
                lights[i].setEmissionColor( new GLVector( 1.0f, 0.0f, 1.0f ) );
                lights[i].setIntensity( 100.2f * ( i + 1 ) );
                lights[i].setLinear( 0.0f );
                lights[i].setQuadratic( 0.5f );
                getScene().addChild( lights[i] );
            }

            Camera cam = new DetachedHeadCamera();
            cam.setPosition( new GLVector( 0.0f, 0.0f, 5.0f ) );
            cam.perspectiveCamera( 50.0f, getRenderer().getWindow().getWidth(), getRenderer().getWindow().getHeight(),
                                   0.1f, 1000.0f );
            cam.setActive( true );
            getScene().addChild( cam );

            Thread rotator = new Thread() {
                public void run() {
                    while( true ) {
                        box.getRotation().rotateByAngleY( 0.01f );
                        box.setNeedsUpdate( true );

                        try {
                            Thread.sleep( 20 );
                        } catch( InterruptedException e ) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            rotator.start();

            //setRepl(new REPL(getScene(), getRenderer()));
            //getRepl().start();
            //getRepl().showConsoleWindow();

        }

    }

}
