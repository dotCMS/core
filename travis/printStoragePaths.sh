BASE_GOOGLE_URL="https://storage.googleapis.com/"

CURRENT_BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
if [ "$TRAVIS_PULL_REQUEST" = "false" ];
then
  CURRENT_BRANCH=$TRAVIS_BRANCH
fi

if [[ "${TEST_TYPE}" == "unit" ]]; then
  GOOGLE_STORAGE_JOB_COMMIT_FOLDER="cicd-246518-tests/${TRAVIS_COMMIT_SHORT}/unit"
  GOOGLE_STORAGE_JOB_BRANCH_FOLDER="cicd-246518-tests/${CURRENT_BRANCH}/unit"
  reportsCommitIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/reports/html/index.html"
  reportsBranchIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/reports/html/index.html"
elif [[ "${TEST_TYPE}" == "curl" ]]; then
  GOOGLE_STORAGE_JOB_COMMIT_FOLDER="cicd-246518-tests/${TRAVIS_COMMIT_SHORT}/${DB_TYPE}"
  GOOGLE_STORAGE_JOB_BRANCH_FOLDER="cicd-246518-tests/${CURRENT_BRANCH}/${DB_TYPE}"
  reportsCommitIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/reports/html/curlTest/index.html"
  reportsBranchIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/reports/html/curlTest/index.html"
else
  GOOGLE_STORAGE_JOB_COMMIT_FOLDER="cicd-246518-tests/${TRAVIS_COMMIT_SHORT}/${DB_TYPE}"
  GOOGLE_STORAGE_JOB_BRANCH_FOLDER="cicd-246518-tests/${CURRENT_BRANCH}/${DB_TYPE}"
  reportsCommitIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/reports/html/integrationTest/index.html"
  reportsBranchIndexURL="${BASE_GOOGLE_URL}${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/reports/html/integrationTest/index.html"
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
if [ "$TRAVIS_PULL_REQUEST" != "false" ];
then
  echo "   GITHUB pull request: [https://github.com/dotCMS/core/pull/${TRAVIS_PULL_REQUEST}]"
  echo
fi
echo -e "\e[36m==========================================================================================================================\e[0m"
echo -e "\e[36m==========================================================================================================================\e[0m"
echo ""