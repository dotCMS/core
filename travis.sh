#!/bin/bash

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
  echo "Running sonar scanner in change preview mode"

  sonar-scanner \
    -Dsonar.analysis.mode=preview \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.scanner.skip=false

else
  sonar-scanner
fi