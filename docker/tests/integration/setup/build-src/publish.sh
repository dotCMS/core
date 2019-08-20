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

# Now we want to add the logs link at the end of index.html results report file
logURL="https://storage.googleapis.com/${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/logs/dotcms.log"
logsLink="<h2 class=\"summaryGroup infoBox\" style=\"margin: 40px; padding: 15px;\"><a href=\"${logURL}\" target=\"_blank\">dotcms.log</a></h2>"

if [[ "${TEST_TYPE}" == "unit"  ]]; then
  echo "
  ${logsLink}
  " >> ${outputFolder}/reports/html/index.html
else
  echo "
  ${logsLink}
  " >> ${outputFolder}/reports/html/integrationTest/index.html
fi

gcloud auth activate-service-account --key-file="${credentialsFile}"
gsutil -m -q cp -a public-read -r ${outputFolder} ${buckedProtocol}${GOOGLE_STORAGE_JOB_FOLDER}

bash /build/github_status.sh
ignoring_return_value=$?

bash /build/printStatus.sh
ignoring_return_value=$?