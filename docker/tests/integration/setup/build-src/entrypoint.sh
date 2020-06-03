#!/bin/bash

# fists we need to do some clean up
rm -rf /custom/output/*

mkdir -p /custom/output/logs
mkdir -p /custom/output/reports/html
#mkdir -p /custom/output/reports/xml

export GOOGLE_STORAGE_JOB_COMMIT_FOLDER="cicd-246518-tests/${BUILD_HASH}"
export GOOGLE_STORAGE_JOB_BRANCH_FOLDER="cicd-246518-tests/${BUILD_ID}"

if [[ "${1}" == "integration" || -z "${1}" ]]; then

  bash /build/integrationTests.sh

elif [[ "${1}" == "unit" || -z "${1}" ]]; then

  bash /build/unitTests.sh

elif [[ "${1}" == "curl" || -z "${1}" ]]; then

  bash /build/curlTests.sh

else
    echo "Running user CMD..."
    exec -- "$@"
fi