#!/bin/bash

CURRENT_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
if [ "$TRAVIS_PULL_REQUEST" = "false" ];
then
  CURRENT_BRANCH=$TRAVIS_BRANCH
fi

# Using this approach in order to avoid travis timeouts waithing for log movement

function bell() {
  while true; do
    echo -e "\a"
    sleep 60
  done
}
bell &

gcloud builds submit \
  --config=travis/cloudrun-test.yml \
  --substitutions=_CURRENT_BRANCH=$CURRENT_BRANCH,_COMMIT_SHORT=$TRAVIS_COMMIT_SHORT,_GOOGLE_CREDENTIALS_BASE64=$GOOGLE_CREDENTIALS_BASE64,_GITHUB_USER=$GITHUB_USER,_GITHUB_USER_TOKEN=$GITHUB_USER_TOKEN,_PULL_REQUEST=$TRAVIS_PULL_REQUEST .
exit $?
