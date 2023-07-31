#!/usr/bin/env bash

#############################
# Script: test-results.sh
# Collection of functions that support storing results in test-results repo

# Checks for token stored in env-var INPUT_CICD_GITHUB_TOKEN. It not exit the execution with error code 1.
function checkForToken {
  if [[ -z "${INPUT_CICD_GITHUB_TOKEN}" ]]; then
    echo "Error: Test results push token is not defined, aborting..."
    exit 1
  fi

  echo "Test results token found"
}

# Removes specific test type results folder and commit the deletion
function cleanTestFolders {
  if [[ -n "${INPUT_BUILD_ID}" ]]; then
    local folder=./${INPUT_BUILD_ID}/${INPUT_TEST_TYPE}
    if [[ -d ${folder} ]]; then
      rm -rf folder
      echo "Removing ${folder}"
      executeCmd "git commit -m \"Cleaning ${INPUT_TEST_TYPE} tests results with hash '${INPUT_BUILD_HASH}' and branch '${INPUT_BUILD_ID}'\""
    fi
  fi
}

# Resolves results path based on the definition INPUT_TEST_TYPE and DB_TYPE env-vars
#
# $1: path: initial path
function resolveResultsPath {
  local path="${1}"
  [[ -n "${INPUT_TEST_TYPE}" ]] && path="${path}/${INPUT_TEST_TYPE}"
  [[ -n "${INPUT_DB_TYPE}" ]] && path="${path}/${INPUT_DB_TYPE}"
  [[ ${path} =~ ^/.* ]] && path="${path/\//}"
  echo ${path}
}

# Evaluates if the provided build_id is master or release branch
#
# $1: build_id: build id
function isMasterOrRelease {
  local build_id=$1
  if [[ "${build_id}" == 'master' || ${build_id} =~ ^release-[0-9]{2}.[0-9]{2}(.[0-9]{1,2})?$|^v[0-9]{2}.[0-9]{2}(.[0-9]{1,2})?$ ]]; then
    return 1
  fi
  return 0
}

# Resolves results path based on the definition of detected mutlti-branch and build_id
#
# $1: build_id: build id
function resolveTestResultsBase {
  local build_id=$1
  local test_results_path=${INPUT_PROJECT_ROOT}/${INPUT_TESTS_RESULTS_REPO}

  isMasterOrRelease ${build_id}
  local master_or_release=$?

  [[ "${MULTI_BRANCH}" == 'true' && ${master_or_release} == 1 ]] \
    && test_results_path=${test_results_path}_${build_id}

  echo ${test_results_path}
}

# Resolves which is the initial branch to use then cutting a new branch
#
# $1: build_id: build id
# $2: remote_branch: 1 if remote branch exists, 0 otherwise
function resolveInitialBranch {
  local build_id=$1
  local remote_branch=$2

  if [[ ${remote_branch} == 1 ]]; then
    clone_branch=${build_id}
  else
    clone_branch=scratch
  fi

  echo ${clone_branch}
}

