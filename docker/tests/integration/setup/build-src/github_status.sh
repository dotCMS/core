#! /bin/sh

GOOGLE_STORAGE_JOB_FOLDER=$1
GITHUB_STATUS="failure"
if [ ${CURRENT_JOB_BUILD_STATUS} == 0 ]
then
  GITHUB_STATUS="success"
fi

# Examples
# https://storage.googleapis.com/cicd-246518-tests/integration/19-08-13/0253ef83cdfecf5c370fd59ebf80491551b4e0a0/mysql/reports/html/integrationTest/index.html
# https://storage.googleapis.com/cicd-246518-tests/integration/19-08-13/0253ef83cdfecf5c370fd59ebf80491551b4e0a0/mysql/logs/dotcms.log
if [ "$PULL_REQUEST" != "false" ];
then

  reportsIndexURL="${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/integrationTest/index.html"
  logURL="${GOOGLE_STORAGE_JOB_FOLDER}/logs/dotcms.log"

  echo ""
  echo "================================================================================"
  echo "================================================================================"
  echo "  >>>   Storage folder for job: [${GOOGLE_STORAGE_JOB_FOLDER}]"
  echo "  >>>   Reports URL for job: [${reportsIndexURL}]"
  echo "  >>>   Log URL for job: [${logURL}]"
  echo "  >>>   GITHUB pull request: [https://github.com/dotCMS/core/pull/${PULL_REQUEST}]"
  echo "  >>>   Job build status: ${CURRENT_JOB_BUILD_STATUS}"
  echo "================================================================================"
  echo "================================================================================"
  echo ""

  jsonBaseValue="https://api.github.com/repos/dotCMS/core/statuses/"
  jsonAttribute="\"href\": \"${jsonBaseValue}"

  # https://developer.github.com/v3/auth/#via-oauth-tokens

  # Reading the pull request information in order to get the statuses URL (has a github PR identifier)

  # https://developer.github.com/v3/pulls/#get-a-single-pull-request
  jsonResponse=$(curl -u ${GITHUB_USER}:${GITHUB_USER_TOKEN} \
  --request GET https://api.github.com/repos/dotCMS/core/pulls/${PULL_REQUEST} -s)

  # Parse the response json to get the statuses URL
  jsonStatusesAttribute=`echo "$jsonResponse" | grep "${jsonAttribute}\w*\""`
  statusesURL=`echo "$jsonStatusesAttribute" | grep -o "${jsonBaseValue}\w*"`

  # https://developer.github.com/v3/repos/statuses/#create-a-status
  # The state of the status. Can be one of error, failure, pending, or success.
  curl -u ${GITHUB_USER}:${GITHUB_USER_TOKEN} \
  --request POST \
  --data "{
    \"state\": \"${GITHUB_STATUS}\",
    \"description\": \"Log: ${logURL}\",
    \"target_url\": \"${reportsIndexURL}\",
    \"context\": \"${databaseType}/CI/travis\"
  }" \
  $statusesURL -s --output /dev/null
fi