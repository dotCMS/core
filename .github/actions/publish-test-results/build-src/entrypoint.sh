#!/usr/bin/env bash

cd /srv

echo "Copying ${INPUT_PROJECT_ROOT}/cicd/local-cicd.sh to ."
cp ${INPUT_PROJECT_ROOT}/cicd/local-cicd.sh .
source ./local-cicd.sh
source ./test-results.sh

case "${INPUT_MODE}" in
  init)
    initResults
    ;;
  close)
    closeResults
    setOutputs
    printStatus
    ;;
  *)
    persistResults
    setOutputs
    printStatus
    ;;
esac

exit 0
