export TEST_TYPE=curl

function resolveLabel() {
  local result=$1
  if [[ $result == 0 ]]; then
    echo "PASS"
  else
    echo "FAIL"
  fi
}

# Validating we have a license file
if [ ! -s "/custom/dotsecure/license/license.dat" ]
then
   echo ""
   echo "================================================================"
   echo " >>> Valid [/custom/dotsecure/license/license.dat] NOT FOUND <<<"
   echo "================================================================"
   exit 1
fi

if [ ! -z "${EXTRA_PARAMS}" ]
then
    echo "Running curl tests with extra parameters [${EXTRA_PARAMS}]"
fi

#  One of ["postgres", "mysql", "oracle", "mssql"]
if [ -z "${databaseType}" ]
then
    echo ""
    echo "======================================================================================"
    echo " >>> 'databaseType' environment variable NOT FOUND, setting postgres as default DB <<<"
    echo "======================================================================================"
    export databaseType=postgres
fi

export GOOGLE_STORAGE_JOB_COMMIT_FOLDER="${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}/${databaseType}"
export GOOGLE_STORAGE_JOB_BRANCH_FOLDER="${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}/${databaseType}"

echo ""
echo "================================================================================"
echo "================================================================================"
echo "  >>>   DB: ${databaseType}"
echo "  >>>   TEST PARAMETERS: ${EXTRA_PARAMS}"
echo "  >>>   BUILD FROM: ${BUILD_FROM}"
echo "  >>>   BUILD ID: ${BUILD_ID}"
echo "  >>>   GIT HASH: ${BUILD_HASH}"
echo "  >>>   GOOGLE_STORAGE_JOB_COMMIT_FOLDER: ${GOOGLE_STORAGE_JOB_COMMIT_FOLDER}"
echo "  >>>   GOOGLE_STORAGE_JOB_BRANCH_FOLDER: ${GOOGLE_STORAGE_JOB_BRANCH_FOLDER}"
echo "================================================================================"
echo "================================================================================"
echo ""

if [ ! -z "${WAIT_SIDECAR_FOR}" ]
then
    echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo "            Requested sleep of [${WAIT_SIDECAR_FOR}]", waiting for the sidecar?
    echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo ""
    sleep ${WAIT_SIDECAR_FOR}
fi

echo ""
echo "========================================================================================================"
echo "Executing... [newman run <collection> -reporters cli,htmlextra --reporter-htmlextra-export] <report file>"
echo "========================================================================================================"
echo ""

# Prepare to run newman for every found postman collection
postmanEnvFile="postman_environment.json"
reportFolder="/build/src/core/dotCMS/build/reports/tests/curlTest"
mkdir -p $reportFolder
cd /build/src/core/dotCMS/src/curl-test
sed -i 's/localhost:8080/sidecar:8080/g' ./$postmanEnvFile
# Create a map to store collection -> newman result
declare -A curlResults
> /build/resultLinks.html

IFS=$'\n'
for f in $(ls *.json);
do
  if [[ "$f" == "$postmanEnvFile" ]]; then
    continue
  fi

  echo "Running newman for collection: \"${f}\""
  collectionName=$(echo "$f"| tr ' ' '_' | cut -f 1 -d '.')
  page="${collectionName}.html"
  resultFile="${reportFolder}/${page}"

  # actual running of postman tests for current collection
  newman run "$f" -e ${postmanEnvFile} --reporters cli,htmlextra --reporter-htmlextra-export $resultFile

  # handle collection results
  curlResults[$collectionName]=$?
  resultLabel=$(resolveLabel ${curlResults[$collectionName]})
  echo "<tr><td><a href=\"./$page\">$f</a></td>
    <td>$resultLabel</td></tr>" >> /build/resultLinks.html
done

cat /build/newmanTestResultsHeader.html /build/resultLinks.html /build/newmanTestResultsFooter.html \
  > "${reportFolder}/index.html" &&
  rm /build/resultLinks.html

curlReturnCode=0
for r in "${!curlResults[@]}"
do
  if [[ ${curlResults[$r]} != 0 ]]; then
    curlReturnCode=1
    break
  fi
done

export CURRENT_JOB_BUILD_STATUS=${curlReturnCode}

echo ""
if [ ${curlReturnCode} == 0 ]
then
  echo "  >>> Curl tests executed successfully <<<"
else
  echo "  >>> Curl tests failed <<<" >&2
fi

echo ""
echo "  >>> Copying gradle reports to [/custom/output/reports/]"
echo ""

# Copying gradle report
cp -R ${reportFolder}/ /custom/output/reports/html/

# Do we want to export the resulting reports to google storage?
if [ ! -z "${EXPORT_REPORTS}" ]
then
  if $EXPORT_REPORTS ;
  then
    bash /build/storage.sh
    ignoring_return_value=$?
  fi
fi

if [ ${curlReturnCode} == 0 ]
then
  exit 0
else
  exit 1
fi