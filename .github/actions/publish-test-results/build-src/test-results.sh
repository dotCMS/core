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
    [[ -d ${folder} ]] && rm -rf folder && [[ "${DEBUG}" == 'true' ]] && echo "Removing ${folder}"
  fi

  git commit -m "Cleaning ${INPUT_TEST_TYPE} tests results with hash '${INPUT_BUILD_HASH}' and branch '${INPUT_BUILD_ID}'"
}

# Creates required directory structure for the provided results folder and copies them to the new location
#
# $1: results: to copy to results location
function addResults {
  local results=${1}
  if [[ -z "${results}" ]]; then
    echo "Cannot add results since its empty, ignoring"
    exit 1
  fi

  local target_folder=$(resolveResultsPath ${results})
  mkdir -p ${target_folder}
  echo "Adding test results ${results} to: ${target_folder}"

  executeCmd "cp -R ${OUTPUT_FOLDER}/* ${target_folder}"
}

# Persists results in 'test-results' repo in the provided INPUT_BUILD_ID branch.
function persistResults {
  cd ${INPUT_PROJECT_ROOT}

  # Resolve test results fully
  local test_results_repo_url=$(resolveRepoUrl ${TEST_RESULTS_GITHUB_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=${INPUT_PROJECT_ROOT}/${TEST_RESULTS_GITHUB_REPO}

  # Query for remote branch
  gitRemoteLs ${test_results_repo_url} ${INPUT_BUILD_ID}
  local remote_branch=$?
  # If it does not exist use master
  if [[ ${remote_branch} == 1 ]]; then
    branch=${INPUT_BUILD_ID}
  else
    branch=master
  fi

  # Clone test-results repo at resolved branch
  gitClone ${test_results_repo_url} ${branch} ${test_results_path}
  # Create results folder if ir does not exist and switch to it
  local results_folder=${test_results_path}/projects/${INPUT_TARGET_PROJECT}
  [[ ! -d ${results_folder} ]] && executeCmd "mkdir -p ${results_folder}"
  cd ${results_folder}

  # Prepare who is pushing the changes
  gitConfig ${GITHUB_USER}

  # If no remote branch detected create one
  [[ ${remote_branch} != 1 ]] && executeCmd "git checkout -b ${INPUT_BUILD_ID}"

  # Clean test results folders by removing contents and committing them
  cleanTestFolders

  # Do not add commit results when the branch is master, otherwise add test results to commit
  [[ "${INPUT_BUILD_ID}" != "master" ]] && addResults ./${INPUT_BUILD_HASH}
  # Add results to current
  addResults ./current

  # Check for something new to commit
  [[ "${DEBUG}" == 'true' ]] \
    && executeCmd "git branch" \
    && executeCmd "git status"

  executeCmd "git status | grep \"nothing to commit, working tree clean\""
  # If there are changes then start the fun part
  if [[ ${cmd_result} != 0 ]]; then
    # Add everything
    executeCmd "git add ."
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error adding to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${git_result}"
      exit 1
    fi

    # Commit the changes
    executeCmd "git commit -m \"Adding ${INPUT_TEST_TYPE} tests results for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}\""
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error committing to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${git_result}"
      exit 1
    fi

    # Do not pull unless branch is remote
    if [[ ${remote_branch} == 1 ]]; then
      # Perform a pull just in case
      executeCmd "git pull origin ${INPUT_BUILD_ID}"
      if [[ ${cmd_result} != 0 ]]; then
        echo "Error pulling from git branch ${INPUT_BUILD_ID}, error code: ${git_result}"
        exit 1
      fi
    else
      echo "Not pulling ${INPUT_BUILD_ID} since it is not yet remote"
    fi

    # Finally push the changes
    executeCmd "git push ${test_results_repo_url}"
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error pushing to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${git_result}"
      exit 1
    fi

    executeCmd "git status"
  else
    echo 'No changes detected, not committing nor pushing'
  fi
}

# Creates a summary status file for test the specific INPUT_TEST_TYPE, INPUT_DB_TYPE in both commit and branch paths.
#
# $1: results status
function trackCoreTests {
  local status=${1}
  if [[ ${status} == 0 ]]; then
    local result_label=SUCCESS
  else
    local result_label=FAIL
  fi

  local result_file=${OUTPUT_FOLDER}/job_results.source
  echo "Tracking job results in ${result_file}"
  touch ${result_file}
  echo "TEST_TYPE=${INPUT_TEST_TYPE^}" >> ${result_file}
  echo "DB_TYPE=${INPUT_DB_TYPE}" >> ${result_file}
  echo "TEST_TYPE_RESULT=${result_label}" >> ${result_file}
  echo "BRANCH_TEST_RESULT_URL=${BRANCH_TEST_RESULT_URL}" >> ${result_file}
  echo "COMMIT_TEST_RESULT_URL=${COMMIT_TEST_RESULT_URL}" >> ${result_file}
  echo "TEST_LOG_URL=${TEST_LOG_URL}" >> ${result_file}

  cat ${result_file}
}

# Prepares and copies test results in HTML format and the corresponding log file.
function copyResults {
  if [[ "${INCLUDE_RESULTS}" == 'true' ]]; then
    mkdir -p ${REPORTS_FOLDER}
    echo "Copying ${INPUT_TEST_TYPE} tests reports to [${REPORTS_FOLDER}]"
    executeCmd "cp -R ${INPUT_TESTS_RESULTS_REPORT_LOCATION}/* ${REPORTS_FOLDER}/"
  fi
  if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
    mkdir -p ${LOGS_FOLDER}
    echo "Copying ${INPUT_TEST_TYPE} tests logs to [${LOGS_FOLDER}]"
    executeCmd "cp -R ${INPUT_PROJECT_ROOT}/dotCMS/dotcms.log ${LOGS_FOLDER}/"
  fi
}

# Set Github Action outputs to be used by other actions
function setOutputs {
  setOutput tests_report_url ${BRANCH_TEST_RESULT_URL}
  setOutput test_logs_url ${TEST_LOG_URL}
  echo "::notice::Commit test results URL: '${COMMIT_TEST_RESULT_URL}'"

  if [[ "${INPUT_TEST_TYPE}" == 'integration' ]]; then
    setOutput ${INPUT_DB_TYPE}_tests_report_url ${BRANCH_TEST_RESULT_URL}
    setOutput ${INPUT_DB_TYPE}_test_logs_url ${TEST_LOG_URL}
    echo "::notice::[${INPUT_DB_TYPE}] Commit test results URL: '${COMMIT_TEST_RESULT_URL}'"
  fi
}

# Appends to html file a section for the log file locations
function appendLogLocation {
  if [[ "${INPUT_TEST_TYPE}" != 'postman' ]]; then
    # Now we want to add the logs link at the end of index.html results report file
    logs_link="<h2 class=\"summaryGroup infoBox\" style=\"margin: 40px; padding: 15px;\"><a href=\"${TEST_LOG_URL}\" target=\"_blank\">dotcms.log</a></h2>"
    echo "
    ${logs_link}
    " >> ${REPORTS_FOLDER}/index.html
  fi
}

# Prints information about the status of any test type
function printStatus {
  local pull_request_url="https://github.com/dotCMS/${INPUT_TARGET_PROJECT}/pull/${INPUT_PULL_REQUEST}"

  echo
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[1;36m                                                REPORTING\e[0m"
  echo

  [[ "${INCLUDE_RESULTS}" == 'true' ]] && echo -e "\e[31m         ${BRANCH_TEST_RESULT_URL}\e[0m"
  [[ "${INCLUDE_RESULTS}" == 'true' ]] && echo -e "\e[31m         ${COMMIT_TEST_RESULT_URL}\e[0m"
  [[ "${INCLUDE_LOGS}" == 'true' ]] && echo -e "\e[31m         ${TEST_LOG_URL}\e[0m"

  echo
  [[ -n "${INPUT_PULL_REQUEST}" && "${INPUT_PULL_REQUEST}" != 'false' ]] \
    && echo "   GITHUB pull request: [${pull_request_url}]" \
    && echo
  if [[ "${INPUT_TESTS_RESULTS_STATUS}" == 'PASSED' ]]; then
    echo -e "\e[1;32m                                 >>> Tests executed SUCCESSFULLY <<<\e[0m"
  else
    echo -e "\e[1;31m                                       >>> Tests FAILED <<<\e[0m"
  fi
  echo
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo
}

# More Env-Vars definition, specifically to results storage
githack_url=$(resolveRepoPath ${TEST_RESULTS_GITHUB_REPO} | sed -e 's/github.com/raw.githack.com/')
export OUTPUT_FOLDER="${INPUT_PROJECT_ROOT}/output"
export REPORTS_LOCATION='reports/html'
export REPORTS_FOLDER="${OUTPUT_FOLDER}/${REPORTS_LOCATION}"
export LOGS_FOLDER="${OUTPUT_FOLDER}/logs"
export BASE_STORAGE_URL="${githack_url}/$(urlEncode ${BUILD_ID})/projects/${INPUT_TARGET_PROJECT}"
export STORAGE_JOB_COMMIT_FOLDER="$(resolveResultsPath ${BUILD_HASH})"
export STORAGE_JOB_BRANCH_FOLDER="$(resolveResultsPath current)"
export GITHUB_PERSIST_COMMIT_URL="${BASE_STORAGE_URL}/${STORAGE_JOB_COMMIT_FOLDER}"
export GITHUB_PERSIST_BRANCH_URL="${BASE_STORAGE_URL}/${STORAGE_JOB_BRANCH_FOLDER}"
export REPORT_PERSIST_COMMIT_URL="${GITHUB_PERSIST_COMMIT_URL}/${REPORTS_LOCATION}"
export REPORT_PERSIST_BRANCH_URL="${GITHUB_PERSIST_BRANCH_URL}/${REPORTS_LOCATION}"
export BRANCH_TEST_RESULT_URL=${REPORT_PERSIST_BRANCH_URL}/index.html
export COMMIT_TEST_RESULT_URL=${REPORT_PERSIST_COMMIT_URL}/index.html
export TEST_LOG_URL=${GITHUB_PERSIST_COMMIT_URL}/logs/dotcms.log
[[ ${INPUT_MODE} =~ ALL|RESULTS ]] && export INCLUDE_RESULTS=true
[[ ${INPUT_MODE} =~ ALL|LOGS ]] && export INCLUDE_LOGS=true

echo "############
Storage vars
############
OUTPUT_FOLDER: ${OUTPUT_FOLDER}
REPORTS_FOLDER: ${REPORTS_FOLDER}
LOGS_FOLDER: ${LOGS_FOLDER}
BASE_STORAGE_URL: ${BASE_STORAGE_URL}
GITHUB_PERSIST_COMMIT_URL: ${GITHUB_PERSIST_COMMIT_URL}
GITHUB_PERSIST_BRANCH_URL: ${GITHUB_PERSIST_BRANCH_URL}
STORAGE_JOB_COMMIT_FOLDER: ${STORAGE_JOB_COMMIT_FOLDER}
STORAGE_JOB_BRANCH_FOLDER: ${STORAGE_JOB_BRANCH_FOLDER}
INPUT_MODE: ${INPUT_MODE}
INCLUDE_RESULTS: ${INCLUDE_RESULTS}
INCLUDE_LOGS: ${INCLUDE_LOGS}
"
