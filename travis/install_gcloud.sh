#!/bin/bash
# Copyright 2017 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ex

if [ ! -d ${HOME}/gcloud/google-cloud-sdk/bin ]; then
    # The install script errors if this directory already exists,
    # but Travis already creates it when we mark it as cached.
    rm -rf ${HOME}/gcloud;
fi

if [ ! -d ${HOME}/gcloud/google-cloud-sdk ]; then
    mkdir -p ${HOME}/gcloud
    wget https://dl.google.com/dl/cloudsdk/release/google-cloud-sdk.tar.gz \
         --directory-prefix=${HOME}/gcloud
    pushd "${HOME}/gcloud"
    tar xzf google-cloud-sdk.tar.gz
    ./google-cloud-sdk/install.sh --usage-reporting false \
        --path-update false --command-completion false
    popd

else
    source ${HOME}/gcloud/google-cloud-sdk/path.bash.inc
fi
${HOME}/gcloud/google-cloud-sdk/bin/gcloud -q components update

credentialsFile=${TRAVIS_BUILD_DIR}/credentials.json
echo $GOOGLE_CREDENTIALS_BASE64 | base64 -d - > $credentialsFile

gcloud auth activate-service-account --key-file="${credentialsFile}"
gcloud config set project "${GOOGLE_PROJECT_ID}"

echo "TRAVIS_COMMIT : $TRAVIS_COMMIT_SHORT"
echo "TRAVIS_BRANCH : $TRAVIS_BRANCH"
echo "GOOGLE_PROJECT_ID : $GOOGLE_PROJECT_ID"