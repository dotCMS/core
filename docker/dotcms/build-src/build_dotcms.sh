#!/bin/bash

set -e

echo "Executing: $0 $@"

build_source=$1
build_id=$2
echo "Build source: $build_source"
echo "Build id: $build_id"

build_target_dir=/build/cms
mkdir -p "${build_target_dir}"

build_by_commit() {
  cd /build/src/core

  # if this is not a shallow checkout (meaning, we are not being built in github)
  if [[ -d "/build/src/core/.git" ]]; then
      git fetch --all --tags
      git pull
      echo "Checking out commit/tag/branch: $1"
      if [[ ${is_release} == true ]]; then
        echo "Executing: git checkout tags/${1} -b ${1}"
        git checkout tags/${1} -b ${1}
      elif [[ "${1}" != 'master' ]]; then
        echo "Executing: git checkout ${1}"
        git checkout ${1}
      fi
      git clean -f -d
  fi
  rm -rf /build/src/core/dist
  cd dotCMS && ./gradlew clean createDistPrep --no-daemon 

  find ../dist/  -name "*.sh" -exec chmod 500 {} \;
  mv ../dist/* "${build_target_dir}"
}

set_tomcat_dir() {
  tomcat_versions=$(find /srv/dotserver/ -type d -name tomcat-* | grep -oP "(?<=tomcat-)[0-9]{1}\.[0-9]{1}\.[0-9]+$" | sort -n)
  display_tomcat_version=$(echo ${tomcat_versions} | tr '\n' ' ')
  echo "Found tomcat installations: ${display_tomcat_version}"

  eval $(cat gradle.properties | grep tomcatInstallVersion | tr -d '[:space:]')
  echo "Found tomcat_version=\"${tomcatInstallVersion}\" from gradle.properties"
  tomcat_version="${tomcatInstallVersion}"

  if [[ -n "${tomcat_version}" ]]; then
    if [[ -n $(echo "${tomcat_versions}" | grep -oP "${tomcat_version}") ]]; then
      echo "Matched tomcat_version: ${tomcat_version} with installed"
    else
      echo "Provided tomcat_version: ${tomcat_version} does not matched installed, aborting"
      exit 1
    fi
  else
    echo 'Could not find a provided tomcat version, falling back to whatever is within /srv/dotserver/'
    tomcat_version=$(find /srv/dotserver/ -type d -name tomcat-* | grep -oP "(?<=tomcat-)[0-9]{1}\.[0-9]{1}\.[0-9]+$" | sort -n | tail -n 1)
    [[ -z "${tomcat_version}" ]] && echo "ERROR: Unable to determine Tomcat version" && exit 1
  fi

  echo "Using tomcat_version=${tomcat_version}"
  echo ${tomcat_version} > /srv/TOMCAT_VERSION
}

case "${build_source}" in

  "COMMIT" | "TAG" )

    build_by_commit "${build_id}"
    ;;

  *)
    echo "Invalid option"
    exit 1
    ;;
esac

mv ${build_target_dir}/* /srv/

set_tomcat_dir
