#!/bin/bash

export GOOGLE_CREDENTIALS_FILE_PATH="./credentials.json"

export GOOGLE_STORAGE_JOB_COMMIT_FOLDER="cicd-246518-tests/core-web/${COMMIT_SHORT}"
export GOOGLE_STORAGE_JOB_BRANCH_FOLDER="cicd-246518-tests/core-web/${CURRENT_BRANCH}"

echo 'Running test'
npm test -- dotcms-ui --watch=false --reporters=html,progress
export CURRENT_JOB_BUILD_STATUS=$?

if [ "${GOOGLE_CREDENTIALS_BASE64}" ];
 then
    echo 'Running storage'
    bash ./testing/storage.sh

    echo 'Updating github status'
    bash ./testing/github_status.sh

    exit $CURRENT_JOB_BUILD_STATUS
fi