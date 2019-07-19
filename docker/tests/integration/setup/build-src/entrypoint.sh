#!/bin/bash

# fists we need to do some clean up
rm -rf /custom/logs/*
rm -rf /custom/reports/*

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

    if [ ! -z "${TEST_PARAMS_ENV}" ]
    then
        echo "Running integration tests with extra parameters [${TEST_PARAMS_ENV}]"
    fi

    echo "Running for database: [${DB_TYPE_ENV}]"
    export databaseType=${DB_TYPE_ENV}

    cd /build/src/core/dotCMS \
    && ./gradlew integrationTest ${TEST_PARAMS_ENV}

    # Required code, without it gradle will exit 1 killing the docker container
    if [ $? -eq 0 ]
    then
      echo "Integration tests executed successfully"
    else
      echo "Integration tests failed" >&2
    fi

    echo "----"
    echo "----"
    echo "Copying gradle report to [/custom/reports/]"

    # Copying gradle report
    cp -R /build/src/core/dotCMS/build/reports/tests/integrationTest/ /custom/reports/
else

    echo "Running user CMD..."
    exec -- "$@"
fi
