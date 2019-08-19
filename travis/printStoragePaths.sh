NOW=$(date +"%y-%m-%d")
GOOGLE_STORAGE_JOB_FOLDER="cicd-246518-tests/integration/${NOW}/${TRAVIS_COMMIT}/${DB_TYPE}"

BASE_GOOGLE_URL="https://storage.googleapis.com/"
reportsIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/integrationTest/index.html"
logURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/logs/dotcms.log"

echo ""
echo "================================================================================="
echo "================================================================================="
echo "  Storage folder for job: [${GOOGLE_STORAGE_JOB_FOLDER}]"
echo "  Reports URL for job: [${reportsIndexURL}]"
echo "  Log URL for job: [${logURL}]"
if [ "$TRAVIS_PULL_REQUEST" != "false" ];
then
  echo "  GITHUB pull request: [https://github.com/dotCMS/core/pull/${TRAVIS_PULL_REQUEST}]"
fi
echo "================================================================================="
echo "================================================================================="
echo ""