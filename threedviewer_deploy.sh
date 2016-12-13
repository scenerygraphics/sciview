#!/bin/bash

if [[ $TRAVIS_BRANCH == 'master' ]] && [[ "$TRAVIS_OS_NAME" == "linux" ]]
then
   mvn deploy --settings settings.xml
   curl -O http://downloads.imagej.net/fiji/latest/fiji-nojre.zip
   unzip fiji-nojre.zip
   mv target/ThreeDViewer* Fiji.app/jars/
   cp -r src/plugins/* Fiji.app/plugins/
   cd Fiji.app
   curl -O https://raw.githubusercontent.com/fiji/fiji/7f13f66968a9d4622e519c8aae04786db6601314/bin/upload-site-simple.sh
   chmod a+x upload-site-simple.sh
   ./upload-site-simple.sh ThreeDViewer ThreeDViewer
fi
# update force pristine
