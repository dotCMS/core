#!/bin/bash

echo "Executing init_security_plugin.sh"

# use while loop to check if elasticsearch is running 
while true
do
    netstat -uplnt | grep :9300 | grep LISTEN > /dev/null
    verifier=$?
    if [ 0 = $verifier ]
        then
            echo "Running security plugin initialization"
            cd /usr/share/elasticsearch/plugins/opendistro_security/tools

            ./securityadmin.sh -cd ../securityconfig/ -icl -nhnv -cacert ../../../config/root-ca.pem -cert ../../../config/kirk.pem -key ../../../config/kirk.key
            break
        else
            echo "ES is not running yet"
            sleep 5
    fi
done