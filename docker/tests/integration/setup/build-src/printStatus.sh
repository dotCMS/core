BASE_GOOGLE_URL="https://storage.googleapis.com/"
reportsIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/integrationTest/index.html"
logURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/logs/dotcms.log"

echo ""
echo "================================================================================="
echo "================================================================================="
echo "  Storage folder for job: [${GOOGLE_STORAGE_JOB_FOLDER}]"
echo "  Reports URL for job: [${reportsIndexURL}]"
echo "  Log URL for job: [${logURL}]"
if [ "$PULL_REQUEST" != "false" ];
then
  echo "  GITHUB pull request: [https://github.com/dotCMS/core/pull/${PULL_REQUEST}]"
fi
if [ ${CURRENT_JOB_BUILD_STATUS} == 0 ]
then
  echo "            >>> Integration tests executed SUCCESSFULLY <<<"
else
  echo "                  >>> Integration tests FAILED <<<"
fi
echo "================================================================================="
echo "================================================================================="
echo ""