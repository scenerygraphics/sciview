#@ SciView sciview
# Based on https://syn.mrc-lmb.cam.ac.uk/acardona/fiji-tutorial/#find-peaks

# Load an image of the Drosophila larval fly brain and segment  
# the 5-micron diameter cells present in the red channel.  
  
from script.imglib.analysis import DoGPeaks  
from script.imglib.color import Red  
from script.imglib.algorithm import Scale2D  
from script.imglib.math import Compute  
from script.imglib import ImgLib  

from ij import IJ
from net.imglib2.img.display.imagej import ImageJFunctions
from org.scijava.util import ColorRGB
from org.joml import Vector3f

from net.imglib2.img.array import ArrayImgFactory

from net.imglib2 import RandomAccessibleInterval
from net.imglib2.type.numeric import ARGBType
from net.imglib2.type.numeric.integer import UnsignedByteType
from java.util import ArrayList

from graphics.scenery import Sphere
  
cell_diameter = 5  # in microns  
minPeak = 40 # The minimum intensity for a peak to be considered so.  
imp = IJ.openImage("http://samples.fiji.sc/first-instar-brain.zip")  
  
# Scale the X,Y axis down to isotropy with the Z axis  
cal = imp.getCalibration()  
scale2D = cal.pixelWidth / cal.pixelDepth  
iso = Compute.inFloats(Scale2D(Red(ImgLib.wrap(imp)), scale2D))  
  
# Find peaks by difference of Gaussian  
sigma = (cell_diameter  / cal.pixelWidth) * scale2D  
peaks = DoGPeaks(iso, sigma, sigma * 0.5, minPeak, 1)  
print "Found", len(peaks), "peaks"  
  
def split_channels(image):
    channels = ArrayList()
    num_channels = 4  # Assuming RGBA color model

    for channel_idx in range(num_channels):
        channel = ArrayImgFactory(UnsignedByteType()).create(image)
        channel_cursor = channel.cursor()
        image_cursor = image.cursor()

        while channel_cursor.hasNext():
            image_cursor.next()
            alpha = (image_cursor.get().get() >> 24) & 0xFF
            red = (image_cursor.get().get() >> 16) & 0xFF
            green = (image_cursor.get().get() >> 8) & 0xFF
            blue = image_cursor.get().get() & 0xFF

            if channel_idx == 0:
                channel_cursor.next().setReal(alpha)
            elif channel_idx == 1:
                channel_cursor.next().setReal(red)
            elif channel_idx == 2:
                channel_cursor.next().setReal(green)
            elif channel_idx == 3:
                channel_cursor.next().setReal(blue)                                

        channels.add(channel)

    return channels

# Show the image data
channels = split_channels(ImageJFunctions.wrap(imp))
scale = Vector3f([cal.getX(1), cal.getY(1), cal.getZ(1)])

ch1 = sciview.addVolume(channels[1])
ch1.setScale(Vector3f(scale))
sciview.setColormap(ch1, "Red.lut")

ch2 = sciview.addVolume(channels[2])
ch2.setScale(Vector3f(scale))
sciview.setColormap(ch2, "Green.lut")

ch3 = sciview.addVolume(channels[3])
ch3.setScale(Vector3f(scale))
sciview.setColormap(ch3, "Blue.lut")

# Convert the peaks into points in calibrated image space and display
for peak in peaks:
    radius = cal.pixelWidth * 1/scale2D
    node = Sphere(radius, 20)
    node.spatial().setPosition(Vector3f(peak).mul(scale))
    node.material().setDiffuse(Vector3f(1, 0, 0))
    ch1.addChild(node)
    sciview.publishNode(node)


ImageJFunctions.show(channels[0])
ImageJFunctions.show(channels[1])
ImageJFunctions.show(channels[2])
ImageJFunctions.show(channels[3])



