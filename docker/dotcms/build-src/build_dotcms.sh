#!/bin/bash

set -e

echo "Executing: $0 $@"

build_source=$1
build_id=$2
echo "Build source: ${build_source}"
echo "Build id: ${build_id}"

build_target_dir=/build/cms
mkdir -p "${build_target_dir}"

build_by_commit() {
  cd /build/src/core
  git config --global user.email "build@dotcms.com"
  git config --global user.name "DotCMS Build"

  git branch
  # if this is not a shallow checkout (meaning, we are not being built in github)
  if [[ -d "/build/src/core/.git" ]]; then
    git fetch --all
    git pull
    echo "Checking out commit/tag/branch: ${build_source}"
    if [[ ${build_id} =~ ^v[0-9]{2}.[0-9]{2}(.[0-9]{1,2})?$ ]]; then
      echo "Executing: git checkout tags/${build_id} -b ${build_id}"
      git checkout tags/${build_id} -b ${build_id}
    elif [[ "${build_id}" != 'master' ]]; then
      echo "Executing: git checkout ${build_id}"
      git checkout ${build_id}
      git pull origin ${build_id}
    fi
    git clean -f -d
  fi

  rm -rf /build/src/core/dist /build/src/core/dotCMS/build /build/src/core/core-web/node_modules
  cd dotCMS && ./gradlew createDistPrep
  find ../dist/  -name "*.sh" -exec chmod 500 {} \;
  mv ../dist/* "${build_target_dir}"
}

case "${build_source}" in

  "COMMIT" | "TAG" )

    build_by_commit
    ;;

  *)
    echo "Invalid option"
    exit 1
    ;;
esac

mv ${build_target_dir}/* /srv/
