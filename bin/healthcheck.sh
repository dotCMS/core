#!/bin/sh

STATUS_CODE=$(wget --quiet --spider --server-response http://localhost:8080/api/v1/system-status 2>&1 | awk 'NR==1{print $2}')
[ "$STATUS_CODE" == "200" ] || exit 1
