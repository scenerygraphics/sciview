#!/bin/sh

if [ "$TRAVIS_SECURE_ENV_VARS" = true \
  -a "$TRAVIS_PULL_REQUEST" = false \
  -a "$TRAVIS_BRANCH" = master \
  -a "$TRAVIS_OS_NAME" == linux ]
then
   mvn -Pdeploy-to-imagej deploy --settings settings.xml
# For update site deployment: temporarily disabled
#   curl -O http://downloads.imagej.net/fiji/latest/fiji-nojre.zip
#   unzip fiji-nojre.zip
#   mv target/SciView* Fiji.app/jars/
#   cp -r src/plugins/* Fiji.app/plugins/
#   cd Fiji.app
#   curl -O https://raw.githubusercontent.com/fiji/fiji/7f13f66968a9d4622e519c8aae04786db6601314/bin/upload-site-simple.sh
#   chmod a+x upload-site-simple.sh
#   ./upload-site-simple.sh SciView SciView
else
    mvn install
fi
