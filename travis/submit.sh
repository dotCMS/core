#!/bin/bash

# Using this approach in order to avoid travis timeouts waithing for log movement

CURRENT_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
if [ "$TRAVIS_PULL_REQUEST" = "false" ];
then
  CURRENT_BRANCH=$TRAVIS_BRANCH
fi

function bell() {
  while true; do
    echo -e "\a"
    sleep 60
  done
}
bell &
gcloud builds submit \
  --config=travis/cloudbuild.yaml \
  --substitutions=_GIT_BRANCH_COMMIT=$CURRENT_BRANCH,COMMIT_SHA=$TRAVIS_COMMIT_SHORT,_LICENSE_KEY=$LICENSE,_CUSTOM_RUN_ID=$TRAVIS_COMMIT_SHORT .
exit $?
