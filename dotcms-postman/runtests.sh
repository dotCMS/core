#!/bin/bash

postman_collections="$1"
dotcms_dir=../dotCMS
postman_tests_dir=${dotcms_dir}/src/curl-test
postman_tests_results_dir=./target/postman-reports

mkdir -p ${postman_tests_results_dir}
old_ifs=$IFS
IFS=,
collections=( ${postman_collections} )
wait=120

echo "Waiting for ${wait} seconds to allow dotCMS to fully start running tests"
echo "Time is up! Preparing to run collections: [${postman_collections//,/$'\n'}]"

echo "Current dir: $(pwd)"
sleep ${wait}

pushd ${postman_tests_dir}
collection_error=false
for collection in "${collections[@]}"
do
  echo "Running collection: [${collection}]"
  collection_name="${collection%.*}"

  echo "Running newman command: newman run ${collection} -e postman_environment.json --reporters cli,junit --reporter-junit-export ${postman_tests_results_dir}/TEST-${collection_name}.xml"
  newman run ${collection} -e postman_environment.json --reporters cli,junit --reporter-junit-export ${postman_tests_results_dir}/TEST-${collection_name}.xml

  pcec=$?
  if [[ $pcec -ne 0 ]]; then
    echo "Collection [${collection}] failed with exit code [${pcec}]"
    collection_error=true
  fi
done
popd

IFS=$old_ifs

if [[ "${collection_error}" == 'true' ]]; then
  echo "One or more collections failed"
  exit 1
fi
