#!/bin/bash

source .cicd/seed.sh

fetchCICD

if [[ $? != 0 ]]; then
  echo "Aborting pipeline"
  exit 1
fi
