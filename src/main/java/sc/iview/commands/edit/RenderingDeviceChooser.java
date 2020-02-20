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
package sc.iview.commands.edit;

import graphics.scenery.backends.Renderer;
import graphics.scenery.backends.opengl.OpenGLRenderer;
import graphics.scenery.backends.vulkan.VulkanRenderer;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import sc.iview.SciView;

import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class, name = "DeviceChooserDialog")
public class RenderingDeviceChooser extends DynamicCommand {

    //since dialog will not show if UI is not available,
    //we need UIService to have someone to ask to show the UI
    @Parameter
    private UIService uiService;

    //for reporting
    @Parameter
    private LogService logService;

    //the main actor!
    @Parameter
    private SciView sciView;


    @Parameter(label = "Select renderer:",
               initializer = "fillAvailRenderers", choices = {})
    private String selectedRenderer = "no renderer";

    List<String> availRenderers = new ArrayList<>(5);

    void fillAvailRenderers() {
        availRenderers = listAvailRenderers(sciView);
        if (availRenderers.size() > 1) {
            logService.info("Will be choosing from these devices:");
            for (String d : availRenderers) logService.info("  "+d);

            //we need the GUI to be visible, otherwise the ImageJ dialogs do not show!
            if (!uiService.isVisible()) uiService.showUI();

            //setup the combo list with the devices
            this.getInfo()
                .getMutableInput("selectedRenderer", String.class)
                .setChoices( availRenderers );
        }
    }


    @Override
    public void run() {
        if (sciView == null)
            throw new RuntimeException("SciView must be running!");

        if (availRenderers.size() == 0) {
            logService.warn("No renderer device available! Doing nothing.");
        }
        else if (availRenderers.size() == 1) {
            logService.info("Only one renderer device is available, using it.");
        }
        else {
            if (selectedRenderer.startsWith("SciView")) return;
            //else
            //ask sciView to actually switch to the selected renderer
            logService.info("Switching to device: "+selectedRenderer);
            if (selectedRenderer.startsWith("OpenGL")) {
                sciView.replaceRenderer("OpenGLRenderer",true);
            } else {
                System.setProperty("scenery.Renderer.Device", selectedRenderer.substring(8));
                sciView.replaceRenderer("VulkanRenderer",true);
            }
        }
    }


    /** Setup system properties scenery.Renderer and scenery.Renderer.Device based
     *  on the provided parameter, if none of them is set or user has not selected
     *  a particular renderer. The input parameter is expected to be one of the
     *  possible menu items that the listAvailRenderers() can generate. */
    static public void setupSystemProperties(final String selectedRenderer) {
        //do nothing if "nothing" is requested...
        if (selectedRenderer == null) return;

        //do nothing if properties are already setup -- do not change them
        if (System.getProperty("scenery.Renderer") != null
         || System.getProperty("scenery.Renderer.Device") != null) return;

        System.out.println("Considering the last selected renderer: "+selectedRenderer);

        //both system properties are not set if we got here,
        //do nothing if "automagic" is requested
        if (selectedRenderer.startsWith("SciView")) return;

        if (selectedRenderer.startsWith("OpenGL")) {
            System.setProperty("scenery.Renderer", "OpenGLRenderer");
            //
            System.out.println("Setting up system property: scenery.Renderer=OpenGLRenderer");
        } else {
            System.setProperty("scenery.Renderer","VulkanRenderer");
            System.setProperty("scenery.Renderer.Device", selectedRenderer.substring(8));
            //
            System.out.println("Setting up system property: scenery.Renderer=VulkanRenderer");
            System.out.println("Setting up system property: scenery.Renderer.Device="+selectedRenderer.substring(8));
        }
    }

    /** Discover renderers and compile a list with their names. */
    static public List<String> listAvailRenderers(final SciView sciView) {
        final Renderer r = sciView.getSceneryRenderer();
        System.out.println("The current renderer: "+r.toString());

        final List<String> availRenderers = new ArrayList<>(5);
        availRenderers.add("SciView decides itself at start up");

        if (r instanceof OpenGLRenderer) {
            availRenderers.add("OpenGL: default renderer");
            final String preferredDev = System.getProperty("scenery.Renderer.Device");
            if (preferredDev == null) {
                availRenderers.add("Vulkan: first available renderer");
            } else {
                availRenderers.add("Vulkan: " + preferredDev);
            }
        } else if (r instanceof VulkanRenderer) {
            availRenderers.addAll( ((VulkanRenderer)r).getDiscoveredDevices() );
            availRenderers.add("OpenGL: default renderer");
        } else
            System.out.println("Unrecognized renderer!");

        return availRenderers;
    }
}
