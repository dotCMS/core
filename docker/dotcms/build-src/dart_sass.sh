#!/bin/bash

set -e

## ====================================================================================================================
## This script downloads the appropriate binary files for the Dart SASS Library depending on the current Operating
## System -- Linux, Mac OS X, etc. -- and its architecture -- x86, ARM, etc. There are a couple of details that must be
## taken into consideration here:
##
## 1. The specific Dart SASS version to be downloaded is specified in "$SOURCE_HOME/core/docker/dotcms/Dockerfile".
## 2. The Tomcat version number in the "dart_sass_lib" variable must be updated every time Tomcat gets updated.
## ====================================================================================================================

dart_sass_latest=$1
tomcat_dir=$(basename /srv/dotserver/tomcat-*)
file_name='dart-sass.tar.gz'
dart_sass_lib="/srv/dotserver/${tomcat_dir}/webapps/ROOT/WEB-INF/bin/"
arch=$(uname -m)
if [[ "$arch" == 'x86_64' ]]
then
  arch='x64'
elif [[ "$arch" == 'aarm64' ]]
then
  arch='arm64'
fi
folder_name="dart-sass-linux-$arch"

curl -s -L -o $file_name https://github.com/sass/dart-sass/releases/download/$dart_sass_latest/dart-sass-$dart_sass_latest-linux-$arch.tar.gz
tar -xf $file_name

rm -rf $dart_sass_lib
mkdir -p $dart_sass_lib
## Rename the folder to have the expected naming convention before moving it to the "WEB-INF/bin/" folder
## Move the Dart SASS folder to the "WEB-INF/bin/" folder and rename it to have the expected naming convention
mv ./dart-sass $dart_sass_lib$folder_name