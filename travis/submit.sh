#!/bin/bash

# Using this approach in order to avoid travis timeouts waithing for log movement

function bell() {
  while true; do
    echo -e "\a"
    sleep 60
  done
}
bell &
gcloud builds submit \
  --config=travis/cloudbuild.yaml \
  --substitutions=_GIT_BRANCH_COMMIT=$TRAVIS_BRANCH,COMMIT_SHA=$TRAVIS_COMMIT,_LICENSE_KEY=$LICENSE,_CUSTOM_RUN_ID=$TRAVIS_BUILD_ID .
exit $?
