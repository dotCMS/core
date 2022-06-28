#!/usr/bin/env bash

cd /srv

cp ${INPUT_PROJECT_ROOT}/cicd/local-cicd.sh .
source ./local-cicd.sh
source ./test-results.sh

setVars
copyResults
trackCoreTests ${INPUT_TESTS_RUN_EXIT_CODE}
printReportLocations
appendLogLocation
checkForToken
persistResults
setOutputs
printStatus
