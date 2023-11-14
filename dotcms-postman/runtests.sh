#!/bin/bash

wait=60
echo "Waiting for ${wait} seconds to images be pulled"
sleep $wait
docker-compose logs -t -f dotcms-test &

postman_collections="$1"
dotcms_dir=../dotCMS
postman_tests_dir=${dotcms_dir}/src/curl-test
postman_tests_results_dir=$(pwd)/target/postman-reports

mkdir -p ${postman_tests_results_dir}
echo "
Current dir: $(pwd)
Postman tests dir: ${postman_tests_dir}
Postman tests results dir: ${postman_tests_results_dir}
"

old_ifs=$IFS
IFS=,
collections=( ${postman_collections} )

echo "Waiting for ${wait} seconds to allow dotCMS to fully start running tests"
sleep ${wait}
echo "Time is up! Preparing to run collections: [
${postman_collections//,/$'\n'}
]"

pushd ${postman_tests_dir}
collection_error=false
for collection in "${collections[@]}"
do
  echo "Running collection: [${collection}]"
  collection_name="${collection%.*}"

  echo "Running newman command: newman run ${collection} -e postman_environment.json --reporters cli,junit --reporter-junit-export ${postman_tests_results_dir}/TEST-${collection_name}.xml"
  newman run ${collection} -e postman_environment.json --reporters cli,junit --reporter-junit-export ${postman_tests_results_dir}/TEST-${collection_name}.xml

  collection_exit_code=$?
  if [[ $collection_exit_code -ne 0 ]]; then
    echo "Collection [${collection}] failed with exit code [${collection_exit_code}]"
    collection_error=true
  fi
done
popd

IFS=$old_ifs

if [[ "${collection_error}" == 'true' ]]; then
  echo "One or more collections failed"
  exit 1
fi
