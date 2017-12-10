#!/bin/sh

if [ "$TRAVIS_SECURE_ENV_VARS" = true \
  -a "$TRAVIS_PULL_REQUEST" = false \
  -a "$TRAVIS_BRANCH" = master \
  -a "$TRAVIS_OS_NAME" == linux ]
then
  mvn -Pdeploy-to-imagej deploy --settings settings.xml
  curl -O http://downloads.imagej.net/fiji/latest/fiji-nojre.zip
  unzip fiji-nojre.zip
  rm target/sciview*tests.jar
  rm target/sciview*sources.jar
  mv target/sciview* Fiji.app/jars/
  # Handle dependencies
  echo "Fetching dependencies"
  mvn -Dimagej.app.directory=Fiji.app/ -DdeleteOtherVersionsProperty=false
  echo "Dependencies fetched"
  cd Fiji.app
  curl -O https://raw.githubusercontent.com/fiji/fiji/7f13f66968a9d4622e519c8aae04786db6601314/bin/upload-site-simple.sh
  chmod a+x upload-site-simple.sh
  echo "Transmitting to update site"
  ./upload-site-simple.sh SciView Kharrington
else
  mvn install
fi
