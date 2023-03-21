#!/bin/sh -l
#Script will delete all images in all repositories of your docker hub account which are older than 50 days

set -e


ORG="dotcms"
REPO="dotcms"
#DELETE_OLDER_THAN_DAYS=50
#PATTERN_TO_MATCH="SNAPSHOT|prerelease|debug|test|master_"
echo "DELETE_OLDER_THAN_DAYS: $DELETE_OLDER_THAN_DAYS"
echo "PATTERN_TO_MATCH: $PATTERN_TO_MATCH"



## So this works on OSX
function gnudate() {
    if hash gdate 2>/dev/null; then
        gdate "$@"
    else
        date "$@"
    fi
}


function deleteImages(){
   # build a list of images from tags
   for j in ${IMAGE_TAGS}
   do
      
      if [[ $j =~ $PATTERN_TO_MATCH ]]; then
          # add last_updated_time
        updated_time=$(curl -s -H "Authorization: JWT ${TOKEN}" https://hub.docker.com/v2/repositories/$ORG/$REPO/tags/${j}/?page_size=100 | jq -r '.last_updated')
        #echo "$j updated $updated_time"
        datetime=$updated_time
        timeago="$DELETE_OLDER_THAN_DAYS days ago"

        dtSec=$(gnudate --date "$datetime" +'%s')
        taSec=$(gnudate --date "$timeago" +'%s')

        #echo "INFO: dtSec=$dtSec, taSec=$taSec"

        if [ $dtSec -lt $taSec ]; then
          echo "${ORG}/${REPO}:${j} > $DELETE_OLDER_THAN_DAYS days, deleting "
          ## Please uncomment below line to delete docker hub images of docker hub repositories
          curl -s  -X DELETE  -H "Authorization: JWT ${TOKEN}" https://hub.docker.com/v2/repositories/${ORG}/${REPO}/tags/${j}/
        fi
      fi
   done
}




# get token to be able to talk to Docker Hub
TOKEN=$(curl -s -H "Content-Type: application/json" -X POST -d '{"username": "'${UNAME}'", "password": "'${UPASS}'"}' https://hub.docker.com/v2/users/login/ | jq -r .token)


echo "Identifying and deleting images which are older than $DELETE_OLDER_THAN_DAYS days in ${ORG} docker hub account"

# get tags for repo
echo
echo "Looping Through ${REPO} repository in ${ORG} account"


RAW_DATA=$(curl -s -H "Authorization: JWT ${TOKEN}" "https://hub.docker.com/v2/repositories/dotcms/dotcms/tags?page=1&page_size=100" )
TOTAL_COUNT=$(echo $RAW_DATA | jq -r '.count')
NUM_PAGES=$(expr $TOTAL_COUNT / 100)
NUM_PAGES=$(expr $NUM_PAGES + 1)

echo TOTAL_COUNT: $TOTAL_COUNT
echo NUM_PAGES: $NUM_PAGES
echo DELETING: "$PATTERN_TO_MATCH"
i=1
while [[ $i -le $NUM_PAGES ]]
do
   echo "PAGE: $i"
   IMAGE_TAGS=$(curl -s -H "Authorization: JWT ${TOKEN}" "https://hub.docker.com/v2/repositories/$ORG/$REPO/tags?page=${i}&page_size=100" | jq -r '.results|.[]|.name')
   deleteImages
   ((i = i + 1))
done






echo "Script execution ends"