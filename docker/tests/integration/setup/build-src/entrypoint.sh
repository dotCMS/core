#!/bin/bash

# fists we need to do some clean up
rm -rf /custom/output/*

mkdir -p /custom/output/logs
mkdir -p /custom/output/reports/html
#mkdir -p /custom/output/reports/xml

if [[ "${1}" == "integration" || -z "${1}" ]]; then

  bash /build/integrationTests.sh

elif [[ "${1}" == "unit" || -z "${1}" ]]; then

  bash /build/unitTests.sh

else
    echo "Running user CMD..."
    exec -- "$@"
fi