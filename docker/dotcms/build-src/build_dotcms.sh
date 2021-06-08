#!/bin/bash

set -e

echo "Executing: $0 $@"

build_source=$1
build_id=$2
echo "Build source: $build_source"
echo "Build id: $build_id"

build_target_dir=/build/cms
mkdir -p "${build_target_dir}"

get_by_url() {
  build_download_dir=/build/download
  mkdir -p ${build_download_dir}

  echo "Fetching build: $1"
  wget --quiet -O "${build_download_dir}/dotcms.tgz" "$1"
  tar xzf "${build_download_dir}/dotcms.tgz" -C "${build_target_dir}"
  # We should have some verification here, but we have no source of truth yet
}

build_by_commit() {
  mkdir -p /build/src && cd /build/src

  cd /build/src/core
  git fetch --all --tags
  git clean -f -d
  git pull

  echo "Checking out commit/tag/branch: $1"
  if [[ ${is_release} == true ]]; then
    echo "Executing: git checkout tags/${1} -b ${1}"
    git checkout tags/${1} -b ${1}
  elif [[ "${1}" != 'master' ]]; then
    echo "Executing: git checkout ${1}"
    git checkout ${1}
  fi

  cd dotCMS && ./gradlew clonePullTomcatDist createDistPrep -PuseGradleNode=false
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

  "TARBALL_URL" )

    get_by_url "${build_id}"
    ;;

  "RELEASE" )

    get_by_url "http://static.dotcms.com/versions/dotcms_${build_id}.tar.gz"
    ;;

  "NIGHTLY" )

    echo "ERROR: Not implemented"
    exit 1
    ;;

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
