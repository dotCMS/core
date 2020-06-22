#!/bin/bash

function usage {
  echo "usage: ${0} <target> <operation>"
  echo "  target: only two values is accepted: 'travis' or 'github', fallbacks to ${DEFAULT_CLOUD_PROVIDER}"
  echo "  operation: identified operation to perform (e.g. 'buildBase' or )"
}

: ${DOT_CICD_PATH:="./dotcicd"} && export DOT_CICD_PATH
: ${DOT_CICD_LIB:="${DOT_CICD_PATH}/library"} && export DOT_CICD_LIB
: ${DOT_CICD_VERSION:="1.0"} && export DOT_CICD_VERSION
export DOT_CICD_CLOUD_PROVIDER=travis

if [[ "${DOT_CICD_CLOUD_PROVIDER}" == "travis" ]]; then
  export DOT_CICD_PERSIST="google"
elif [[ "${DOT_CICD_CLOUD_PROVIDER}" == "github" ]]; then
  export DOT_CICD_PERSIST="github"
fi

echo "#############"
echo "dot-cicd vars"
echo "#############"
echo "DOT_CICD_PATH: ${DOT_CICD_PATH}"
echo "DOT_CICD_LIB: ${DOT_CICD_LIB}"
echo "DOT_CICD_VERSION: ${DOT_CICD_VERSION}"
echo "DOT_CICD_CLOUD_PROVIDER: ${DOT_CICD_CLOUD_PROVIDER}"
echo "DOT_CICD_PERSIST: ${DOT_CICD_PERSIST}"
echo "DOT_CICD_TARGET: ${DOT_CICD_TARGET}"
echo

if [[ $# == 0 ]]; then
  usage
  exit 1
fi

operation=${1}
if [[ -z "${operation}" ]]; then
  echo "Operation argument was not specified, aborting..."
  usage
  exit 1
fi

if [[ -z "${DOT_CICD_TARGET}" ]]; then
  echo "No target project (DOT_CICD_TARGET variable) has been defined, aborting..."
  exit 1
fi

providerPath=${DOT_CICD_LIB}/pipeline/${DOT_CICD_CLOUD_PROVIDER}
. ${providerPath}/${DOT_CICD_CLOUD_PROVIDER}Common.sh

pipelineScript=${providerPath}/${DOT_CICD_TARGET}/${operation}.sh
if [[ ! -s ${pipelineScript} ]]; then
  echo "Pipeline script associated to operation cannot be found, aborting..."
  exit 1
fi

echo "Executing ${pipelineScript}"
. ${pipelineScript} ${2} ${3} ${4}
