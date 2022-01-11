/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
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
package sc.iview

import graphics.scenery.SceneryBase
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.JPanel
import kotlin.math.roundToInt

/**
 * Splash label class to show logo, version, and git hashes.
 *
 * @author Ulrik Guenther
 */
class SplashLabel : JPanel(), ItemListener {
    private val logger = LoggerFactory.getLogger("SciView")
    private var splashImage: BufferedImage
    val sceneryVersion: String? = SceneryBase::class.java.getPackage().implementationVersion
    val sciviewVersion: String? = SciView::class.java.getPackage().implementationVersion
    var versionString: String = ""
        private set
    @Volatile var alpha = 1.0f

    override fun itemStateChanged(e: ItemEvent) {
        isVisible = e.stateChange == ItemEvent.SELECTED
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.color = Color(0, 0, 0, (255 * alpha).roundToInt())
        g2d.fillRect(0, 0, width, height)
        val x = 100
        val y = 100

        g2d.drawImage(splashImage, x, y, splashImage.width / 2, splashImage.height / 2, null)

        g2d.color = Color.LIGHT_GRAY
        g2d.drawString(versionString, x, height - 50)
        g2d.drawString("made with 💗 at CASUS Görlitz, MDC Berlin, University of Idaho, & MPI-CBG Dresden", x, height - 30)
    }

    private fun getGitHashFor(clazz: Class<*>): String {
        val sciviewBaseClassName = clazz.simpleName + ".class"
        val sciviewClassPath = clazz.getResource(sciviewBaseClassName).toString()
        var gitHash = ""
        if (!sciviewClassPath.startsWith("jar")) {
            return gitHash
        }
        gitHash = try {
            val url = URL(sciviewClassPath)
            val jarConnection = url.openConnection() as JarURLConnection
            val manifest = jarConnection.manifest
            val attributes = manifest.mainAttributes
            attributes.getValue("Implementation-Build")
        } catch (ioe: IOException) {
            ""
        } catch (npe: NullPointerException) {
            ""
        }
        return gitHash
    }

    init {
        isOpaque = false
        var sceneryGitHash = getGitHashFor(SceneryBase::class.java)
        var sciviewGitHash = getGitHashFor(SciView::class.java)

        if (sceneryGitHash.isNotEmpty()) {
            sceneryGitHash = " ($sceneryGitHash)"
        }

        if (sciviewGitHash.isNotEmpty()) {
            sciviewGitHash = " ($sciviewGitHash) "
        }

        versionString = if (sceneryVersion == null || sciviewVersion == null) {
            "sciview / scenery, development version"
        } else {
            "sciview $sciviewVersion$sciviewGitHash) / scenery $sceneryVersion$sceneryGitHash"
        }
        logger.info("This is $versionString ($sciviewGitHash / $sceneryGitHash)")

        splashImage = try {
            ImageIO.read(this.javaClass.getResourceAsStream("sciview-logo.png"))
        } catch (e: IOException) {
            logger.warn("Could not read splash image 'sciview-logo.png'")
            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }

        addMouseListener(object: MouseListener {
            /**
             * Invoked when the mouse button has been clicked (pressed
             * and released) on a component.
             * @param e the event to be processed
             */
            override fun mouseClicked(e: MouseEvent?) {
                if(e?.clickCount == 2) {
                    val selection = StringSelection(versionString)
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(selection, selection)
                    logger.info("Copied version string to the system clipboard.")
                }
            }

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}

        })
    }
}
