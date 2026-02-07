#!/bin/bash

set -e


### URL to download the custom starter from
CUSTOM_STARTER_URL=${CUSTOM_STARTER_URL:-""}

### dotCMS API Token
CUSTOM_STARTER_URL_AUTH_TOKEN=${CUSTOM_STARTER_URL_AUTH_TOKEN:-""}

### Or basic auth - in the form of username:password
CUSTOM_STARTER_URL_BASIC_AUTH=${CUSTOM_STARTER_URL_BASIC_AUTH:-""}

### Folder where the custom starter will be downloaded
CUSTOM_STARTER_DATA_FOLDER=${CUSTOM_STARTER_DATA_FOLDER:-"/data/shared"}


## if we dont have a custom starter
if [ -z ${CUSTOM_STARTER_URL} ]; then
	echo "Using default starter";
else

  HASHED_URL=$(echo -n "$CUSTOM_STARTER_URL" | md5sum | cut -d ' ' -f 1)
  CUSTOM_STARTER=dotcms-starter-$HASHED_URL.zip

	if [[ ! -f $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER ]]; then

  echo "CUSTOM_STARTER_DATA_FOLDER: $CUSTOM_STARTER_DATA_FOLDER"
  echo "CUSTOM_STARTER_URL: $CUSTOM_STARTER_URL"
  echo "HASHED_URL: $HASHED_URL"
  echo "CUSTOM_STARTER file: $CUSTOM_STARTER"
  if [[ -n  $CUSTOM_STARTER_URL_AUTH_TOKEN ]]; then
    echo "CUSTOM_STARTER_URL_AUTH_TOKEN: XXXXXX"
  fi
  if [[ -n  $CUSTOM_STARTER_URL_AUTH_TOKEN ]]; then
    echo "CUSTOM_STARTER_URL_BASIC_AUTH: XXXXXX:XXXXXX"
  fi
	  mkdir -p  $CUSTOM_STARTER_DATA_FOLDER
    if [[ -n  $CUSTOM_STARTER_URL_AUTH_TOKEN ]]; then
      echo "Downloading Custom Starter with Auth Token:" $CUSTOM_STARTER_URL
      echo "curl -s -L -o $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER -HAuthorization: Bearer $CUSTOM_STARTER_URL_AUTH_TOKEN $CUSTOM_STARTER_URL"
      curl -k -s -L -o $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER -H"Authorization: Bearer $CUSTOM_STARTER_URL_AUTH_TOKEN" "$CUSTOM_STARTER_URL" || echo "Failed to download starter with auth token"
    elif [[ -n $CUSTOM_STARTER_URL_BASIC_AUTH ]]; then
      echo "Downloading Custom Starter with Basic Auth:" $CUSTOM_STARTER_URL
      curl -k -s -L -o $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER -u"$CUSTOM_STARTER_URL_BASIC_AUTH" "$CUSTOM_STARTER_URL" || echo "Failed to download starter with basic auth"
    else
      echo "Downloading Custom Starter:" $CUSTOM_STARTER_URL
      curl -k -s -L -o $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER "$CUSTOM_STARTER_URL" || echo "Failed to download starter"
    fi

		if [[ -s $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER ]] ; then
			export DOT_STARTER_DATA_LOAD=$CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER
		else
			rm -f $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER
			echo "No starter downloaded, skipping"
		fi
	else
		echo "custom starter already downloaded"
		echo "if you need to redownload a new starter, delete the existing custom starter file found here: $CUSTOM_STARTER_DATA_FOLDER/$CUSTOM_STARTER"
	fi
fi
