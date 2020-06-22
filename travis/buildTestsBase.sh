#!/bin/bash

source .cicd/seed.sh

if [[ -z "${DOT_CICD_BRANCH}" ]]; then
  travis/pipeline.sh buildIntegration
else
  travis/pipeline.sh buildTestsBase
fi