#!/bin/bash

echo ""
echo "========================================================================================================"
echo "Executing... [newman run <collection> -reporters cli,htmlextra --reporter-htmlextra-export] <report file>"
echo "========================================================================================================"
echo ""

# Prepare to run newman for every found postman collection
postmanEnvFile="postman_environment.json"
reportFolder="/tmp/curlTests"
mkdir -p $reportFolder
# Create a map to store collection -> newman result
> ${reportFolder}/resultLinks.html

IFS=$'\n'
for f in $(ls *.json);
do
  if [[ "${f}" == "${postmanEnvFile}" ]]; then
    continue
  fi

  echo "Running newman for collection: \"${f}\""
  collectionName=$(echo "$f"| tr ' ' '_' | cut -f 1 -d '.')
  page="${collectionName}.html"
  resultFile="${reportFolder}/${page}"

  # actual running of postman tests for current collection
  newman run "$f" -e ${postmanEnvFile} --reporters cli,htmlextra --reporter-htmlextra-export $resultFile

  # handle collection results
  if [[ $? == 0 ]]; then
  	resultLabel=PASS
  else
  	resultLabel=FAIL
  fi
  echo "<tr><td><a href=\"./$page\">$f</a></td>
    <td>${resultLabel}</td></tr>" >> ${reportFolder}/resultLinks.html
done

echo "<!DOCTYPE html>
<html>
<head>
    <meta charset=\"UTF-8\">
    <title>Newman Report</title>
</head>
<body>
<div>
    <h2>Newman Report</h2>
    <table>
        <tbody>
        <tr><th>Collection</th><th>Result</th></tr>
        $(cat ${reportFolder}/resultLinks.html)
        </tbody>
    </table>
</div>
</body>
</html>
" > ${reportFolder}/index.html
rm ${reportFolder}/resultLinks.html

open ${reportFolder}/index.html
