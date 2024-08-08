#!/bin/bash

set -e

mode=${1}

function process_directory {
  for d in "$1"/* ; do
    [[ ! -d ${d} ]] && continue
    if [[ -f ${d}/package.json && -n "$(cat ${d}/action.yml | grep "using: 'node*")" ]]; then
      echo "Building Github Action ${d}"
      pushd ${d}
      if [[ "${mode}" == 'clean' ]]; then
        [[ -d ./node_modules ]] && rm -rf ./node_modules
        npm install
      fi
      npm run all
      popd
    else
      process_directory "${d}"
    fi
  done
}

process_directory .