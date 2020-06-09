#!/bin/bash

source .cicd/seed.source

script=$(curl -fsSL https://raw.githubusercontent.com/dotCMS/dot-cicd/master/seed/install-dot-cicd.sh)
echo "${script}"

sh -c "${script}"
