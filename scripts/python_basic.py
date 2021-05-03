import os
from scyjava import jimport, config

# Instructions for setting up python env and testing:
# [assumes conda is installed, and in root of git repo]
# 1) conda env create -f environment.yml
# 2) conda activate sciview
# 3) python scripts/python_basic.py

#print(os.environ['JAVA_HOME'])

# Setup maven repositories
config.add_repositories(
    {'scijava.public': 'https://maven.scijava.org/content/groups/public'})
config.add_repositories({'jitpack': 'https://jitpack.io'})

# Import/load dependencies
# This is a maven based version, transitive versions behave properly
sciview_ageratum = [
    'graphics.scenery:scenery:5de0b1e', 'sc.iview:sciview:92add67'
]

# There seem to be transitive dep issues with the gradle versions
sciview_gradle = [
    'graphics.scenery:scenery:4a0c1f7', 'sc.iview:sciview:0e36b9b'
]

dependencies = [
    'net.imagej:imagej:2.1.0',
] + sciview_ageratum

for dep in dependencies:
    config.add_endpoints(dep)

# Setup classes
HashMap = jimport('java.util.HashMap')
SciView = jimport('sc.iview.SciView')

# Launch sciview
sv = SciView.create()
