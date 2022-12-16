#!/bin/bash

: ${BUILD_ID:="master"} && export BUILD_ID

curl -fsSL https://raw.githubusercontent.com/dotCMS/core/${BUILD_ID}/cicd/local-env.sh | bash
curl -fsSL https://raw.githubusercontent.com/dotCMS/core/${BUILD_ID}/cicd/local-cicd.sh | bash
