#!/bin/bash

bash ./testing/printStoragePaths.sh
ignoring_return_value=$?

outputFolder="/usr/src/app/karma_html"
buckedProtocol="gs://"


# Do we have service account permissions
if [ -z "${GOOGLE_CREDENTIALS_BASE64}" ]
then
    echo ""
    echo "======================================================================================"
    echo " >>>      'GOOGLE_CREDENTIALS_BASE64' environment variable NOT FOUND               <<<"
    echo "======================================================================================"
    exit 1
fi

# Validating if we have something to copy
if [ -z "$(ls -A $outputFolder)" ]; then
   echo ""
   echo "================================================================"
   echo "           >>> EMPTY [${outputFolder}] FOUND <<<"
   echo "================================================================"
   exit 1
fi

echo $GOOGLE_CREDENTIALS_BASE64 | base64 -d - > $GOOGLE_CREDENTIALS_FILE_PATH

echo ""
echo "  >>> Pushing reports and logs to [${buckedProtocol}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}] <<<"
echo "  >>> Pushing reports and logs to [${buckedProtocol}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}] <<<"
echo ""

gcloud auth activate-service-account --key-file="${GOOGLE_CREDENTIALS_FILE_PATH}"
gsutil -m -q cp -a public-read -r ${outputFolder} ${buckedProtocol}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}

# When the bucket has the branch name we need to clean up the bucket first
gsutil -q rm ${buckedProtocol}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/**
gsutil -m -q cp -a public-read -r ${outputFolder} ${buckedProtocol}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}
