#!/bin/bash

echo $GOOGLE_CREDENTIALS_BASE64 | base64 -d - > credentials.json

if [ ! -s "credentials.json" ]
then
   echo ""
   echo "================================================================"
   echo " >>> Valid [credentials.json] NOT FOUND <<<"
   echo "================================================================"
   exit 1
fi