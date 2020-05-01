#!/bin/bash

# Using this approach in order to avoid travis timeouts waiting for log movement

CURRENT_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
if [ "$TRAVIS_PULL_REQUEST" = "false" ];
then
  CURRENT_BRANCH=$TRAVIS_BRANCH
fi

scriptPah=$(dirname $0)
. "$scriptPah/common.sh"

bell &
gcloud builds submit \
  --config=travis/cloudbuild-sidecar.yaml \
  --substitutions=_GIT_BRANCH_COMMIT=$CURRENT_BRANCH,COMMIT_SHA=$TRAVIS_COMMIT_SHORT,_CUSTOM_RUN_ID=$TRAVIS_COMMIT_SHORT .
exit $?
