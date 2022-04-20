#!/usr/bin/env bash

#########################
# Script: local-cicd.sh
# Collection of common functions used across the pipeline

# Evaluates a provided command to be echoed and then executed setting the result in a variable
#
# $1: cmd command to execute
function executeCmd {
  local cmd=${1}
  cmd=$(echo ${cmd} | tr '\n' ' \ \n')
  echo "==============
Executing cmd:
==============
${cmd}"
  eval "${cmd}"
  export cmd_result=$?
  echo "cmd_result: ${cmd_result}"
  if [[ ${cmd_result} != 0 ]]; then
    echo "Error executing: ${cmd}"
  fi
  echo
}

# HTTP-Encodes a provided string
#
# $1: string: url to encode
function urlEncode {
  local string="${1}"
  local str_len=${#string}
  local encoded=
  local pos c o

  for (( pos=0 ; pos<str_len ; pos++ )); do
     c=${string:$pos:1}
     case "$c" in
        [-_.~a-zA-Z0-9] ) o="${c}" ;;
        * ) printf -v o '%%%02x' "'$c"
     esac
     encoded+="${o}"
  done
  echo "${encoded}"
}

# Resolves results path based on the definition TEST_TYPE and DB_TYPE env-vars
#
# $1: path: initial path
function resolveResultsPath {
  local path="${1}"
  [[ -n "${TEST_TYPE}" ]] && path="${path}/${TEST_TYPE}"
  [[ -n "${DB_TYPE}" ]] && path="${path}/${DB_TYPE}"
  echo "${path}"
}

# TODO: At some point, please change these vars to someone else :P
DEFAULT_GITHUB_USER=victoralfaro-dotcms
DEFAULT_GITHUB_USER_EMAIL='victor.alfaro@dotcms.com'

[[ -z "${BUILD_ID}" ]] && BUILD_ID=${CURRENT_BRANCH}
[[ -z "${BUILD_ID}" ]] && BUILD_ID=${BRANCH}
: ${BUILD_HASH:="${GITHUB_SHA::8}"} && export BUILD_HASH
: ${GITHUB_USER:="${DEFAULT_GITHUB_USER}"} && export GITHUB_USER
: ${GITHUB_USER_EMAIL:="${DEFAULT_GITHUB_USER_EMAIL}"} && export GITHUB_USER_EMAIL
export DOTCMS_GITHUB_ORG=dotCMS
export DOT_CICD_GITHUB_REPO=dot-cicd
export CORE_GITHUB_REPO=core
export ENTERPRISE_GITHUB_REPO=enterprise
export CORE_WEB_GITHUB_REPO=core-web
export DOCKER_GITHUB_REPO=docker
export PLUGIN_SEEDS_GITHUB_REPO=plugin-seeds
export TEST_RESULTS_GITHUB_REPO=test-results
: ${DEBUG:=true} && export DEBUG

echo "###########
Github vars
###########
BUILD_ID: ${BUILD_ID}
BUILD_HASH: ${BUILD_HASH}
GITHUB_RUN_NUMBER: ${GITHUB_RUN_NUMBER}
PULL_REQUEST: ${PULL_REQUEST}
GITHUB_REF: ${GITHUB_REF}
GITHUB_HEAD_REF: ${GITHUB_HEAD_REF}
GITHUB_USER: ${GITHUB_USER}
DOTCMS_GITHUB_ORG: ${DOTCMS_GITHUB_ORG}
CORE_GITHUB_REPO: ${CORE_GITHUB_REPO}
CORE_WEB_GITHUB_REPO: ${CORE_WEB_GITHUB_REPO}
DOCKER_GITHUB_REPO: ${DOCKER_GITHUB_REPO}
TEST_RESULTS_GITHUB_REPO: ${TEST_RESULTS_GITHUB_REPO}
PLUGIN_SEEDS_GITHUB_REPO: ${PLUGIN_SEEDS_GITHUB_REPO}
DEBUG: ${DEBUG}
"

# Builds credentials part for Github repo url, that is Github user token and username associated with it
#
# $1: token: token to use when building credentials part
# $2: user: github username to use in credentials part
function resolveCreds {
  local token=${1}
  local user=${2}
  local creds=

  if [[ -n "${user}" ]]; then
    creds="${user}"
  fi

  if [[ -n "${token}" ]]; then
    local sep=
    [[ -n "${creds}" ]] && sep=':'
    creds="${creds}${sep}${token}"
  fi

  echo ${creds}
}

# Builds credentials and host parts for Github repo url
#
# $1: token: token to use when building credentials part
# $2: user: Github username to use in credentials part
function resolveSite {
  local creds=$(resolveCreds ${1} ${2})
  [[ -n "${creds}" ]] && creds="${creds}@"

  local site="https://${creds}github.com"
  echo ${site}
}

# Builds path: Github organization + repo name
#
# $1: path: Github repo name
function resolvePath {
  local path=${DOTCMS_GITHUB_ORG}/${1}
  echo ${path}
}

