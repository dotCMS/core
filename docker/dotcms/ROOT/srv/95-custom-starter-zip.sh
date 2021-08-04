#!/bin/bash

set -e

CUSTOM_STARTER=custom_starter.zip

## if we have a custom starter
if [ -z ${CUSTOM_STARTER_URL} ]; then
	echo "Using default starter"; 
else 
	if [[ ! -f /data/shared/$CUSTOM_STARTER ]]; then
		touch /data/shared/$CUSTOM_STARTER
		echo "Downloading Custom Starter:" $CUSTOM_STARTER_URL
		mkdir -p  /data/shared
		curl -s -L -o /data/shared/$CUSTOM_STARTER $CUSTOM_STARTER_URL
		if [[ -s /data/shared/$CUSTOM_STARTER ]] ; then
			cp -af /data/shared/$CUSTOM_STARTER ${TOMCAT_HOME}/webapps/ROOT/starter.zip
		else
			rm /data/shared/$CUSTOM_STARTER
			echo "No starter downloaded, skipping"
		fi
	else
		echo "custom starter already downloaded"
		echo "if you need to redownload a new starter, delete the existing custom starter file found here: /data/shared/$CUSTOM_STARTER" 
	fi
fi