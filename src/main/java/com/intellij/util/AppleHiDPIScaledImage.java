package com.intellij.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Konstantin Bulenkov
 */
public class AppleHiDPIScaledImage {
  public static BufferedImage create(int width, int height, int imageType) {
      return new BufferedImage(width, height, imageType);
  }

  public static boolean is(Image image) {
      return false;
  }
}