# Builds Github repo url
#
# $1: Github repo name
# $2: token to use when building credentials part
# $3: Github username to use in credentials part
function resolveRepoUrl {
  local path=$(resolvePath ${1})
  local site=$(resolveSite ${2} ${3})
  local url=${site}/${path}.git
  echo ${url}
}

# Builds non-cloning Github repo url, that is without the '.git' suffix
#
# $@: same args as resolveRepoUrl
function resolveRepoPath {
  local path=$(resolveRepoUrl $@)
  echo ${path%".git"}
}

# Sets git config globals user.email and user.name
#
# $1: username: Github username
# $2: name: Github user name
function gitConfig {
  local username=${1}
  local name=${2}

  [[ -z "${name}" ]] && name=${username}

  if [[ -n "${GITHUB_USER_EMAIL}" ]]; then
    local email="${GITHUB_USER_EMAIL}"
  else
    local email="${username}@dotcms.com"
  fi

  [[ "${DEBUG}" == 'true' ]] \
    && echo "Git Config:
      git config --global user.email \"${email}\"
      git config --global user.name \"${name}\""

  git config --global user.email "${email}"
  git config --global user.name "${name}"
  git config --global pager.branch false
  git config pull.rebase false

  [[ "${DEBUG}" == 'true' ]] && git config --list
}

# Defaults to a extracted repo name from repo url
#
# $1: repo_rl: repo url
# $2: dest: destination to
function defaultLocation {
  local repo_url=${1}
  local dest=${2}
  [[ -z "${dest}" ]] && dest=$(basename ${repo_url} .git)
  echo ${dest}
}

# Git clones a given repo url, with a specific branch to a specific location.
#
# $1: repo_url: repo url
# $2: branch: branch to check out
# $3: dest: destination to save the repo
function gitClone {
  local repo_url=${1}
  local branch=${2}
  local dest=${3}

  [[ -z "${branch}" ]] && branch='master'
  dest=$(defaultLocation ${repo_url} ${dest})

  echo "Cloning
    repo: ${repo_url}
    branch: ${branch}
    location: ${dest}"

  local git_clone_mode=
  [[ "${GIT_CLONE_STRATEGY}" != 'full' ]] && git_clone_mode='--depth 1'

  if [[ -z "${GIT_TAG}" ]]; then
    local git_branch_params=
    if [[ "${branch}" != 'master' ]]; then
      git_branch_params="--branch ${branch}"
      [[ "${GIT_CLONE_STRATEGY}" != 'full' ]] && git_clone_mode="${git_clone_mode} --single-branch"
    fi
  fi

  local git_clone_params="${git_clone_mode} ${git_branch_params}"
  local clone_cmd="git clone ${git_clone_params} ${repo_url} ${dest}"
  executeCmd "${clone_cmd}"
  [[ ${cmd_result} != 0 ]] && return ${cmd_result}

  if [[ -n "${GIT_TAG}" ]]; then\
    pushd ${dest}
    [[ "${GIT_CLONE_STRATEGY}" != 'full' ]] \
      && executeCmd "git fetch --all" \
      && executeCmd "git fetch --tags"
    executeCmd "git checkout tags/${GIT_TAG} -b ${GIT_TAG}"
    [[ ${cmd_result} != 0 ]] && return ${cmd_result}
    popd
  fi

  return 0
}

# Given a repo url use it to replace the url element in a .gitmodules file in provided location
#
# $1: repo_url: repo url
# $2: dest: destination to save the repo
function gitSubModules {
  local repo_url=${1}
  [[ -z "${repo_url}" ]] && echo "No repo url provided, aborting" && return 1
  local dest=${2}
  [[ -z "${dest}" ]] && echo "No git folder provided, aborting" && return 2

  echo 'Getting submodules'
  pushd ${dest}

  [[ "${DEBUG}" == 'true' ]] \
    && cat .gitmodules \
    && echo "Injecting ${repo_url} to submodule"
  sed -i "s,git@github.com:dotCMS,${repo_url},g" .gitmodules
  [[ "${DEBUG}" == 'true' ]] && cat .gitmodules

  executeCmd "git submodule update --init --recursive"
  [[ ${cmd_result} != 0 ]] && popd && return 3

  local module_path=$(cat .gitmodules| grep "path =" | cut -d'=' -f2 | tr -d '[:space:]')
  pushd ${module_path}
  gitConfig ${GITHUB_USER}
  popd

  if [[ "${SUBMODULE_CHECKOUT}" != 'detached' ]]; then
    # Try to checkout submodule branch and
    local module_branch=$(cat .gitmodules| grep "branch =" | cut -d'=' -f2 | tr -d '[:space:]')

    pushd ${module_path}

    if [[ "${module_branch}" != 'master' ]]; then
      executeCmd "git checkout -b ${module_branch} --track origin/${module_branch}"
    else
      executeCmd "git checkout master"
    fi

    executeCmd "git pull origin ${module_branch}"
    [[ ${cmd_result} != 0 ]] && echo 'Error pulling from submodule' && popd && popd && return 4

    popd
  fi

  popd

  return 0
}

