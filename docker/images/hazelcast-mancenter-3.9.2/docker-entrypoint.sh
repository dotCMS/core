#!/bin/bash

set -e 


# Add mancenter as command if needed
if [ "${1:0:1}" = '-' ]; then
    set -- mancenter "$@"
fi

if [[ "$1" == 'mancenter' ]]; then

    ./startManCenter.sh

else

    exec "$@"

fi



