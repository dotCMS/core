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
  --config=travis/cloudrun.yaml \
  --substitutions=_DB_TYPE=$DB_TYPE,_CUSTOM_RUN_ID=$TRAVIS_COMMIT,_PULL_REQUEST=$TRAVIS_PULL_REQUEST,_GOOGLE_CREDENTIALS_BASE64=$GOOGLE_CREDENTIALS_BASE64,_GITHUB_USER=$GITHUB_USER,_GITHUB_USER_TOKEN=$GITHUB_USER_TOKEN .
exit $?
