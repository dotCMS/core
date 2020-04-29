BASE_GOOGLE_URL="https://storage.googleapis.com/"

if [[ "${TEST_TYPE}" == "unit" ]]; then
  reportsCommitIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/reports/html/index.html"
  reportsBranchIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/reports/html/index.html"
elif [[ "${TEST_TYPE}" == "integration" ]]; then
  reportsCommitIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/reports/html/integrationTest/index.html"
  reportsBranchIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/reports/html/integrationTest/index.html"
else
  reportsCommitIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/reports/html/curlTest/index.html"
  reportsBranchIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/reports/html/curlTest/index.html"
fi
logCommitURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/logs/dotcms.log"
logBranchURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/logs/dotcms.log"

echo ""
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[1;36m                                                REPORTING\e[0m"
echo
echo -e "\e[31m   ${reportsBranchIndexURL}\e[0m"
echo -e "\e[31m   ${logBranchURL}\e[0m"
echo
echo -e "\e[31m   ${reportsCommitIndexURL}\e[0m"
echo -e "\e[31m   ${logCommitURL}\e[0m"
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