# Creates initial branch to add tests results to in case it does not exist
#
# $1: build_id: build id
function initBranchResults {
  local build_id=$1

  cd ${INPUT_PROJECT_ROOT}

  # Resolve test results fully
  local test_results_repo_url=$(resolveRepoUrl ${INPUT_TESTS_RESULTS_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=$(resolveTestResultsBase ${build_id})

  gitRemoteLs ${test_results_repo_url} ${build_id}
  local remote_branch=$?
  local clone_branch=$(resolveInitialBranch ${build_id} ${remote_branch})

  # Clone test-results repo at resolved branch
  gitClone ${test_results_repo_url} ${clone_branch} ${test_results_path}
  cd ${test_results_path}

  # Prepare who is pushing the changes
  gitConfig ${GITHUB_USER}

  # If no remote branch detected create one
  [[ ${remote_branch} != 1 ]] \
    && executeCmd "git checkout -b ${build_id}" \
    && executeCmd "git push ${test_results_repo_url}"
}

# Creates initial branch to add tests results to in case it does not exist
function initResults {
  initBranchResults ${BUILD_ID}
  [[ "${MULTI_BRANCH}" == 'true' ]] && initBranchResults ${INPUT_BUILD_ID}
}

# Creates required directory structure for the provided results folder and copies them to the new location
#
# $1: results_path: to copy to results location
function distResults {
  local results_path=${1}
  if [[ -z "${results_path}" ]]; then
    echo "Cannot add results path since its empty, ignoring"
    exit 1
  fi

  local target_folder=$(resolveResultsPath ${results_path})
  executeCmd "mkdir -p ${target_folder}"
  echo "Adding test results path ${OUTPUT_FOLDER} to: ${target_folder}"
  executeCmd "mv ${OUTPUT_FOLDER}/* ${target_folder}"
}

# Resolves possible conflicts by checking out ours
#
# $1: $build_id: build id
function pullAndResolve {
  local build_id=$1
  local pull_output=$(git pull origin ${build_id})
  [[ $? != 0 ]] \
    && echo "Error pulling from git branch ${build_id}, error code: ${cmd_result}" \
    && exit 1

  local ifs_bak=${IFS}
  while IFS= read -r line; do
    if [[ ${line} =~ ^CONFLICT*$ ]]; then
      local arr=(${line})
      local size=${#arr[@]}
      local last=$(expr $size - 1)
      local conflict_path=${arr[${last}]}
      echo "Conflict detected: at ${conflict_path}, resolving with ours"
      executeCmd "git checkout --ours ${conflict_path}"
      [[ ${cmd_result} != 0 ]] \
        && echo "Error checking out ours for ${conflict_path}, error code: ${cmd_result}" \
        && exit 1
    else
      echo ${line}
    fi
  done <<< "${pull_output}"
  IFS=$ifs_bak
}

# Prepares and copies test results in HTML format and the corresponding log file.
function prepareResults {
  if [[ "${INCLUDE_RESULTS}" == 'true' ]]; then
    executeCmd "mkdir -p ${HTML_REPORTS_FOLDER}"
    echo "Copying ${INPUT_TEST_TYPE} tests reports to [${HTML_REPORTS_FOLDER}]"
    executeCmd "mv ${INPUT_TESTS_RESULTS_REPORT_LOCATION}/* ${HTML_REPORTS_FOLDER}/"

    executeCmd "mkdir -p ${XML_REPORTS_FOLDER}"
    if [[ -f ${INPUT_TESTS_RESULTS_LOCATION} ]]; then
      echo "Copying ${INPUT_TEST_TYPE} tests results to [${XML_REPORTS_FOLDER}]"
      executeCmd "mv ${INPUT_TESTS_RESULTS_LOCATION}/* ${XML_REPORTS_FOLDER}/"
    else
      echo "No results file found at ${INPUT_TESTS_RESULTS_LOCATION}"
    fi
  fi
  if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
    mkdir -p ${LOGS_FOLDER}
    if [[ -f ${INPUT_TESTS_RESULTS_LOG_LOCATION} ]]; then
      echo "Copying ${INPUT_TEST_TYPE} tests logs to [${LOGS_FOLDER}]"
      executeCmd "mv ${INPUT_TESTS_RESULTS_LOG_LOCATION}/*.log ${LOGS_FOLDER}/"
    else
      echo "No log file found at ${INPUT_TESTS_RESULTS_LOG_LOCATION}"
    fi
  fi
}

# Appends to html file a section for the log file locations
function appendLogLocation {
  if [[ ${INPUT_TEST_TYPE} =~ ^(unit|integration)$ ]]; then
    # Now we want to add the logs link at the end of index.html results report file
    logs_link="<h2 class=\"summaryGroup infoBox\" style=\"margin: 40px; padding: 15px;\"><a href=\"${BRANCH_TEST_LOG_URL}\" target=\"_blank\">dotcms.log</a></h2>"
    echo "
    ${logs_link}
    " >> ${HTML_REPORTS_FOLDER}/index.html
  fi
}

# Persists results in 'test-results' repo in the provided INPUT_BUILD_ID branch.
function persistBranchResults {
  local build_id=$1

  cd ${INPUT_PROJECT_ROOT}

  # Resolve test results fully
  local test_results_repo_url=$(resolveRepoUrl ${INPUT_TESTS_RESULTS_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=$(resolveTestResultsBase ${BUILD_ID})

  # Query for remote branch
  gitRemoteLs ${test_results_repo_url} ${build_id}
  local remote_branch=$?
  local clone_branch=$(resolveInitialBranch ${build_id} ${remote_branch})

  # Clone test-results repo at resolved branch
  gitClone ${test_results_repo_url} ${clone_branch} ${test_results_path}

  # Create results folder if ir does not exist and switch to it
  [[ ! -d ${test_results_path} ]] && executeCmd "mkdir -p ${test_results_path}"
  cd ${test_results_path}

  # Prepare who is pushing the changes
  gitConfig ${GITHUB_USER}

  # If no remote branch detected create one
  [[ ${remote_branch} != 1 ]] && executeCmd "git checkout -b ${build_id}"

  # Clean test results folders by removing contents and committing them
  cleanTestFolders
  # Add results to the branch
  distResults .

  # Check for something new to commit
  executeCmd "git branch && git status"
  executeCmd "git status | grep \"nothing to commit, working tree clean\""

  # If there are changes then start the fun part
  if [[ ${cmd_result} != 0 ]]; then
    # Add everything
    executeCmd "git add ."
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error adding to git for ${INPUT_BUILD_HASH} at ${build_id}, error code: ${cmd_result}"
      exit 1
    fi

    # Commit the changes
    executeCmd "git commit -m \"Adding ${INPUT_TEST_TYPE} tests results for ${INPUT_BUILD_HASH} at ${build_id}\""
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error committing to git for ${INPUT_BUILD_HASH} at ${build_id}, error code: ${cmd_result}"
      exit 1
    fi

    # Do not pull unless branch is remote
    if [[ ${remote_branch} == 1 ]]; then
      # Perform a pull just in case
      pullAndResolve ${build_id}
    else
      echo "Not pulling ${build_id} since it is not remote yet"
    fi

    # Finally push the changes
    executeCmd "git push ${test_results_repo_url}"
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error pushing to git for ${INPUT_BUILD_HASH} at ${build_id}, error code: ${cmd_result}"
      exit 1
    fi
  else
    echo 'No changes detected, not committing nor pushing'
  fi
}

# Persists results in 'test-results' repo in the provided INPUT_BUILD_ID branch.
function persistResults {
  prepareResults
  appendLogLocation
  checkForToken

  persistBranchResults ${BUILD_ID}
  [[ "${MULTI_BRANCH}" == 'true' ]] && persistBranchResults ${INPUT_BUILD_ID}
}

# Build necessary results files
function buildResults {
  local results_base_path=$1
  executeCmd "mv ${results_base_path}/postman/reports/xml/* ${INPUT_TESTS_RESULTS_LOCATION}/"
  executeCmd "cat ./postman-results-header.html > ./index.html"
  executeCmd "cat ./*.inc >> ./index.html"
  executeCmd "cat ./postman-results-footer.html >> ./index.html"
  executeCmd "rm ./*.inc"
  executeCmd "rm ./postman-results-header.html"
  executeCmd "rm ./postman-results-footer.html"
  executeCmd "cat ./index.html"
  executeCmd "ls -las ."
  for rc_file in *.rc; do
    local rc_content=$(cat ${rc_file})
    eval "${rc_content}"
    if [[ -z "${test_results_rc}" || ${test_results_rc} != 0 ]]; then
      echo "Error return code at ${rc_file} with content [${rc_content}]"
      return ${test_results_rc}
    fi
  done
}

# Executes logic for matrix partitioned tests such as postman tests
function closeBranchResults {
  local build_id=$1

  [[ "${INPUT_TEST_TYPE}" != 'postman' ]] && return 1

  local test_results_repo_url=$(resolveRepoUrl ${INPUT_TESTS_RESULTS_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=$(resolveTestResultsBase ${build_id})

  gitRemoteLs ${test_results_repo_url} ${build_id}
  local remote_branch=$?
  echo "Branch ${build_id} exists: ${remote_branch}"
  [[ ${remote_branch} != 1 ]] \
    && echo "Tests results branch ${build_id} does not exist, cannot close results" \
    && exit 1

  gitClone ${test_results_repo_url} ${build_id} ${test_results_path}

  cd ${test_results_path}/postman/reports/html
  gitConfig ${GITHUB_USER}
  executeCmd "mkdir -p ${INPUT_TESTS_RESULTS_LOCATION}"
  setOutput tests_results_location ${INPUT_TESTS_RESULTS_LOCATION}

  buildResults ${test_results_path}

  cd ${test_results_path}
  executeCmd "git status"
  executeCmd "git add ."
  executeCmd "git commit -m \"Closing results for branch ${BUILD_ID}\""

  pullAndResolve ${build_id}

  executeCmd "git push ${test_results_repo_url}"
  local push_rc=${cmd_result}

  executeCmd "rm -rf ${INPUT_TESTS_RESULTS_LOCATION}"

  [[ ${push_rc} != 0 ]] \
    && echo "Error pushing to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${cmd_result}" \
    && exit 1

  return 0
}

# Executes logic for matrix partitioned tests such as postman tests
function closeResults {
  closeBranchResults ${BUILD_ID}
  [[ $? != 0 ]] \
    && echo "Error closing results for branch ${BUILD_ID}" \
    && return $?

  local rc=0
  [[ "${MULTI_BRANCH}" == 'true' ]] \
    && closeBranchResults ${INPUT_BUILD_ID} \
    && rc=$?
  return $rc
}

# Set Github Action outputs to be used by other actions
function setOutputs {
  setOutput tests_report_url ${BRANCH_TEST_RESULT_URL} true
  setOutput test_logs_url ${BRANCH_TEST_LOG_URL}

  if [[ "${INPUT_TEST_TYPE}" == 'integration' ]]; then
    setOutput ${INPUT_DB_TYPE}_tests_report_url ${BRANCH_TEST_RESULT_URL} true
    setOutput ${INPUT_DB_TYPE}_test_logs_url ${BRANCH_TEST_LOG_URL}
  fi
}

# Prints information about the status of any test type
function printStatus {
  local pull_request_url="https://github.com/dotCMS/${INPUT_TARGET_PROJECT}/pull/${INPUT_PULL_REQUEST}"
  local results_base_path=${INPUT_PROJECT_ROOT}/${INPUT_TESTS_RESULTS_REPO}

  echo
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[1;36m                                                REPORTING [${BUILD_ID}]\e[0m"
  echo

  [[ "${INCLUDE_RESULTS}" == 'true' ]] && echo -e "\e[31m         ${BRANCH_TEST_RESULT_URL}\e[0m"
  if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
    if [[ "${INPUT_TEST_TYPE}" == 'postman' ]]; then
      cd ${results_base_path}/postman/logs
      if [[ -n "${INPUT_RUN_IDENTIFIER}" ]]; then
        echo -e "\e[31m         ${BRANCH_TEST_LOG_URL}\e[0m"
      else
        for l in *.log
        do
          echo -e "\e[31m         ${GITHUB_PERSIST_BRANCH_URL}/${HTML_REPORTS_LOCATION}/${l%.*}.html\e[0m"
          echo -e "\e[31m         ${GITHUB_PERSIST_BRANCH_URL}/logs/${l}\e[0m"
        done
      fi
    else
      echo -e "\e[31m         ${BRANCH_TEST_LOG_URL}\e[0m"
    fi
  fi

  if [[ "${MULTI_BRANCH}" == 'true' ]]; then
    echo
    echo
    echo -e "\e[1;36m                                                REPORTING [${INPUT_BUILD_ID}]\e[0m"
    echo
    [[ "${INCLUDE_RESULTS}" == 'true' ]] && echo -e "\e[31m         ${COMMIT_TEST_RESULT_URL}\e[0m"
    if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
      if [[ "${INPUT_TEST_TYPE}" == 'postman' ]]; then
        cd ${results_base_path}/postman/logs
        if [[ -n "${INPUT_RUN_IDENTIFIER}" ]]; then
          echo -e "\e[31m         ${COMMIT_TEST_LOG_URL}\e[0m"
        else
          for l in *.log
          do
            echo -e "\e[31m         ${GITHUB_PERSIST_COMMIT_URL}/${HTML_REPORTS_LOCATION}/${l%.*}.html\e[0m"
            echo -e "\e[31m         ${GITHUB_PERSIST_COMMIT_URL}/logs/${l}\e[0m"
          done
        fi
      else
        echo -e "\e[31m         ${COMMIT_TEST_LOG_URL}\e[0m"
      fi
    fi
  fi

  echo
  [[ -n "${INPUT_PULL_REQUEST}" && "${INPUT_PULL_REQUEST}" != 'false' ]] \
    && echo "   GITHUB pull request: [${pull_request_url}]" \
    && echo
  if [[ "${INPUT_TESTS_RESULTS_STATUS}" == 'PASSED' ]]; then
    echo -e "\e[1;32m                                 >>> Tests executed SUCCESSFULLY <<<\e[0m"
  elif [[ "${INPUT_TESTS_RESULTS_STATUS}" == 'FAILED' ]]; then
    echo -e "\e[1;31m                                       >>> Tests FAILED <<<\e[0m"
  fi
  echo
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo
}

# More Env-Vars definition, specifically to results storage
githack_url=$(resolveRepoPath ${INPUT_TESTS_RESULTS_REPO} | sed -e 's/github.com/raw.githack.com/')

isMasterOrRelease ${INPUT_BUILD_ID}
is_master_or_release=$?
if [[ ${is_master_or_release} == 1 ]]; then
  export MULTI_BRANCH=true
  export BUILD_ID="${INPUT_BUILD_ID}_${INPUT_BUILD_HASH}"
else
  export MULTI_BRANCH=false
  export BUILD_ID=${INPUT_BUILD_ID}
fi

export OUTPUT_FOLDER="${INPUT_PROJECT_ROOT}/output"
export HTML_REPORTS_LOCATION='reports/html'
export HTML_REPORTS_FOLDER="${OUTPUT_FOLDER}/${HTML_REPORTS_LOCATION}"
export XML_REPORTS_LOCATION='reports/xml'
export XML_REPORTS_FOLDER="${OUTPUT_FOLDER}/${XML_REPORTS_LOCATION}"
export LOGS_FOLDER="${OUTPUT_FOLDER}/logs"
export BASE_STORAGE_URL="${githack_url}/$(urlEncode ${BUILD_ID})"

export STORAGE_JOB_BRANCH_FOLDER="$(resolveResultsPath '')"
if [[ "${MULTI_BRANCH}" == 'true' ]]; then
  export STORAGE_JOB_COMMIT_FOLDER="${STORAGE_JOB_BRANCH_FOLDER}"
  export COMMIT_BASE_STORAGE_URL="${githack_url}/$(urlEncode ${INPUT_BUILD_ID})"
  export GITHUB_PERSIST_COMMIT_URL="${COMMIT_BASE_STORAGE_URL}/${STORAGE_JOB_COMMIT_FOLDER}"
  export REPORT_PERSIST_COMMIT_URL="${GITHUB_PERSIST_COMMIT_URL}/${HTML_REPORTS_LOCATION}"
  COMMIT_TEST_RESULT_URL=${REPORT_PERSIST_COMMIT_URL}/index.html
  COMMIT_TEST_LOG_URL=${GITHUB_PERSIST_COMMIT_URL}/logs/dotcms.log
  [[ -n "${INPUT_RUN_IDENTIFIER}" ]] \
    && COMMIT_TEST_RESULT_URL=${REPORT_PERSIST_COMMIT_URL}/${INPUT_RUN_IDENTIFIER}.html \
    && COMMIT_TEST_LOG_URL=${GITHUB_PERSIST_COMMIT_URL}/logs/${INPUT_RUN_IDENTIFIER}.log
  export COMMIT_TEST_RESULT_URL
  export COMMIT_TEST_LOG_URL
fi

export GITHUB_PERSIST_BRANCH_URL="${BASE_STORAGE_URL}/${STORAGE_JOB_BRANCH_FOLDER}"
export REPORT_PERSIST_BRANCH_URL="${GITHUB_PERSIST_BRANCH_URL}/${HTML_REPORTS_LOCATION}"
BRANCH_TEST_RESULT_URL=${REPORT_PERSIST_BRANCH_URL}/index.html
BRANCH_TEST_LOG_URL=${GITHUB_PERSIST_BRANCH_URL}/logs/dotcms.log
[[ -n "${INPUT_RUN_IDENTIFIER}" ]] \
  && BRANCH_TEST_RESULT_URL=${REPORT_PERSIST_BRANCH_URL}/${INPUT_RUN_IDENTIFIER}.html \
  && BRANCH_TEST_LOG_URL=${GITHUB_PERSIST_BRANCH_URL}/logs/${INPUT_RUN_IDENTIFIER}.log
export BRANCH_TEST_RESULT_URL
export BRANCH_TEST_LOG_URL

[[ ${INPUT_INCLUDE} =~ ALL|RESULTS ]] && export INCLUDE_RESULTS=true
[[ ${INPUT_INCLUDE} =~ ALL|LOGS ]] && export INCLUDE_LOGS=true

echo "############
Storage vars
############
INPUT_TESTS_RESULTS_REPO: ${INPUT_TESTS_RESULTS_REPO}
BUILD_ID: ${BUILD_ID}
OUTPUT_FOLDER: ${OUTPUT_FOLDER}
HTML_REPORTS_FOLDER: ${HTML_REPORTS_FOLDER}
XML_REPORTS_FOLDER: ${XML_REPORTS_FOLDER}
LOGS_FOLDER: ${LOGS_FOLDER}
BASE_STORAGE_URL: ${BASE_STORAGE_URL}
GITHUB_PERSIST_BRANCH_URL: ${GITHUB_PERSIST_BRANCH_URL}
COMMIT_BASE_STORAGE_URL: ${COMMIT_BASE_STORAGE_URL}
STORAGE_JOB_BRANCH_FOLDER: ${STORAGE_JOB_BRANCH_FOLDER}
STORAGE_JOB_COMMIT_FOLDER: ${STORAGE_JOB_COMMIT_FOLDER}
INPUT_INCLUDE: ${INPUT_INCLUDE}
INCLUDE_RESULTS: ${INCLUDE_RESULTS}
INCLUDE_LOGS: ${INCLUDE_LOGS}
"
