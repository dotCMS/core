#!/bin/sh -e


request_license () {

  DEV_LICENSE_SRC=/srv/dev_licensepack.zip


  if([ -z "$DEV_REQUEST_TOKEN" ]); then
      echo "No dev license request token provided, skipping license request"
      return 0
  fi


  EXPIRE_DATE=$(date +%Y-%m-%d -d "+365 day")

  curl -H "Content-Type: application/json" -H "Authorization:Bearer $DEV_REQUEST_TOKEN" -XPUT https://license.dotcms.com/api/ext/license -d "
    {
        \"licensePack\": 1,
        \"level\": 500,
        \"licenseType\": \"dev\",
        \"clientName\": \"dotCMS Developer License\",
        \"clientEmail\": \"dev@dotcms.com\",
        \"clientId\": \"dotCMS\",
        \"expireDate\": \"$EXPIRE_DATE\"
    }
  " > $DEV_LICENSE_SRC
  return 0

}

echo "Requesting dev license"

request_license
