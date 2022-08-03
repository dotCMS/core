#!/bin/bash

set -e

dartSassLatest=$1
arch=$(uname -m)
[[ "$arch" == 'x86_64' ]] && arch='x64'

fileName='dart-sass.tar.gz'
wget https://github.com/sass/dart-sass/releases/download/$dartSassLatest/dart-sass-$dartSassLatest-linux-$arch.tar.gz -O $fileName

tar -xf $fileName
dart_sass_lib='/srv/dotserver/tomcat-9.0.60/webapps/ROOT/WEB-INF/bin/'
mkdir -p $dart_sass_lib
mv ./dart-sass $dart_sass_lib
