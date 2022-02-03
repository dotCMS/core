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
