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

if [ "${CIRCLECI}" == "true" ]; then
    # Need sudo on circleci:
    # https://discuss.circleci.com/t/gcloud-components-update-version-restriction/3725
    # They also overrides the PATH to use
    # /opt/google-cloud-sdk/bin/gcloud so we can not easily use our
    # own gcloud
    sudo /opt/google-cloud-sdk/bin/gcloud -q components update beta
else
    if [ ! -d ${HOME}/gcloud/google-cloud-sdk ]; then
        mkdir -p ${HOME}/gcloud
        wget https://dl.google.com/dl/cloudsdk/release/google-cloud-sdk.tar.gz \
             --directory-prefix=${HOME}/gcloud
        pushd "${HOME}/gcloud"
        tar xzf google-cloud-sdk.tar.gz
        ./google-cloud-sdk/install.sh --usage-reporting false \
            --path-update false --command-completion false
        popd
    fi
    ${HOME}/gcloud/google-cloud-sdk/bin/gcloud -q components update beta
fi

gcloud auth activate-service-account --key-file="${GOOGLE_CREDENTIALS}"
gcloud config set project "${GOOGLE_PROJECT_ID}"

echo "TRAVIS_COMMIT : $TRAVIS_COMMIT"
echo "TRAVIS_BRANCH : $TRAVIS_BRANCH"
echo "GOOGLE_PROJECT_ID : $GOOGLE_PROJECT_ID"