#!/bin/bash

# fists we need to do some clean up
rm -rf /custom/output/*

mkdir -p /custom/output/logs
mkdir -p /custom/output/reports/html
#mkdir -p /custom/output/reports/xml

if [[ "${1}" == "dotcms" || -z "${1}" ]]; then

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

    echo ""
    echo "================================================================================"
    echo "================================================================================"
    echo "  >>>   DB: ${databaseType}"
    echo "  >>>   TEST PARAMETERS: ${EXTRA_PARAMS}"
    echo "  >>>   BUILD FROM: ${BUILD_FROM}"
    echo "  >>>   BUILD ID: ${BUILD_ID}"
    echo "  >>>   GIT HASH: ${BUILD_HASH}"
    echo "================================================================================"
    echo "================================================================================"
    echo ""

    cd /build/src/core/dotCMS \
    && ./gradlew integrationTest ${EXTRA_PARAMS}

    # Required code, without it gradle will exit 1 killing the docker container
    gradlewReturnCode=$?
#    echo ">>>"
#    echo ">>> GRADLE EXIT CODE: ${gradlewReturnCode}"
#    echo ">>>"

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
    if [ ! -z "${exportReports}" ]
    then
      if $exportReports ;
      then
        bash /build/publish.sh
      fi
    fi

    if [ ${gradlewReturnCode} == 0 ]
    then
      exit 0
    else
      exit 1
    fi
else

    echo "Running user CMD..."
    exec -- "$@"
fi
