#!/bin/bash

[[ -s .cicd/seed.source ]] && source .cicd/seed.source

sh -c "$(curl -fsSL https://raw.githubusercontent.com/dotCMS/dot-cicd/master/seed/install-dot-cicd.sh)"
