NOW=$(date +"%y-%m-%d")
BASE_GOOGLE_URL="https://storage.googleapis.com/"

if [[ "${TEST_TYPE}" == "unit"  ]]; then
  GOOGLE_STORAGE_JOB_FOLDER="cicd-246518-tests/${NOW}/${TRAVIS_COMMIT}/unit"
  reportsIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/index.html"
else
  GOOGLE_STORAGE_JOB_FOLDER="cicd-246518-tests/${NOW}/${TRAVIS_COMMIT}/integration/${DB_TYPE}"
  reportsIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/integrationTest/index.html"
fi

logURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/logs/dotcms.log"

echo ""
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[1;36m                                                REPORTING\e[0m"
echo
echo -e "\e[31m   ${reportsIndexURL}\e[0m"
echo
echo -e "\e[31m   ${logURL}\e[0m"
echo
if [ "$TRAVIS_PULL_REQUEST" != "false" ];
then
  echo "   GITHUB pull request: [https://github.com/dotCMS/core/pull/${TRAVIS_PULL_REQUEST}]"
  echo
fi
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[36m==========================================================================================================================\e[0m"
echo ""