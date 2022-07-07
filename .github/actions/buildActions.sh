#!/bin/bash

set -e

mode=${1}

for d in $(ls) ; do
  [[ ! -d ${d} || ! -f ${d}/package.json || -z "$(cat ${d}/action.yml | grep "using: 'node*")" ]] && continue
  echo "Building Github Action ${d}"
  pushd ${d}
  if [[ "${mode}" == 'clean' ]]; then
    [[ -d ./node_modules ]] && rm -rf ./node_modules
    [[ -f package-lock.json ]] && rm package-lock.json
    npm install
  elif [[ "${mode}" == 'install' ]]; then
    npm install
  fi
  npm run all
  popd
done