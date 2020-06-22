#!/bin/bash

if [[ -s .cicd/discover.sh ]]; then
  travis/pipeline.sh buildTestsBase
else
  travis/pipeline.sh buildIntegration
fi