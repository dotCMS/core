#!/bin/bash

export DOT_CICD_TARGET=core

sh -c "$(curl -fsSL https://raw.githubusercontent.com/dotCMS/dot-cicd/master/seed/install-dot-cicd.sh)"
