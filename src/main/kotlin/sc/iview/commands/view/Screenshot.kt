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

import graphics.scenery.backends.RenderedImage;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import sc.iview.SciView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_SCREENSHOT;

/**
 * Command to take a screenshot. The screenshot is opened in ImageJ.
 *
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "View", weight = VIEW), //
                 @Menu(label = "Screenshot", weight = VIEW_SCREENSHOT) })
public class Screenshot implements Command {

    @Parameter
    private SciView sciView;

    @Parameter
    private OpService opService;

    @Parameter
    private IOService ioService;

    @Parameter
    private UIService uiService;

    @Parameter(type = ItemIO.OUTPUT)
    private ImgPlus img;

    @Override
    public void run() {
        Img<ARGBType> screenshot = sciView.getARGBScreenshot();

        img = new ImgPlus<>(screenshot);

        uiService.show(img);
    }
}
