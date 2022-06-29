#!/usr/bin/env bash

cd /srv
cp ${INPUT_PROJECT_ROOT}/cicd/local-cicd.sh .

source ./local-cicd.sh
source ./testmo-wrapper.sh

echo "Running Testmo operation ${INPUT_OPERATION}"

# Main logic, decide which operation to run
case "${INPUT_OPERATION}" in
  init)
    init
    ;;
  resources)
    addResources
    ;;
  thread-resources)
    addThreadResources
    ;;
  create-thread)
    createThread
    ;;
  submit)
    submit
    ;;
  submit-thread)
    submitThread
    ;;
  complete)
    complete
    ;;
  *)
    echo "Operation ${INPUT_OPERATION} is invalid"
    ;;
esac

echo "Testmo wrapper ran smoothly, returning 0 as exit code"
exit 0
