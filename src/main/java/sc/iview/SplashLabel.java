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
package sc.iview;

import graphics.scenery.SceneryBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Splash label class to show logo, version, and git hashes.
 *
 * @author Ulrik Guenther
 */
public class SplashLabel extends JComponent implements ItemListener {
    private Logger logger = LoggerFactory.getLogger("SciView");
    public void itemStateChanged(ItemEvent e) {
        setVisible(e.getStateChange() == ItemEvent.SELECTED);
    }

    protected void paintComponent(Graphics g) {
        Point point = new Point(100, 100);

        if (point != null) {
//            g.setColor(Color.red);
//            g.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paintComponent(g);
    }

    private String getGitHashFor(Class<?> clazz) {
        final String sciviewBaseClassName = clazz.getSimpleName() + ".class";
        final String sciviewClassPath = clazz.getResource(sciviewBaseClassName).toString();
        String gitHash = "";
        if(!sciviewClassPath.startsWith("jar")) {
            return gitHash;
        }

        try {
            URL url = new URL(sciviewClassPath);
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            Manifest manifest = jarConnection.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            gitHash = attributes.getValue("Implementation-Build").substring(0, 8);
        } catch (IOException ioe){
            gitHash = "";
        }

        return gitHash;
    }

    public SplashLabel() {
        setOpaque(true);
        final String sceneryVersion = SceneryBase.class.getPackage().getImplementationVersion();
        final String sciviewVersion = SciView.class.getPackage().getImplementationVersion();
        final String versionString;

        String sceneryGitHash = getGitHashFor(SceneryBase.class);
        String sciviewGitHash = getGitHashFor(SciView.class);

        if(sceneryGitHash.length() > 0) {
            sceneryGitHash = " (" + sceneryGitHash + ")";
        }

        if(sciviewGitHash.length() > 0) {
            sciviewGitHash = " (" + sciviewGitHash + ") ";
        }

        if(sceneryVersion == null || sciviewVersion == null) {
            versionString = "sciview / scenery";
        } else {
            versionString = "sciview " + sciviewVersion + sciviewGitHash + ") / scenery " + sceneryVersion + sceneryGitHash;
        }

        logger.info("This is " + versionString + " ("+ sciviewGitHash + " / " + sceneryGitHash + ")");

        BufferedImage splashImage;
        try {
            splashImage = ImageIO.read(this.getClass().getResourceAsStream("sciview-logo.png"));
        } catch (IOException e) {
            logger.warn("Could not read splash image 'sciview-logo.png'");
            splashImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        final JLabel splashLabel = new JLabel("\n\n" + versionString,
                new ImageIcon(splashImage.getScaledInstance(500, 200, java.awt.Image.SCALE_SMOOTH)),
                SwingConstants.CENTER);
        splashLabel.setBackground(new java.awt.Color(50, 48, 47));
        splashLabel.setForeground(new java.awt.Color(78, 76, 75));
        splashLabel.setOpaque(true);
        splashLabel.setVerticalTextPosition(JLabel.BOTTOM);
        splashLabel.setHorizontalTextPosition(JLabel.CENTER);
        this.add(splashLabel);
    }
}
