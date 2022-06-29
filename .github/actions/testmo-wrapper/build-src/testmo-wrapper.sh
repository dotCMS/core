#!/usr/bin/env bash

#############################
# Script: testmo-wrapper.sh
# Collection of functions that support sending results to Testmo

export TESTMO_URL=${INPUT_TESTMO_URL}
export TESTMO_TOKEN=${INPUT_TESTMO_TOKEN}

run_url="${INPUT_GITHUB_SERVER_URL}/${INPUT_GITHUB_REPOSITORY}/actions/runs/${INPUT_GITHUB_RUN_ID}"
testmo_run_id_file='./testmo_run_id.txt'
[[ "${INPUT_DEBUG}" == 'true' ]] && debug_param='--debug'
testmo_run_id_prefix='Created new test automation run (ID: '
testmo_run_id_suffix=')'
#[[ -n "${INPUT_GITHUB_ACTOR}" ]] && title_suffix=" - ${INPUT_GITHUB_ACTOR}"
[[ -n "${INPUT_BUILD_ID}" ]] && title_suffix="${title_suffix} (${INPUT_BUILD_ID})"

# Initialize npm project to install testmo-cli
function init {
  executeCmd 'npm init -y'
  executeCmd 'npm install'
  executeCmd 'npm ci'
  executeCmd 'npm install --no-save @testmo/testmo-cli'
}

# Add resources to JSON file to be sent as metadata later
function addResources {
  executeCmd "npx testmo automation:resources:add-field
    --name git
    --type string
    --value ${INPUT_GITHUB_SHA}
    --resources resources.json
    ${debug_param}"
    [[ ${cmd_result} != 0 ]] && echo 'Error executing command' && exit 1
  executeCmd "npx testmo automation:resources:add-link
    --name build
    --url ${run_url}
    --resources resources.json
    ${debug_param}"
  [[ ${cmd_result} != 0 ]] && echo 'Error executing command' && exit 2
}

# Creates a metadata for thread at Testmo
function createThread {
  executeCmd "npx testmo automation:run:create
    --instance ${INPUT_TESTMO_URL}
    --project-id ${INPUT_TESTMO_PROJECT_ID}
    --name '${INPUT_TEST_TYPE^} Tests ${title_suffix}'
    --source ${INPUT_TEST_TYPE}-tests
    --resources resources.json
    ${debug_param}
    > ${testmo_run_id_file}"
  [[ ${cmd_result} != 0 ]] && echo 'Error executing command' && exit 3
  cat ${testmo_run_id_file}
  setTestmoOutputs
}

# Initializes, add resources and create thread for Testmo
function addThreadResources {
  init
  addResources
  createThread
}

# Submit test results to Testmo
function submit {
  init
  addResources
  executeCmd "npx testmo automation:run:submit
    --instance ${INPUT_TESTMO_URL}
    --project-id ${INPUT_TESTMO_PROJECT_ID}
    --name '${INPUT_TEST_TYPE^} Tests ${title_suffix}'
    --source ${INPUT_TEST_TYPE}-tests
    --resources resources.json
    --results '${INPUT_TESTS_RESULTS_LOCATION}'
    ${debug_param}
    > ${testmo_run_id_file}"
  [[ ${cmd_result} != 0 ]] && echo 'Error executing command' && exit 4
  cat ${testmo_run_id_file}
  setTestmoOutputs true
}

# Submit thread test results to Testmo
function submitThread {
  init
  export CI_INDEX=${INPUT_CI_INDEX}
  export CI_TOTAL=${INPUT_CI_TOTAL}
  executeCmd "npx testmo automation:run:submit-thread
    --instance ${TESTMO_URL}
    --run-id ${INPUT_TESTMO_RUN_ID}
    --results '${INPUT_TESTS_RESULTS_LOCATION}'
    ${debug_param}"
  [[ ${cmd_result} != 0 ]] && echo 'Error executing command' && exit 5
  setTestmoOutputs true
}

# Send a complete event for threads within execution at Testmo
function complete {
  init
  executeCmd "npx testmo automation:run:complete
    --instance ${TESTMO_URL}
    --run-id ${INPUT_TESTMO_RUN_ID}
    ${debug_param}"
  [[ ${cmd_result} != 0 ]] && echo 'Error executing command' && exit 6
}

# Extracts Testmo run id from output
function extractTestmoRunId {
  if [[ -n "${INPUT_TESTMO_RUN_ID}" ]]; then
    echo ${INPUT_TESTMO_RUN_ID}
    return
  fi
  if [[ "${INPUT_TEST_TYPE}" == 'integration' && "${INPUT_DEBUG}" == 'true' ]]; then
    testmo_run_id_prefix='Run CreatedAutomationRun { id: '
    testmo_run_id_suffix='}'
  fi
  echo $(grep "${testmo_run_id_prefix}" ${testmo_run_id_file} | sed -n "s/${testmo_run_id_prefix}//p" | sed -n "s/${testmo_run_id_suffix}//p")
}

# Sets Testmo run id an report location (when specified to) as outputs
#
# $1: setReport: flag telling to set the report location as well
function setTestmoOutputs {
  local testmo_run_id=$(extractTestmoRunId)

  echo "Run ID: ${testmo_run_id}"
  setOutput testmo_run_id ${testmo_run_id}

  local setReport=${1}
  if [[ "${setReport}" == 'true' ]]; then
    local tests_report_url="${INPUT_TESTMO_URL}/automation/runs/view/${testmo_run_id}"
    echo "Tests report URL: ${tests_report_url}"
    setOutput tests_report_url ${tests_report_url}
    if [[ "${INPUT_TEST_TYPE}" ==  'integration' && -n "${INPUT_CI_LABEL}" ]]; then
      setOutput ${INPUT_CI_LABEL}_tests_report_url ${tests_report_url}
    fi
  fi
}
