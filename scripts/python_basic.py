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
dependencies = [
    'net.imagej:imagej:2.1.0', 'graphics.scenery:scenery:4a0c1f7',
    'sc.iview:sciview:ad0a6e5'
]

for dep in dependencies:
    config.add_endpoints(dep)

# Setup classes
SciView = jimport('sc.iview.SciView')

# Launch sciview
sv = SciView.create()
