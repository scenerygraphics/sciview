#!/bin/sh

if [ "$TRAVIS_SECURE_ENV_VARS" = true \
  -a "$TRAVIS_PULL_REQUEST" = false \
  -a "$TRAVIS_BRANCH" = master \
  -a "$TRAVIS_OS_NAME" == linux ]
then
  mvn -Pdeploy-to-imagej deploy --settings settings.xml

  echo
  echo "====== Generating Fiji installation ======"
  sh populate_fiji.sh

  echo
  echo "====== Uploading to SciView update site ======"
  cd Fiji.app
  curl -O https://raw.githubusercontent.com/fiji/fiji/7f13f66968a9d4622e519c8aae04786db6601314/bin/upload-site-simple.sh
  chmod a+x upload-site-simple.sh
  ./upload-site-simple.sh SciView Kharrington
else
  mvn install
fi
