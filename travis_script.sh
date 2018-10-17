#!/bin/bash

echo "Deploy situation is:"
echo "  TRAVIS_SECURE_ENV_VARS $TRAVIS_SECURE_ENV_VARS"
echo "  TRAVIS_PULL_REQUEST $TRAVIS_PULL_REQUEST"
echo "  TRAVIS_BRANCH $TRAVIS_BRANCH"
echo "  TRAVIS_OS_NAME $TRAVIS_OS_NAME"
echo "  TRAVIS_COMMIT_MESSAGE $TRAVIS_COMMIT_MESSAGE"

required_message="Trigger SciView release. The secret is tiddly winks."

if [ "$TRAVIS_SECURE_ENV_VARS" = true \
  -a "$TRAVIS_PULL_REQUEST" = false \
  -a "$TRAVIS_BRANCH" = master \
  -a "$TRAVIS_OS_NAME" == linux ] && \
  [ -z "${TRAVIS_COMMIT_MESSAGE##*$required_message*}" ]
then
  # We'll use the standard scijava script for maven deployment
  #mvn -Pdeploy-to-imagej deploy --settings settings.xml
  
  devVersion=$(mvn -Dexec.executable='echo' -Dexec.args='${project.version}' exec:exec -q)
  pomVersion=${devVersion%-SNAPSHOT}
  
  echo
  echo "====== Releasing SciView $pomVersion ======"
  git checkout git@github.com:scijava/scijava-scripts.git
  sh scijava-scripts/release-version.sh $pomVersion
  
  # Checkout the tag
  echo
  echo "====== Uploading version $pomVersion to update site ======"
  git fetch --all --tags --prune
  git checkout tags/sciview-$pomVersion -b current
  
  source sciview_deploy.sh

fi
