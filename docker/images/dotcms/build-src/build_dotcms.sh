#!/bin/bash

set -e

build_source=$1
build_id=$2
echo "Build source: $build_source"
echo "Build id: $build_id"

build_target_dir=/build/cms
mkdir -p "${build_target_dir}"

mkdir -p /tmp/nodetmp
curl --output /tmp/nodetmp/testing.tar.gz https://nodejs.org/dist/v7.9.0/node-v7.9.0-linux-x64.tar.gz && tar -zxvf /tmp/nodetmp/testing.tar.gz -C /tmp/nodetmp/
export PATH=$PATH:/tmp/nodetmp/node-v7.9.0-linux-x64:/tmp/nodetmp/node-v7.9.0-linux-x64/bin

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
    git clean -f -d 
    git pull
    
    echo "Checking out commit/tag/branch: $1"
    git checkout $1

    export GRADLE_OPTS="-Dfile.encoding=utf-8 -Xmx4096m  -XX:+HeapDumpOnOutOfMemoryError"
    cd dotCMS && ./gradlew --no-daemon createDistPrep
    find ../dist/  -name "*.sh" -exec chmod 500 {} \;
    mv ../dist/* "${build_target_dir}"
}

set_tomcat_dir() {
    TOMCAT_VERSION=$(find /srv/dotserver/ -type d -name tomcat-* | grep -oP "(?<=tomcat-)[0-9]{1}\.[0-9]{1}\.[0-9]+$" | head -n 1)
    [[ -z "${TOMCAT_VERSION}" ]] && echo "ERROR: Unable to determine Tomcat version" && exit 1
    echo ${TOMCAT_VERSION} >/srv/TOMCAT_VERSION
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


