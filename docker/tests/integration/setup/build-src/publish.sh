#!/bin/bash

outputFolder="/custom/output"
credentialsFile="/build/credentials.json"
buckedProtocol="gs://"

# Do we have service account permissions
if [ -z "${GOOGLE_CREDENTIALS_BASE64}" ]
then
    echo ""
    echo "======================================================================================"
    echo " >>>      'GOOGLE_CREDENTIALS_BASE64' environment variable NOT FOUND               <<<"
    echo "======================================================================================"
    exit 0
fi

# Do we have service account permissions
if [ -z "${BUILD_HASH}" ]
then
    echo ""
    echo "======================================================================================"
    echo " >>>                'BUILD_HASH' environment variable NOT FOUND                    <<<"
    echo "======================================================================================"
    exit 0
fi

# Validating if we have something to copy
if [ -z "$(ls -A $outputFolder)" ]; then
   echo ""
   echo "================================================================"
   echo "           >>> EMPTY [${outputFolder}] FOUND <<<"
   echo "================================================================"
   exit 0
fi

echo $GOOGLE_CREDENTIALS_BASE64 | base64 -d - > $credentialsFile

echo ""
echo "  >>> Pushing reports and logs to [${buckedProtocol}${GOOGLE_STORAGE_JOB_FOLDER}] <<<"
echo ""

gcloud auth activate-service-account --key-file="${credentialsFile}"
gsutil -m -q cp -a public-read -r ${outputFolder} ${buckedProtocol}${GOOGLE_STORAGE_JOB_FOLDER}

bash /build/github_status.sh
ignoring_return_value=$?

bash /build/printStatus.sh
ignoring_return_value=$?