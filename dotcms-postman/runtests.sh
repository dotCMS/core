#!/bin/bash

postman_collections="$1"
dotcms_dir=../dotCMS
postman_tests_dir=${dotcms_dir}/src/curl-test
postman_tests_results_dir=${dotcms_dir}/postman-test-results

mkdir -p ${postman_tests_results_dir}
old_ifs=$IFS
IFS=,
collections=( ${postman_collections} )
wait=120

echo "Waiting for ${wait} seconds to allow dotCMS to fully start"
sleep ${wait}
pushd ${postman_tests_dir}

for collection in "${collections[@]}"
do
  echo "Running collection: ${collection}"
  collection_name="${collection%.*}"

  sed -i 's,{{serverURL}},http://localhost:8080,g' ./${collection}

  cmd="newman run ${collection} --reporters cli,junit --reporter-junit-export ${postman_tests_results_dir}/${collection_name}.xml"
  echo "Running newman command: ${cmd}"
  eval ${cmd}
done

IFS=$old_ifs
popd