# Git clones with submodules support
#
# $@: same args as gitClone
function gitCloneSubModules {
  gitClone $@
  local clone_result=$?
  [[ ${clone_result} != 0 ]] && return ${clone_result}

  [[ "${DEBUG}" == 'true' ]] && pwd && ls -las .

  local repo_url=${1}
  [[ "${DEBUG}" == 'true' ]] && echo "defaultLocation args: ${repo_url} ${3}"
  local dest=$(defaultLocation ${repo_url} ${3})
  [[ "${DEBUG}" == 'true' ]] && echo "submodules args: $(dirname ${repo_url}) ${dest}"

  gitSubModules $(dirname ${repo_url}) ${dest}
  local sub_result=$?

  return ${sub_result}
}

# Given a repo url and a branch, run a remote list to see if it exists remotely
#
# $1: repo_url: repo url
# $2: build_id: branch to query
function gitRemoteLs {
  local repo_url=${1}
  local build_id=${2}
  return $(git ls-remote --heads ${repo_url} ${build_id} | wc -l | tr -d '[:space:]')
}

# Prints information about the status of any test type
function printStatus {
  local commit_folder=${BASE_STORAGE_URL}/${STORAGE_JOB_COMMIT_FOLDER}
  local branch_folder=${BASE_STORAGE_URL}/${STORAGE_JOB_BRANCH_FOLDER}
  local reports_commit_index_url="${commit_folder}/reports/html/index.html"
  local reports_branch_index_url="${branch_folder}/reports/html/index.html"
  local log_commit_url="${commit_folder}/logs/dotcms.log"
  local log_branch_url="${branch_folder}/logs/dotcms.log"
  local pull_request_url="https://github.com/dotCMS/${INPUT_TARGET_PROJECT}/pull/${INPUT_PULL_REQUEST}"

  echo ""
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[1;36m                                                REPORTING\e[0m"
  echo
  echo -e "\e[31m   ${reports_branch_index_url}\e[0m"
  if [[ "${INPUT_TEST_TYPE}" != 'postman' ]]; then
    echo -e "\e[31m   ${log_branch_url}\e[0m"
  fi
  echo
  echo -e "\e[31m   ${reports_commit_index_url}\e[0m"
  if [[ "${INPUT_TEST_TYPE}" != 'postman' ]]; then
    echo -e "\e[31m   ${log_commit_url}\e[0m"
  fi
  echo
  [[ -n "${INPUT_PULL_REQUEST}" && "${INPUT_PULL_REQUEST}" != 'false' ]] \
    && echo "   GITHUB pull request: [${pull_request_url}]" \
    && echo
  if [[ ${INPUT_TESTS_RUN_EXIT_CODE} == 0 ]]; then
    echo -e "\e[1;32m                                 >>> Tests executed SUCCESSFULLY <<<\e[0m"
  else
    echo -e "\e[1;31m                                       >>> Tests FAILED <<<\e[0m"
  fi
  echo
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo ""
}

# Set Github Action outputs to be used by other actions
function setOutputs {
  echo "::set-output name=test_results_branch_url::${GITHUB_PERSIST_COMMIT_URL}/${REPORTS_LOCATION}/index.html"
  echo "::set-output name=test_results_commit_url::${GITHUB_PERSIST_BRANCH_URL}/${REPORTS_LOCATION}/index.html"
}

# More Env-Vars definition, specifically to results storage
githack_url=$(resolveRepoPath ${TEST_RESULTS_GITHUB_REPO} | sed -e 's/github.com/raw.githack.com/')
export BASE_STORAGE_URL="${githack_url}/$(urlEncode ${BUILD_ID})/projects/${INPUT_TARGET_PROJECT}"
export STORAGE_JOB_COMMIT_FOLDER="$(resolveResultsPath ${BUILD_HASH})"
export STORAGE_JOB_BRANCH_FOLDER="$(resolveResultsPath current)"
export GITHUB_PERSIST_COMMIT_URL="${BASE_STORAGE_URL}/${STORAGE_JOB_COMMIT_FOLDER}"
export GITHUB_PERSIST_BRANCH_URL="${BASE_STORAGE_URL}/${STORAGE_JOB_BRANCH_FOLDER}"

echo "############
Storage vars
############
BASE_STORAGE_URL: ${BASE_STORAGE_URL}
GITHUB_PERSIST_COMMIT_URL: ${GITHUB_PERSIST_COMMIT_URL}
GITHUB_PERSIST_BRANCH_URL: ${GITHUB_PERSIST_BRANCH_URL}
STORAGE_JOB_COMMIT_FOLDER: ${STORAGE_JOB_COMMIT_FOLDER}
STORAGE_JOB_BRANCH_FOLDER: ${STORAGE_JOB_BRANCH_FOLDER}
"
