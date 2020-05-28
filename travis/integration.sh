#!/bin/bash

export TEST_TYPE=integration
bash travis/printStoragePaths.sh
ignoring_return_value=$?

scriptPah=$(dirname $0)
. "$scriptPah/common.sh"

bell &
gcloud builds submit \
  --config=travis/cloudrun-integration.yaml \
  --substitutions=_DB_TYPE=$DB_TYPE,_CUSTOM_RUN_ID=$TRAVIS_COMMIT_SHORT,_PULL_REQUEST=$TRAVIS_PULL_REQUEST,_GOOGLE_CREDENTIALS_BASE64=$GOOGLE_CREDENTIALS_BASE64,_GITHUB_USER=$GITHUB_USER,_GITHUB_USER_TOKEN=$GITHUB_USER_TOKEN .
exit $?
