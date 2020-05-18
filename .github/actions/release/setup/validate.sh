#!/bin/bash

MISSING_VALUES=false

foundInFile() {
  if grep -qF "${1}" $2; then
    return 0
  else
    echo
    echo -e "\e[1;31m  >>> Value [${1}] not found found in file [${2}] <<<\e[0m"
    echo
    return 1
  fi
}

# ************************************************************
# Core
# Validations on the .gitmodules
STRING_TO_CHECK="branch = release-${RELEASE_VERSION}"
FILE_TO_CHECK=${CORE_REPOSITORY_FOLDER}.gitmodules
foundInFile "${STRING_TO_CHECK}" "${FILE_TO_CHECK}"
returnCode=$?
if [ ${returnCode} != 0 ]
then
  MISSING_VALUES=true
fi

# Validations on the dependencies.gradle
STRING_TO_CHECK="name: 'ee', version: '${RELEASE_VERSION}'"
FILE_TO_CHECK=${CORE_REPOSITORY_FOLDER}dotCMS/dependencies.gradle
foundInFile "${STRING_TO_CHECK}" "${FILE_TO_CHECK}"
returnCode=$?
if [ ${returnCode} != 0 ]
then
  MISSING_VALUES=true
fi

# Validations on the gradle.properties
STRING_TO_CHECK="dotcmsReleaseVersion=${RELEASE_VERSION}"
FILE_TO_CHECK=${CORE_REPOSITORY_FOLDER}dotCMS/gradle.properties
foundInFile "${STRING_TO_CHECK}" "${FILE_TO_CHECK}"
returnCode=$?
if [ ${returnCode} != 0 ]
then
  MISSING_VALUES=true
fi

# Validations on the gradle.properties
STRING_TO_CHECK="coreWebReleaseVersion=${RELEASE_VERSION}"
FILE_TO_CHECK=${CORE_REPOSITORY_FOLDER}dotCMS/gradle.properties
foundInFile "${STRING_TO_CHECK}" "${FILE_TO_CHECK}"
returnCode=$?
if [ ${returnCode} != 0 ]
then
  MISSING_VALUES=true
fi

# ************************************************************
# Enterprise
# Validations on the gradle.properties
STRING_TO_CHECK="dotcmsReleaseVersion=${RELEASE_VERSION}"
FILE_TO_CHECK=${ENTERPRISE_REPOSITORY_FOLDER}gradle.properties
foundInFile "${STRING_TO_CHECK}" "${FILE_TO_CHECK}"
returnCode=$?
if [ ${returnCode} != 0 ]
then
  MISSING_VALUES=true
fi

# ************************************************************
# core-web
# Validations on the package.json
STRING_TO_CHECK="\"version\": \"${RELEASE_VERSION}\""
FILE_TO_CHECK=${CORE_WEB_REPOSITORY_FOLDER}package.json
foundInFile "${STRING_TO_CHECK}" "${FILE_TO_CHECK}"
returnCode=$?
if [ ${returnCode} != 0 ]
then
  MISSING_VALUES=true
fi

# ==========================================================
# Validation failed!
if [ "$MISSING_VALUES" = true ]; then
  exit 1
fi
# ==========================================================