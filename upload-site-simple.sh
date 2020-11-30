#!/bin/sh

# When used from an existing ImageJ.app directory, uploads
# the current ImageJ.app state to the specified ImageJ wiki
# update site.

die () {
	echo "$*" >&2
	exit 1
}

test $# -eq 2 ||
die "Usage: $0 <update-site> <webdav-user>"

update_site="$1"
webdav_user="$2"
url="http://sites.imagej.net/$update_site/"

# determine correct launcher to launch MiniMaven and the Updater
case "$(uname -s),$(uname -m)" in
Linux,x86_64) launcher=ImageJ-linux64;;
Linux,*) launcher=ImageJ-linux32;;
Darwin,*) launcher=Contents/MacOS/ImageJ-tiger;;
MING*,*) launcher=ImageJ-win32.exe;;
*) echo "Unknown platform" >&2; exit 1;;
esac

echo "Found launcher: $launcher"

echo "Removing TrainableSegmentation JAR"
rm -f plugins/Trainable_Segmentation*
rm -f jars/imagej-mesh*
wget https://maven.scijava.org/service/local/repositories/jitpack/content/net/imagej/imagej-mesh/a40d46f/imagej-mesh-a40d46f.jar -O jars/imagej-mesh-a40d46f.jar
# upload complete update site
password=$WIKI_UPLOAD_PASS
./$launcher --update edit-update-site $update_site $url "webdav:$webdav_user:$password" .
echo "===== Simulating update site upload ====="
./$launcher --update upload-complete-site --simulate --force --force-shadow $update_site
echo "===== Updating update site ====="
./$launcher --update upload-complete-site --force --force-shadow $update_site
