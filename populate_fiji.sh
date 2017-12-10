#!/bin/sh

#
# populate_fiji.sh
#
# This script generates a local Fiji.app with SciView installed.

# -- Functions --

left() { echo "${1%$2*}"; }
right() { echo "${1##*$2}"; }
mid() { right "$(left "$1" "$3")" "$2"; }

die() {
  echo "$@" 1>&2
  exit 1
}

# Copies the given Maven coordinate to the specified output directory.
install() {
  (set -x; mvn dependency:copy -Dartifact="$1" -DoutputDirectory="$2" > /dev/null) ||
    die "Install failed"
}

# Deletes the natives JAR with the given artifactId and classifier.
deleteNative() {
  (set -x; rm -f Fiji.app/jars/$1-[0-9]*-$2.jar Fiji.app/jars/*/$1-[0-9]*-$2.jar) ||
    die "Delete failed"
}

# Deletes all natives JARs with the given artifactId.
deleteNatives() {
  (set -x; rm -f Fiji.app/jars/$1-[0-9]*-natives-*.jar Fiji.app/jars/*/$1-[0-9]*-natives-*.jar) ||
    die "Delete failed"
}

# -- Determine correct ImageJ launcher executable --

case "$(uname -s),$(uname -m)" in
  Linux,x86_64) launcher=ImageJ-linux64 ;;
  Linux,*) launcher=ImageJ-linux32 ;;
  Darwin,*) launcher=Contents/MacOS/ImageJ-macosx ;;
  MING*,*) launcher=ImageJ-win32.exe ;;
  *) die "Unknown platform" ;;
esac

# -- Roll out a fresh Fiji --

if [ ! -f fiji-nojre.zip ]
then
  echo
  echo "--> Downloading Fiji"
  curl -O http://downloads.imagej.net/fiji/latest/fiji-nojre.zip
fi

echo "--> Unpacking Fiji"
rm -rf Fiji.app
unzip fiji-nojre.zip

echo
echo "--> Updating Fiji"
Fiji.app/$launcher --update update-force-pristine

echo
echo "--> Copying dependencies into Fiji installation"
(set -x; mvn -Dimagej.app.directory=Fiji.app)

# -- Put back jar/gluegen-rt and jar/jogl-all --

echo
echo "--> Reinstalling gluegen-rt and jogl-all"
gluegenJar=$(echo Fiji.app/jars/gluegen-rt-main-*.jar)
gluegenVersion=$(mid "$gluegenJar" "-" ".jar")
install "org.jogamp.gluegen:gluegen-rt:$gluegenVersion" Fiji.app/jars

joglJar=$(echo Fiji.app/jars/jogl-all-main-*.jar)
joglVersion=$(mid "$joglJar" "-" ".jar")
install "org.jogamp.jogl:jogl-all:$joglVersion" Fiji.app/jars

# -- Get the list of native libraries --

# [NB] dependency:list emits G:A:P:C:V but dependency:copy needs G:A:V:P:C.
echo
echo "--> Extracting list of native dependencies"
natives=$(mvn -B dependency:list |
  grep natives |
  sed -e 's/^\[INFO\] *\([^:]*\):\([^:]*\):\([^:]*\):\([^:]*\):\([^:]*\):.*/\1:\2:\5:\3:\4/' |
  grep -v -- '-\(android\|armv6\|solaris\)' |
  sort)
for gavpc in $natives
do
  gavp=$(left "$gavpc" ':')
  gav=$(left "$gavp" ':')
  ga=$(left "$gav" ':')
  g=$(left "$ga" ':')
  a=$(right "$ga" ':')
  v=$(right "$gav" ':')
  p=$(right "$gavp" ':')
  c=$(right "$gavpc" ':')
  echo
  echo "[$a-$v-$c]"
  case "$g" in
    org.lwjgl|graphics.scenery)
      deleteNatives "$a"
      # [NB] Install all architectures manually; only one is a dependency.
      install "$gavp:natives-windows" Fiji.app/jars/win64
      install "$gavp:natives-macos" Fiji.app/jars/macosx
      install "$gavp:natives-linux" Fiji.app/jars/linux64
      ;;
    *)
      deleteNative "$a" "$c"
      case "$c" in
        natives-win*-i586) platform=win32 ;;
        natives-win*) platform=win64 ;;
        natives-linux*-i586) platform=linux32 ;;
        natives-linux*) platform=linux64 ;;
        natives-osx|natives-mac*) platform=macosx ;;
        *) die "Unsupported platform: $c" ;;
      esac
      install "$gavpc" "Fiji.app/jars/$platform"
      ;;
  esac
done


