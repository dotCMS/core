BASE_GOOGLE_URL="https://storage.googleapis.com/"
reportsIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/reports/html/integrationTest/index.html"
logURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_FOLDER}/logs/dotcms.log"

echo ""
echo -e "\e[34m==========================================================================================================================\e[0m"
echo -e "\e[34m==========================================================================================================================\e[0m"
echo -e "\e[34m                                                REPORTING\e[0m"
echo -e "\e[38;5;214m   ${reportsIndexURL}\e[0m"
echo
echo -e "\e[38;5;214m   ${logURL}\e[0m"
echo
if [ "$PULL_REQUEST" != "false" ];
then
  echo "  GITHUB pull request: [https://github.com/dotCMS/core/pull/${PULL_REQUEST}]"
fi
if [ ${CURRENT_JOB_BUILD_STATUS} == 0 ]
then
  echo "\e[32m            >>> Integration tests executed SUCCESSFULLY <<<\e[0m"
else
  echo "\e[31m                  >>> Integration tests FAILED <<<\e[0m"
fi
echo -e "\e[34m==========================================================================================================================\e[0m"
echo -e "\e[34m==========================================================================================================================\e[0m"
echo ""