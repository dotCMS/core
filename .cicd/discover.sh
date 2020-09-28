#!/bin/bash

set -e

[[ -s .cicd/seed.source ]] && source .cicd/seed.source
: ${DOT_CICD_BRANCH:="master"}

sh -c "$(curl -fsSL https://raw.githubusercontent.com/dotCMS/dot-cicd/${DOT_CICD_BRANCH}/seed/install-dot-cicd.sh)"
