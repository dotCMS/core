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
      [[ "${DEBUG}" == 'true' ]] && echo "Removing ${folder}"
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

# Creates initial branch to add tests results to in case it does not exist
function initResults {
  cd ${INPUT_PROJECT_ROOT}

  # Resolve test results fully
  local test_results_repo_url=$(resolveRepoUrl ${TEST_RESULTS_GITHUB_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=${INPUT_PROJECT_ROOT}/${TEST_RESULTS_GITHUB_REPO}

  gitRemoteLs ${test_results_repo_url} ${BUILD_ID}
  local remote_branch=$?
  echo "Branch ${BUILD_ID} exists: ${remote_branch}"
  # If it does not exist use master
  if [[ ${remote_branch} == 1 ]]; then
    clone_branch=${BUILD_ID}
  else
    clone_branch=scratch
  fi

  # Clone test-results repo at resolved branch
  gitClone ${test_results_repo_url} ${clone_branch} ${test_results_path}
  cd ${test_results_path}

  # Prepare who is pushing the changes
  gitConfig ${GITHUB_USER}

  # If no remote branch detected create one
  [[ ${remote_branch} != 1 ]] \
    && executeCmd "git checkout -b ${BUILD_ID}" \
    && executeCmd "git push ${test_results_repo_url}"
}

# Creates required directory structure for the provided results folder and copies them to the new location
#
# $1: results_path: to copy to results location
function addResults {
  local results_path=${1}
  if [[ -z "${results_path}" ]]; then
    echo "Cannot add results path since its empty, ignoring"
    exit 1
  fi

  local target_folder=$(resolveResultsPath ${results_path})
  mkdir -p ${target_folder}
  echo "Adding test results path ${results_path} to: ${target_folder}"

  executeCmd "cp -R ${OUTPUT_FOLDER}/* ${target_folder}"
}

# Creates required directory structure for the provided results folder and copies them to the new location
#
# $1: results_path: to copy to results location
function addResults {
  local results_path=${1}
  if [[ -z "${results_path}" ]]; then
    echo "Cannot add results path since its empty, ignoring"
    exit 1
  fi

  local target_folder=$(resolveResultsPath ${results_path})
  mkdir -p ${target_folder}
  echo "Adding test results path ${results_path} to: ${target_folder}"

  executeCmd "cp -R ${OUTPUT_FOLDER}/* ${target_folder}"
}

# Persists results in 'test-results' repo in the provided INPUT_BUILD_ID branch.
function persistResults {
  cd ${INPUT_PROJECT_ROOT}

  # Resolve test results fully
  local test_results_repo_url=$(resolveRepoUrl ${TEST_RESULTS_GITHUB_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=${INPUT_PROJECT_ROOT}/${TEST_RESULTS_GITHUB_REPO}

  # Query for remote branch
  gitRemoteLs ${test_results_repo_url} ${BUILD_ID}
  local remote_branch=$?
  echo "Branch ${BUILD_ID} exists: ${remote_branch}"
  # If it does not exist use master
  if [[ ${remote_branch} == 1 ]]; then
    clone_branch=${BUILD_ID}
  else
    clone_branch=scratch
  fi

  # Clone test-results repo at resolved branch
  gitClone ${test_results_repo_url} ${clone_branch} ${test_results_path}
  # Create results folder if ir does not exist and switch to it
  local results_folder=${test_results_path}/projects/${INPUT_TARGET_PROJECT}
  [[ ! -d ${results_folder} ]] && executeCmd "mkdir -p ${results_folder}"
  cd ${results_folder}

  # Prepare who is pushing the changes
  gitConfig ${GITHUB_USER}

  # If no remote branch detected create one
  [[ ${remote_branch} != 1 ]] && executeCmd "git checkout -b ${BUILD_ID}"

  # Clean test results folders by removing contents and committing them
  cleanTestFolders

  if [[ "${MULTI_COMMIT}" == 'true' ]]; then
    # Do not add commit results when the branch is master, otherwise add test results to commit
    addResults ./${INPUT_BUILD_HASH}
    # Add results to current
    addResults ./current
  else
    addResults .
  fi

  # Check for something new to commit
  [[ "${DEBUG}" == 'true' ]] \
    && executeCmd "git branch && git status"

  executeCmd "git status | grep \"nothing to commit, working tree clean\""
  # If there are changes then start the fun part
  if [[ ${cmd_result} != 0 ]]; then
    # Add everything
    executeCmd "git add ."
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error adding to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${cmd_result}"
      exit 1
    fi

    # Commit the changes
    executeCmd "git commit -m \"Adding ${INPUT_TEST_TYPE} tests results for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}\""
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error committing to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${cmd_result}"
      exit 1
    fi

    # Do not pull unless branch is remote
    if [[ ${remote_branch} == 1 ]]; then
      # Perform a pull just in case
      executeCmd "git pull --strategy-option=ours origin ${BUILD_ID}"
      if [[ ${cmd_result} != 0 ]]; then
        echo "Error pulling from git branch ${BUILD_ID}, error code: ${cmd_result}"
      fi
    else
      echo "Not pulling ${BUILD_ID} since it is not remote yet"
    fi

    # Finally push the changes
    executeCmd "git push ${test_results_repo_url}"
    if [[ ${cmd_result} != 0 ]]; then
      echo "Error pushing to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${cmd_result}"
      exit 1
    fi
  else
    echo 'No changes detected, not committing nor pushing'
  fi
}

function gatherResults {
  local results_base_path=$1
  executeCmd "cp -R ${results_base_path}/postman/reports/xml/* ${INPUT_TESTS_RESULTS_LOCATION}/"
  executeCmd "cat ./postman-results-header.html > ./index.html"
  executeCmd "cat ./*.inc >> ./index.html"
  executeCmd "cat ./postman-results-footer.html >> ./index.html"
  executeCmd "rm ./*.inc"
  executeCmd "rm ./postman-results-header.html"
  executeCmd "rm ./postman-results-footer.html"
  executeCmd "cat ./index.html"
  executeCmd "ls -las ."
}

# Executes logic for matrix partitioned tests such as postman tests
function closeResults {
  [[ "${INPUT_TEST_TYPE}" != 'postman' ]] && return 1

  local test_results_repo_url=$(resolveRepoUrl ${TEST_RESULTS_GITHUB_REPO} ${INPUT_CICD_GITHUB_TOKEN} ${GITHUB_USER})
  local test_results_path=${INPUT_PROJECT_ROOT}/${TEST_RESULTS_GITHUB_REPO}

  gitRemoteLs ${test_results_repo_url} ${BUILD_ID}
  local remote_branch=$?
  echo "Branch ${BUILD_ID} exists: ${remote_branch}"
  [[ ${remote_branch} != 1 ]] \
    && echo "Tests results branch ${BUILD_ID} does not exist, cannot close results" \
    && exit 1

  gitClone ${test_results_repo_url} ${BUILD_ID} ${test_results_path}

  local base_path=${test_results_path}/projects/${INPUT_TARGET_PROJECT}
  local results_base_path=${base_path}
  [[ "${MULTI_COMMIT}" == 'true' ]] && results_base_path="${results_base_path}/${INPUT_BUILD_HASH}"

  cd ${results_base_path}/postman/reports/html
  gitConfig ${GITHUB_USER}
  executeCmd "mkdir -p ${INPUT_TESTS_RESULTS_LOCATION}"
  setOutput tests_results_location ${INPUT_TESTS_RESULTS_LOCATION}

  gatherResults ${results_base_path}
  [[ "${MULTI_COMMIT}" == 'true' ]] \
    && results_base_path=${base_path}/current \
    && cd ${results_base_path}/postman/reports/html \
    && gatherResults ${results_base_path}

  cd ${base_path}
  executeCmd "git status"
  executeCmd "git add ."
  executeCmd "git commit -m \"Closing results for branch ${BUILD_ID}\""
  executeCmd "git pull origin ${BUILD}"
  executeCmd "git push ${test_results_repo_url}"
  if [[ ${cmd_result} != 0 ]]; then
    echo "Error pushing to git for ${INPUT_BUILD_HASH} at ${INPUT_BUILD_ID}, error code: ${cmd_result}"
    exit 1
  fi

  for rc_file in *.rc; do
    local rc_content=$(cat ${rc_file})
    eval "${rc_content}"
    if [[ -z "${test_results_rc}" || ${test_results_rc} != 0 ]]; then
      echo "Error return code at ${rc_file} with content [${rc_content}]"
      return ${test_results_rc}
    fi
  done

  return 0
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
  echo "BRANCH_TEST_LOG_URL=${BRANCH_TEST_LOG_URL}" >> ${result_file}

  cat ${result_file}
}

# Prepares and copies test results in HTML format and the corresponding log file.
function copyResults {
  if [[ "${INCLUDE_RESULTS}" == 'true' ]]; then
    executeCmd "mkdir -p ${HTML_REPORTS_FOLDER}"
    echo "Copying ${INPUT_TEST_TYPE} tests reports to [${HTML_REPORTS_FOLDER}]"
    executeCmd "cp -R ${INPUT_TESTS_RESULTS_REPORT_LOCATION}/* ${HTML_REPORTS_FOLDER}/"

    executeCmd "mkdir -p ${XML_REPORTS_FOLDER}"
    echo "Copying ${INPUT_TEST_TYPE} tests results to [${XML_REPORTS_FOLDER}]"
    executeCmd "cp -R ${INPUT_TESTS_RESULTS_LOCATION}/* ${XML_REPORTS_FOLDER}/"
  fi
  if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
    mkdir -p ${LOGS_FOLDER}
    echo "Copying ${INPUT_TEST_TYPE} tests logs to [${LOGS_FOLDER}]"
    executeCmd "cp -R ${INPUT_PROJECT_ROOT}/dotCMS/*.log ${LOGS_FOLDER}/"
  fi
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

# Appends to html file a section for the log file locations
function appendLogLocation {
  if [[ "${INPUT_TEST_TYPE}" != 'postman' ]]; then
    # Now we want to add the logs link at the end of index.html results report file
    logs_link="<h2 class=\"summaryGroup infoBox\" style=\"margin: 40px; padding: 15px;\"><a href=\"${BRANCH_TEST_LOG_URL}\" target=\"_blank\">dotcms.log</a></h2>"
    echo "
    ${logs_link}
    " >> ${HTML_REPORTS_FOLDER}/index.html
  fi
}

# Prints information about the status of any test type
function printStatus {
  local pull_request_url="https://github.com/dotCMS/${INPUT_TARGET_PROJECT}/pull/${INPUT_PULL_REQUEST}"
  local results_base_path=${INPUT_PROJECT_ROOT}/${TEST_RESULTS_GITHUB_REPO}/projects/${INPUT_TARGET_PROJECT}
  [[ "${MULTI_COMMIT}" == 'true' ]] && results_base_path="${results_base_path}/${INPUT_BUILD_HASH}"

  echo
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[36m==========================================================================================================================\e[0m"
  echo -e "\e[1;36m                                                REPORTING\e[0m"
  echo

  cd ${results_base_path}/postman/logs

  if [[ "${MULTI_COMMIT}" == 'true' ]]; then
    [[ "${INCLUDE_RESULTS}" == 'true' ]] && echo -e "\e[31m         ${COMMIT_TEST_RESULT_URL}\e[0m"
    if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
      if [[ "${INPUT_TEST_TYPE}" == 'postman' ]]; then
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
  else
    [[ "${INCLUDE_RESULTS}" == 'true' ]] && echo -e "\e[31m         ${BRANCH_TEST_RESULT_URL}\e[0m"
    if [[ "${INCLUDE_LOGS}" == 'true' ]]; then
      if [[ "${INPUT_TEST_TYPE}" == 'postman' ]]; then
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
  fi

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

if [[ "${INPUT_BUILD_ID}" != 'master' \
    && ! ${INPUT_BUILD_ID} =~ ^release-[0-9]{2}.[0-9]{2}(.[0-9]{1,2})?$|^v[0-9]{2}.[0-9]{2}(.[0-9]{1,2})?$ ]]; then
  export MULTI_COMMIT=false
  export BUILD_ID="${INPUT_BUILD_ID}_${INPUT_BUILD_HASH}"
else
  export MULTI_COMMIT=true
  export BUILD_ID=${INPUT_BUILD_ID}
fi
export OUTPUT_FOLDER="${INPUT_PROJECT_ROOT}/output"
export HTML_REPORTS_LOCATION='reports/html'
export HTML_REPORTS_FOLDER="${OUTPUT_FOLDER}/${HTML_REPORTS_LOCATION}"
export XML_REPORTS_LOCATION='reports/xml'
export XML_REPORTS_FOLDER="${OUTPUT_FOLDER}/${XML_REPORTS_LOCATION}"
export LOGS_FOLDER="${OUTPUT_FOLDER}/logs"
export BASE_STORAGE_URL="${githack_url}/$(urlEncode ${BUILD_ID})/projects/${INPUT_TARGET_PROJECT}"

if [[ "${MULTI_COMMIT}" == 'true' ]]; then
  export STORAGE_JOB_BRANCH_FOLDER="$(resolveResultsPath current)"
  export STORAGE_JOB_COMMIT_FOLDER="$(resolveResultsPath ${INPUT_BUILD_HASH})"
  export GITHUB_PERSIST_COMMIT_URL="${BASE_STORAGE_URL}/${STORAGE_JOB_COMMIT_FOLDER}"
  export REPORT_PERSIST_COMMIT_URL="${GITHUB_PERSIST_COMMIT_URL}/${HTML_REPORTS_LOCATION}"
  COMMIT_TEST_RESULT_URL=${REPORT_PERSIST_COMMIT_URL}/index.html
  COMMIT_TEST_LOG_URL=${GITHUB_PERSIST_COMMIT_URL}/logs/dotcms.log
  [[ -n "${INPUT_RUN_IDENTIFIER}" ]] \
    && COMMIT_TEST_RESULT_URL=${REPORT_PERSIST_COMMIT_URL}/${INPUT_RUN_IDENTIFIER}.html \
    && COMMIT_TEST_LOG_URL=${GITHUB_PERSIST_COMMIT_URL}/logs/${INPUT_RUN_IDENTIFIER}.log
  export COMMIT_TEST_RESULT_URL
  export COMMIT_TEST_LOG_URL
else
  export STORAGE_JOB_BRANCH_FOLDER="$(resolveResultsPath '')"
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

[[ ${INPUT_MODE} =~ ALL|RESULTS ]] && export INCLUDE_RESULTS=true
[[ ${INPUT_MODE} =~ ALL|LOGS ]] && export INCLUDE_LOGS=true

echo "############
Storage vars
############
BUILD_ID: ${BUILD_ID}
OUTPUT_FOLDER: ${OUTPUT_FOLDER}
HTML_REPORTS_FOLDER: ${HTML_REPORTS_FOLDER}
XML_REPORTS_FOLDER: ${XML_REPORTS_FOLDER}
LOGS_FOLDER: ${LOGS_FOLDER}
BASE_STORAGE_URL: ${BASE_STORAGE_URL}
GITHUB_PERSIST_BRANCH_URL: ${GITHUB_PERSIST_BRANCH_URL}
STORAGE_JOB_BRANCH_FOLDER: ${STORAGE_JOB_BRANCH_FOLDER}
STORAGE_JOB_COMMIT_FOLDER: ${STORAGE_JOB_COMMIT_FOLDER}
INPUT_MODE: ${INPUT_MODE}
INCLUDE_RESULTS: ${INCLUDE_RESULTS}
INCLUDE_LOGS: ${INCLUDE_LOGS}
"
