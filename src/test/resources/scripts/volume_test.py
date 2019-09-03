#@SciView sciView
#@Dataset img

# Add a Volume to the scene
vol = sciView.addVolume(img, "Test Volume", [1,1,1])

# Center the camera on the sphere
sciView.centerOnNode(vol)
