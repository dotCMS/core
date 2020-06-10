#!/bin/bash

export CICD_REPO="https://github.com/dotCMS/dot-cicd.git"
export CICD_BRANCH=
export CICD_FOLDER=dotcicd
export CICD_DEST=${CICD_FOLDER}/library
export CICD_VERSION=
export CICD_TARGET=core
export CICD_TOOL=travis

# Prepares folders for CI/CD
function prepareCICD() {
  mkdir ${CICD_FOLDER}
}



# Clones and checkout a provided repo url with branch (optional)
function gitCloneAndCheckout() {
  local CICD_REPO=$1
  local CICD_BRANCH=$2

  if [[ -z "${CICD_REPO}" ]]; then
    echo "Repo not provided, cannot continue"
    exit 1
  fi

  cloneOk=false
  if [[ ! -z "${CICD_BRANCH}" ]]; then
    echo "Cloning CI/CD repo from ${CICD_REPO} with branch ${CICD_BRANCH} to ${CICD_DEST}"
    git clone ${CICD_REPO} -b ${CICD_BRANCH} ${CICD_DEST}
    if [[ $? != 0 ]]; then
      echo "Error checking out branch '${CICD_BRANCH}', continuing with master"
    else
      cloneOk=true
    fi
  fi

  if [[ $cloneOk == false ]]; then
    echo "Cloning CI/CD repo from ${CICD_REPO} to ${CICD_DEST}"
    git clone ${CICD_REPO} ${CICD_DEST}

    if [[ $? != 0 ]]; then
      echo "Error cloning repo '${CICD_REPO}'"
      exit 1
    fi
  fi
}

# Make bash scripts to be executable
function prepareScripts() {
  pushd ${CICD_DEST}

  for script in $(find . -type f -name "*.sh"); do
    echo "Making ${script} executable"
    chmod +x ${script}
  done

  popd
}

# Fetch CI/CD github repo to include and use its library
function fetchCICD() {
  prepareCICD
  gitCloneAndCheckout ${CICD_REPO} ${CICD_BRANCH}
  prepareScripts
}