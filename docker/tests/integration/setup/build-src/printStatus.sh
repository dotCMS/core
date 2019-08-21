BASE_GOOGLE_URL="https://storage.googleapis.com/"

if [[ "${TEST_TYPE}" == "unit"  ]]; then
  reportsIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/index.html"
else
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
if [ "$PULL_REQUEST" != "false" ];
then
  echo "   GITHUB pull request: [https://github.com/dotCMS/core/pull/${PULL_REQUEST}]"
fi
echo
if [ ${CURRENT_JOB_BUILD_STATUS} == 0 ]
then
  echo -e "\e[1;32m                                 >>> Tests executed SUCCESSFULLY <<<\e[0m"
  echo
else
  echo -e "\e[1;31m                                       >>> Tests FAILED <<<\e[0m"
  echo
fi
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[36m==========================================================================================================================\e[0m"
echo ""