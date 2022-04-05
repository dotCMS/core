#!/usr/bin/env bash

######################
# Script: local-env.sh
# Collection of common env-vars used across the pipeline

export DOTCMS_SRC="${GITHUB_WORKSPACE}/dotCMS" && echo "DOTCMS_SRC=${DOTCMS_SRC}" >> ${GITHUB_ENV}
echo "DotCMS source folder: ${DOTCMS_SRC}"

[[ -f ${DOTCMS_SRC}/gradlew && -f ${DOTCMS_SRC}/build.gradle ]] && export GRADLE_ENV=true
[[ -f ${GITHUB_WORKSPACE}/mvnw && -f ${GITHUB_WORKSPACE}/pom.xml ]] && export MAVEN_ENV=true

if [[ "${GRADLE_ENV}" == 'true' ]]; then
  export BUILD_ENV=gradle
  export BUILD_TOOL=./gradlew
  export DOTCMS_ROOT=${DOTCMS_SRC}
elif [[ "${MAVEN_ENV}" == 'true' ]]; then
  export BUILD_ENV=maven
  export BUILD_TOOL=./mvnw
  export DOTCMS_ROOT=${GITHUB_WORKSPACE}
else
  echo "Build tool cannot be found, aborting"
  exit 1
fi

echo "BUILD_ENV=${BUILD_ENV}" >> ${GITHUB_ENV}
echo "BUILD_TOOL=${BUILD_TOOL}" >> ${GITHUB_ENV}
echo "DOTCMS_ROOT=${DOTCMS_ROOT}" >> ${GITHUB_ENV}

uname -rma
echo
java -version
echo
node --version
echo
echo "Build tool detected: ${BUILD_TOOL}
BUILD_ENV: ${BUILD_ENV}
BUILD_TOOL: ${BUILD_TOOL}
DOTCMS_ROOT: ${DOTCMS_ROOT}
"
