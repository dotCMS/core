#!/bin/bash

# Shallow clone core
git clone --depth 1 https://github.com/dotCMS/core.git -b ${RELEASE_BRANCH} $CORE_REPOSITORY_FOLDER
gitReturnCode=$?
if [ ${gitReturnCode} != 0 ]
then
  echo
  echo -e "\e[1;31m  >>> Problem cloning [${RELEASE_BRANCH}] in core repository <<<\e[0m"
  echo
  exit 1
fi

# Shallow clone enterprise
git clone --depth 1 https://${GITHUB_TOKEN}@github.com/dotCMS/enterprise-2.x.git -b ${RELEASE_BRANCH} $ENTERPRISE_REPOSITORY_FOLDER
gitReturnCode=$?
if [ ${gitReturnCode} != 0 ]
then
  echo
  echo -e "\e[1;31m  >>> Problem cloning [${RELEASE_BRANCH}] in enterprise-2.x repository <<<\e[0m"
  echo
  exit 1
fi

# Shallow clone core-web
git clone --depth 1 https://github.com/dotCMS/core-web.git -b ${RELEASE_BRANCH} $CORE_WEB_REPOSITORY_FOLDER
gitReturnCode=$?
if [ ${gitReturnCode} != 0 ]
then
  echo
  echo -e "\e[1;31m  >>> Problem cloning [${RELEASE_BRANCH}] in core-web repository <<<\e[0m"
  echo
  exit 1
fi