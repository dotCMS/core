#!/bin/bash

# Shallow clonning the repositories
bash /build/download.sh
returnCode=$?
if [ ${returnCode} != 0 ]
then
  exit ${returnCode}
fi

# Validate data in the clonned repositories
bash /build/validate.sh
returnCode=$?
if [ ${returnCode} != 0 ]
then
  exit ${returnCode}
fi