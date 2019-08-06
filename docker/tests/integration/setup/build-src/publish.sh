#!/bin/bash

outputFolder="/custom/output"
credentialsFile="/build/credentials.json"
buckedBasePath="gs://cicd-246518-tests/integration/"
folderId="${BUILD_HASH}/${databaseType}"
buckedPath="${buckedBasePath}${folderId}"

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
echo "  >>> Pushing reports and logs to [${buckedPath}] <<<"
echo ""

gcloud auth activate-service-account --key-file="${credentialsFile}"
gsutil -m -q cp -a public-read -r ${outputFolder} ${buckedPath}