#!/bin/bash

set -e

dotCMS_license="$1"

if [ -z "${dotCMS_license}" ]
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

    echo ""
    echo ""
    echo "================================================================================"
    echo ">>>                     LICENSE_KEY parameter provided                       <<<"
    echo "================================================================================"
    echo ""
    echo ""

    mkdir -p /custom/dotsecure/license
    touch /custom/dotsecure/license/license.dat
    chmod 777 /custom/dotsecure/license/license.dat

    echo ${dotCMS_license} > /custom/dotsecure/license/license.dat
fi
