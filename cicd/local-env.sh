#!/usr/bin/env bash

######################
# Script: local-env.sh
# Collection of common env-vars used across the pipeline

dotcms_src="${GITHUB_WORKSPACE}/dotCMS"
echo "DotCMS source folder: ${dotcms_src}"

[[ -f ${dotcms_src}/gradlew && -f ${dotcms_src}/build.gradle ]] && gradle_env=true
[[ -f ${GITHUB_WORKSPACE}/mvnw && -f ${GITHUB_WORKSPACE}/pom.xml ]] && maven_env=true

if [[ "${gradle_env}" == 'true' ]]; then
  build_env=gradle
  build_tool=./gradlew
elif [[ "${maven_env}" == 'true' ]]; then
  build_env=maven
  build_tool=./mvnw
else
  echo "Build tool cannot be found, aborting"
  exit 1
fi

echo "::set-output name=build-env::${build_env}"
echo "::set-output name=build-tool:${build_tool}"

uname -rma
echo
java -version
echo
node --version
echo
echo "Build tool detected: ${build_tool}
build_env: ${build_env}
build_tool: ${build_tool}
"
