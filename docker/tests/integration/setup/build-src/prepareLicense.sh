#!/bin/bash

set -e

dotCMS_license="$1"

if [ -z ${dotCMS_license} ]
then
    echo ""
    echo ""
    echo "================================================================================"
    echo ">>> No LICENSE_KEY parameter was provided, are you binding the license file? <<<"
    echo "================================================================================"
    echo ""
    echo ""
    exit
else
    if [ ${dotCMS_license} == "YOUR_LICENSE_HERE" ]
    then
        echo ""
        echo ""
        echo "==================================================================================================================================="
        echo ">>> License required in order to run the integration tests, if binding the license file please remove the LICENSE_KEY parameter <<<"
        echo "==================================================================================================================================="
        exit 1
    else

        mkdir -p /custom/dotsecure/license
        touch /custom/dotsecure/license/license.dat
        chmod 777 /custom/dotsecure/license/license.dat

        echo ${dotCMS_license} > /custom/dotsecure/license/license.dat

    fi
fi
