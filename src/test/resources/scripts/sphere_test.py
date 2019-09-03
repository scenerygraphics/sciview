#@SciView sciView

import cleargl

# Add a sphere to the scene
sphere = sciView.addSphere()

# Make the sphere red
sphere.getMaterial().setDiffuse(cleargl.GLVector([1,0,0]))
sphere.getMaterial().setSpecular(cleargl.GLVector([1,0,0]))
sphere.getMaterial().setAmbient(cleargl.GLVector([1,0,0]))

# Center the camera on the sphere
sciView.centerOnNode(sphere)
