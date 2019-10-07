#!/bin/bash


# We'll use the standard scijava script for maven deployment
#mvn -Pdeploy-to-imagej deploy --settings settings.xml

echo
echo "====== Generating Fiji installation ======"
sh populate_fiji.sh

echo
echo "====== Uploading to SciView update site ======"
cd Fiji.app
curl -O https://raw.githubusercontent.com/fiji/fiji/7f13f66968a9d4622e519c8aae04786db6601314/bin/upload-site-simple.sh
chmod a+x upload-site-simple.sh
./upload-site-simple.sh SciView-Unstable Kharrington

echo
echo "===== Creating unstable release ====="
cd ..
zip SciView-Fiji.zip Fiji.app
