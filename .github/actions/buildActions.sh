#!/bin/bash

set -e

mode=${1}

echo "Node version is: $(node -v)"
for d in $(ls) ; do
  [[ ! -d ${d} || ! -f ${d}/package.json || -z "$(cat ${d}/action.yml | grep "using: 'node*")" ]] && continue
  echo "Building Github Action ${d}"
  pushd ${d}
  if [[ "${mode}" == 'clean' ]]; then
    [[ -d ./node_modules ]] && rm -rf ./node_modules
    [[ -f package-lock.json ]] && rm package-lock.json
    npm install
  fi
  npm run all
  popd
done