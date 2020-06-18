#!/bin/bash

if [[ -s .cicd/seed.source ]] && source .cicd/seed.source

#sh -c "$(curl -fsSL https://raw.githubusercontent.com/dotCMS/dot-cicd/master/seed/install-dot-cicd.sh)"
#TODO: restore to previous
sh -c "$(curl -fsSL https://raw.githubusercontent.com/dotCMS/dot-cicd/issue-18504-ci-github-actions/seed/install-dot-cicd.sh)"
