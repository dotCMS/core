export TEST_TYPE=integration

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
    echo "Running integration tests with extra parameters [${EXTRA_PARAMS}]"
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

NOW=$(date +"%y-%m-%d")
export GOOGLE_STORAGE_JOB_FOLDER="cicd-246518-tests/${NOW}/${BUILD_HASH}/${databaseType}"

echo ""
echo "================================================================================"
echo "================================================================================"
echo "  >>>   DB: ${databaseType}"
echo "  >>>   TEST PARAMETERS: ${EXTRA_PARAMS}"
echo "  >>>   BUILD FROM: ${BUILD_FROM}"
echo "  >>>   BUILD ID: ${BUILD_ID}"
echo "  >>>   GIT HASH: ${BUILD_HASH}"
echo "  >>>   GOOGLE_STORAGE_JOB_FOLDER: ${GOOGLE_STORAGE_JOB_FOLDER}"
echo "================================================================================"
echo "================================================================================"
echo ""

if [ ! -z "${WAIT_DB_FOR}" ]
then
    echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo "            Requested sleep of [${WAIT_DB_FOR}]", waiting for the db?
    echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo ""
    sleep ${WAIT_DB_FOR}
fi

cd /build/src/core/dotCMS \
&& ./gradlew integrationTest ${EXTRA_PARAMS}

# Required code, without it gradle will exit 1 killing the docker container
gradlewReturnCode=$?
export CURRENT_JOB_BUILD_STATUS=${gradlewReturnCode}

echo ""
if [ ${gradlewReturnCode} == 0 ]
then
  echo "  >>> Integration tests executed successfully <<<"
else
  echo "  >>> Integration tests failed <<<" >&2
fi

echo ""
echo "  >>> Copying gradle reports to [/custom/output/reports/]"
echo ""

# Copying gradle report
cp -R /build/src/core/dotCMS/build/reports/tests/integrationTest/ /custom/output/reports/html/
#cp -R /build/src/core/dotCMS/build/test-results/integrationTest/ /custom/output/reports/xml/

# Do we want to export the resulting reports to google storage?
if [ ! -z "${EXPORT_REPORTS}" ]
then
  if $EXPORT_REPORTS ;
  then
    bash /build/publish.sh
    ignoring_return_value=$?
  fi
fi

if [ ${gradlewReturnCode} == 0 ]
then
  exit 0
else
  exit 1
fi