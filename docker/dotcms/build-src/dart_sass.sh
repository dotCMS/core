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

dartSassLatest=$1
fileName='dart-sass.tar.gz'
dart_sass_lib='/srv/dotserver/tomcat-9.0.60/webapps/ROOT/WEB-INF/bin/'
arch=$(uname -m)
[[ "$arch" == 'x86_64' ]] && arch='x64'
folderName="dart-sass-linux-$arch"

wget https://github.com/sass/dart-sass/releases/download/$dartSassLatest/dart-sass-$dartSassLatest-linux-$arch.tar.gz -O $fileName
tar -xf $fileName

rm -rf $dart_sass_lib
mkdir -p $dart_sass_lib
## Rename the folder to have the expected naming convention before moving it to the "WEB-INF/bin/" folder
mv ./dart-sass $dart_sass_lib$folderName
