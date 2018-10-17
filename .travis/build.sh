#!/bin/sh

# Discern whether this is a release build.
if [ "$TRAVIS_SECURE_ENV_VARS" = true \
  -a "$TRAVIS_PULL_REQUEST" = false \
  -a -f release.properties ]
then
  echo '--> Release build detected. Will upload it to the update site.'
  UPLOAD_TO_UPDATE_SITE=true
fi

# Perform the main build.
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_eb7aa63bf7ac_key $encrypted_eb7aa63bf7ac_iv

# Upload release version to the ImageJ update site.
test "$UPLOAD_TO_UPDATE_SITE" && sh sciview_deploy.sh
