#!/bin/bash

echo '{
"project_id": "'$GOOGLE_PROJECT_ID'",
"private_key_id": "'$GOOGLE_CREDENTIALS_BASE64'"
}' > credentials.json

#echo $GOOGLE_CREDENTIALS_BASE64 | base64 -d - > credentials.json

if [ ! -s "credentials.json" ]
then
   echo ""
   echo "================================================================"
   echo " >>> Valid [credentials.json] NOT FOUND <<<"
   echo "================================================================"
   exit 1
fi

echo "========================"
echo "========================"
echo ""
cat credentials.json
echo ""
echo "========================"
echo "========================"
echo "========================"
echo "========================"
echo ""
echo $PWD
echo ""
echo "========================"
echo "========================"
