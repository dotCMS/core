#!/bin/bash

echo $GOOGLE_CREDENTIALS_BASE64 | base64 -d - > credentials.json