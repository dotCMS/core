#!/usr/bin/env bash

cp ${INPUT_PROJECT_ROOT}/cicd/*.sh .

source ./local-env.sh
source ./local-cicd.sh
souce ./integration-tests.sh

prepareTests

overrideProps

prepareLicense ${LICENSE_FOLDER}
validateLicense ${LICENSE_FILE}

startDependencies

waitFor "${INPUT_DB_TYPE} database" 30

runIntegrationTests
it_result=$?

stopDependencies

if [[ ${it_result} != 0 ]]; then
  echo 'Integration tests FAILED'
  exit ${it_result}
else
  echo 'AMAZING!! Integration tests have run SUCCESSFULLY (epic music starts)'
fi
