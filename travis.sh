#!/bin/bash

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
  echo "Running sonar scanner in change preview mode"

  sonar-scanner \
    -Dsonar.host.url=$SONAR_URL \
    -Dsonar.analysis.mode=preview \
    -Dsonar.login=$SONAR_TOKEN \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.oauth=$SONAR_GITHUB_TOKEN \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.scanner.skip=false

else
  sonar-scanner
fi