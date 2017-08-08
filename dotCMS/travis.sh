#!/bin/bash

echo "=============================="
pwd
ls -al
echo "=============================="

./dotCMS/gradlew -p dotCMS/ war

echo "=============================="
echo "dotCMS"
ls -al dotCMS
echo "dotCMS/build"
ls -al dotCMS/build
echo "dotCMS/build/classes"
ls -al dotCMS/build/classes
echo "dotCMS/build/classes/main"
ls -al dotCMS/build/classes/main
echo "=============================="

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
  echo "--------------------------------------------------"
  echo ">> Running sonar scanner in change preview mode"
  echo ">> For pull request [$TRAVIS_PULL_REQUEST]"
  echo ">> For repository [$TRAVIS_REPO_SLUG]"
  echo "--------------------------------------------------"

  sonar-scanner \
    -Dsonar.analysis.mode=preview \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.github.oauth=$SONAR_GITHUB_TOKEN \
    -Dsonar.github.repository=dotCMS/core \
    -Dsonar.scanner.skip=false \
    -Dsonar.java.binaries=dotCMS/build/classes/main \
    -Dsonar.java.libraries=dotCMS/build/pluginsLib/*.jar

else
  sonar-scanner \
    -Dsonar.scanner.skip=false \
    -Dsonar.java.binaries=dotCMS/build/classes/main \
    -Dsonar.java.libraries=dotCMS/build/pluginsLib/*.jar
fi
