#!/bin/sh


die() { echo "ERROR: $@" 1>&2; exit 1; }

if [ -d Fiji.app ]
then
  die "Fiji.app exists. If you want to rebuild it, please delete it first."
fi

case "$(uname -s),$(uname -m)" in
  Linux,x86_64) launcher=ImageJ-linux64 ;;
  Linux,*) launcher=ImageJ-linux32 ;;
  Darwin,*) launcher=Contents/MacOS/ImageJ-macosx ;;
  MING*,*) launcher=ImageJ-win32.exe ;;
  MSYS_NT*,*) launcher=ImageJ-win32.exe ;;
  *) die "Unknown platform" ;;
esac

if [ ! -f fiji-nojre.zip ]
then
  echo
  echo "--> Downloading Fiji"
  curl -fLO https://downloads.imagej.net/fiji/latest/fiji-nojre.zip || 
    die "Failed to download Fiji"
fi

echo
echo "--> Unpacking Fiji"
unzip fiji-nojre.zip || die "Failed to unpack Fiji"

echo
echo "--> Enabling sciview and updating Fiji"
Fiji.app/$launcher --update add-update-site sciview \
  https://sites.imagej.net/sciview ||
  die "Failed to enable sciview"
Fiji.app/$launcher --update update-force-pristine ||
  die "Failed to update Fiji"

echo
echo "--> Ready"
echo
echo "To populate this Fiji installation with a"
echo "build of the current source, please run:"
echo
echo "   ./gradlew populateFiji"
