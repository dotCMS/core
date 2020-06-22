#!/bin/bash

source .cicd/seed.sh

if [[ -z  ]]; then
  travis/pipeline.sh buildIntegration
else
  travis/pipeline.sh buildTestsBase
fi