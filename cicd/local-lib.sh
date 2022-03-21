#!/usr/bin/env bash

######################
# Script: local-lib.sh
# Collection of common functions used across the pipeline

source ${GITHUB_WORKSPACE}/cicd/local-env.sh

# Prints debug message to stdout
#
# $1: message: message to print
function debug {
  [[ "${DEBUG}" != 'true' ]] && return
  local message=${1}
  echo "::debug::${message}"
}

# Evaluates a provided command to be echoed and then executed setting the result in a variable
#
# $1: cmd: command to execute
function executeCmd {
  local cmd=${1}
  cmd=$(echo ${cmd} | tr '\n' ' \ \n')
  debug "==============
Executing cmd:
==============
${cmd}"
  eval "${cmd}"
  export cmdResult=$?
  echo "cmdResult: ${cmdResult}"
  [[ ${cmdResult} != 0 ]] && echo "::error::Error executing: ${cmd}"
  echo
}

function runCmd() {
  local cmd=${1}
  pushd ${DOTCMS_ROOT}
  executeCmd "${cmd}"
  popd
}

function buildCmd {
  if [[ "${BUILD_ENV}" == 'gradle' ]]; then
    echo "${BUILD_TOOL} createDistPrep"
  elif [[ "${BUILD_ENV}" == 'maven' ]]; then
    echo "${BUILD_TOOL} install -DskipTests"
  else
    echo ''
  fi
}

function runUnitCmd {
  echo "${BUILD_TOOL} test"
}
