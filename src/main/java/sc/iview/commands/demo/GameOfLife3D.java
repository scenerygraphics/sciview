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

package sc.iview.commands.demo;

import bdv.BigDataViewer;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.SourceAndConverter;
import cleargl.GLVector;
import graphics.scenery.BoundingGrid;
import graphics.scenery.volumes.Volume;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;
import sc.iview.SciView;
import sc.iview.event.NodeRemovedEvent;

import javax.swing.*;

import static sc.iview.commands.MenuWeights.DEMO;
import static sc.iview.commands.MenuWeights.DEMO_GAME_OF_LIFE;

/**
 * Conway's Game of Life&mdash;in 3D!
 *
 * @author Curtis Rueden
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Game of Life 3D", weight = DEMO_GAME_OF_LIFE) })
public class GameOfLife3D implements Command {

    private static final int ALIVE = 255;
    private static final int DEAD = 16;

    private static final String SIX = "6-connected";
    private static final String EIGHTEEN = "18-connected";
    private static final String TWENTY_SIX = "26-connected";

    @Parameter
    private SciView sciView;

    @Parameter(label = "Starvation threshold", min = "0", max = "26", persist = false)
    private int starvation = 5;

    @Parameter(label = "Birth threshold", min = "0", max = "26", persist = false)
    private int birth = 6;

    @Parameter(label = "Suffocation threshold", min = "0", max = "26", persist = false)
    private int suffocation = 9;

    @Parameter(choices = { SIX, EIGHTEEN, TWENTY_SIX }, persist = false)
    private String connectedness = TWENTY_SIX;

    @Parameter(label = "Initial saturation % when randomizing", min = "1", max = "99", style = NumberWidget.SCROLL_BAR_STYLE, persist = false)
    private int saturation = 10;

//    @Parameter(label = "Play speed", min = "1", max="100", style = NumberWidget.SCROLL_BAR_STYLE, persist = false)
    private int playSpeed = 10;
//
//    @Parameter(callback = "iterate")
//    private Button iterate;
//
//    @Parameter(callback = "randomize")
//    private Button randomize;
//
//    @Parameter(callback = "play")
//    private Button play;
//
//    @Parameter(callback = "pause")
//    private Button pause;

    private int w = 64, h = 64, d = 64;
    private Img<UnsignedByteType> field;
    private String name;
    private float[] voxelDims;
    private Volume volume;

    /** Temporary buffer for use while recomputing the image. */
    private boolean[] bits = new boolean[w * h * d];
    private GenericDialog dialog;

    /** Repeatedly iterates the simulation until stopped **/
    public void play() {
        sciView.animate( playSpeed, this::iterate);
    }

    /** Stops the simulation **/
    public void pause() {
        sciView.stopAnimation();
    }

    /** Randomizes a new bit field. */
    public void randomize() {
        final Cursor<UnsignedByteType> cursor = field.localizingCursor();
        final double chance = saturation / 100d;
        while( cursor.hasNext() ) {
            final boolean alive = Math.random() <= chance;
            cursor.next().set( alive ? ALIVE : DEAD );
        }
        updateVolume();
    }

    /** Performs one iteration of the game. */
    public void iterate() {
        final int connected;
        switch( connectedness ) {
        case SIX: connected = 6; break;
        case EIGHTEEN: connected = 18; break;
        default: connected = 26; break;
        }

        // compute the new image field
        final RandomAccess<UnsignedByteType> access = field.randomAccess();

        //RandomAccess<UnsignedByteType> access = ((SourceAndConverter) ((Volume.VolumeDataSource.RAIISource) volume.getDataSource()).getSources().get(0)).getSpimSource().getSource(0, 0).randomAccess();

        for( int z = 0; z < d; z++ ) {
            for( int y = 0; y < h; y++ ) {
                for( int x = 0; x < w; x++ ) {
                    final int i = z * w * h + y * w + x;
                    final int n = neighbors( access, x, y, z, connected );
                    access.setPosition( x, 0 );
                    access.setPosition( y, 1 );
                    access.setPosition( y, 2 );
                    if( alive( access ) ) {
                        // Living cell stays alive within (starvation, suffocation).
                        bits[i] = n > starvation && n < suffocation;
                    } else {
                        // New cell forms within [birth, suffocation).
                        bits[i] = n >= birth && n < suffocation;
                    }
                }
            }
        }

        // write the new bit field into the image
        final Cursor<UnsignedByteType> cursor = field.localizingCursor();
        while( cursor.hasNext() ) {
            cursor.fwd();
            final int x = cursor.getIntPosition( 0 );
            final int y = cursor.getIntPosition( 1 );
            final int z = cursor.getIntPosition( 2 );
            final boolean alive = bits[z * w * h + y * w + x];
            cursor.get().set( alive ? ALIVE : DEAD );
        }

//        for( int z = 0; z < d; z++ ) {
//            for( int y = 0; y < h; y++ ) {
//                for( int x = 0; x < w; x++ ) {
//                    access.setPosition( x, 0 );
//                    access.setPosition( y, 1 );
//                    access.setPosition( y, 2 );
//                    final boolean alive = bits[z * w * h + y * w + x];
//                    access.get().set( alive ? ALIVE : DEAD );
//                }
//            }
//        }

        updateVolume();
    }

    @Override
    public void run() {
        field = ArrayImgs.unsignedBytes( w, h, d );
        randomize();

        dialog = new GenericDialog("Game of Life 3D");
        dialog.addNumericField("Starvation threshold", starvation, 0);
        dialog.addNumericField("Birth threshold", birth, 0);
        dialog.addNumericField("Suffocation threshold", suffocation, 0);
        dialog.addNumericField("Initial saturation % when randomizing", saturation, 0);
        dialog.showDialog();

        if( dialog.wasCanceled() ) return;

        starvation = (int) dialog.getNextNumber();
        birth = (int) dialog.getNextNumber();
        suffocation = (int) dialog.getNextNumber();
        saturation = (int) dialog.getNextNumber();

        randomize();
        play();

//
//    @Parameter(callback = "iterate")
//    private Button iterate;
//
//    @Parameter(callback = "randomize")
//    private Button randomize;
//
//    @Parameter(callback = "play")
//    private Button play;
//
//    @Parameter(callback = "pause")
//    private Button pause;

        //play();

        //eventService.subscribe(this);
    }


    // -- Helper methods --

    private int neighbors( RandomAccess<UnsignedByteType> access, int x, int y, int z, int connected ) {
        int n = 0;
        // six-connected
        n += val( access, x - 1, y, z );
        n += val( access, x + 1, y, z );
        n += val( access, x, y - 1, z );
        n += val( access, x, y + 1, z );
        n += val( access, x, y, z - 1 );
        n += val( access, x, y, z + 1 );
        // eighteen-connected
        if( connected >= 18 ) {
            n += val( access, x - 1, y - 1, z );
            n += val( access, x + 1, y - 1, z );
            n += val( access, x - 1, y + 1, z );
            n += val( access, x + 1, y + 1, z );
            n += val( access, x - 1, y, z - 1 );
            n += val( access, x + 1, y, z - 1 );
            n += val( access, x - 1, y, z + 1 );
            n += val( access, x + 1, y, z + 1 );
            n += val( access, x, y - 1, z - 1 );
            n += val( access, x, y + 1, z - 1 );
            n += val( access, x, y - 1, z + 1 );
            n += val( access, x, y + 1, z + 1 );
        }
        // twenty-six-connected
        if( connected == 26 ) {
            n += val( access, x - 1, y - 1, z - 1 );
            n += val( access, x + 1, y - 1, z - 1 );
            n += val( access, x - 1, y + 1, z - 1 );
            n += val( access, x + 1, y + 1, z - 1 );
            n += val( access, x - 1, y - 1, z + 1 );
            n += val( access, x + 1, y - 1, z + 1 );
            n += val( access, x - 1, y + 1, z + 1 );
            n += val( access, x + 1, y + 1, z + 1 );
        }
        return n;
    }



    private int val( RandomAccess<UnsignedByteType> access, int x, int y, int z ) {
        if( x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d ) return 0;
        access.setPosition( x, 0 );
        access.setPosition( y, 1 );
        access.setPosition( z, 2 );
        return alive( access ) ? 1 : 0;
    }

    private boolean alive( final Sampler<UnsignedByteType> access ) {
        return access.get().get() == ALIVE;
    }

    private long tick;

    private void updateVolume() {
        if( volume == null ) {
            name = "Life Simulation";
            voxelDims = new float[] { 1, 1, 1 };
            volume = ( Volume ) sciView.addVolume( field, name, voxelDims );

            BoundingGrid bg = new BoundingGrid();
            bg.setNode( volume );

//            volume.setVoxelSizeX(10.0f);
//            volume.setVoxelSizeY(10.0f);
//            volume.setVoxelSizeZ(10.0f);

            volume.putAbove(new Vector3f(0.0f, 0.0f, 0.0f));
//            volume.setRenderingMethod(2);
            volume.getTransferFunction().addControlPoint(0.0f, 0.0f);
            volume.getTransferFunction().addControlPoint(0.4f, 0.3f);

            volume.setName( "Game of Life 3D" );

            sciView.centerOnNode(volume);
        } else {
            // NB: Name must be unique each time.
            //sciView.updateVolume( field, name + "-" + ++tick, voxelDims, volume );

            RandomAccessibleIntervalSource<UnsignedByteType> newSource = new RandomAccessibleIntervalSource<UnsignedByteType>(field, new UnsignedByteType(), name + "-" + ++tick);

            SourceAndConverter<UnsignedByteType> sourceAndConverter = BigDataViewer.wrapWithTransformedSource(
                    new SourceAndConverter<>(newSource, BigDataViewer.createConverterToARGB(new UnsignedByteType())));

            ((Volume.VolumeDataSource.RAISource) volume.getDataSource()).getSources().set(0, sourceAndConverter);

            volume.setDirty(true);
            volume.setNeedsUpdate(true);
            volume.getVolumeManager().requestRepaint();
            //volume.getCacheControl().prepareNextFrame();
        }
    }

    /**
     * Stops the animation when the volume node is removed.
     * @param event
     */
    @EventHandler
    private void  onNodeRemoved(NodeRemovedEvent event) {
        if(event.getNode() == volume) {
            sciView.stopAnimation();
        }
    }

    /**
     * Returns the current Img
     */
    public Img<UnsignedByteType> getImg() {
        return field;
    }

    /**
     * Returns the scenery volume node.
     */
    public Volume getVolume() {
        return volume;
    }
}
