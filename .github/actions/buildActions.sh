#!/bin/bash

set -e

mode=${1}

for d in $(ls) ; do
  [[ ! -d ${d} || ! -f ${d}/package.json || -z "$(cat ${d}/action.yml | grep "using: 'node*")" ]] && continue
  echo "Building Github Action ${d}"
  pushd ${d}
  [[ "${mode}" == 'clean' ]] \
    && rm -rf ./node_modules \
    && npm install
  npm run all
  popd
done