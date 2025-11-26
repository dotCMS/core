#!/bin/bash

# Prints docker-compose.yml snippets to fetch the proper
# Demo Site content for each dotCMS version

# This clones the dotCMS core repo on its first run which will take some time

gitdir=/var/tmp/dotcms-get-starter-urls-repo
if [ ! -d $gitdir ]
then
    echo "Cloning dotcms core repo to $gitdir"
    git clone https://github.com/dotCMS/core.git $gitdir
fi

pushd  $gitdir >/dev/null
git checkout -q main
git pull -q
for version in $(git tag -l v* | sed 's/^v//' | sort -n | uniq | grep -v MM.YY)
do
    echo
    git checkout -q v${version} 
    starter_date=$(grep 'starter group' dotCMS/build.gradle | grep -v empty_ | awk -F \' '{print $6}')
    cat <<EOF
  dotcms:
    image: dotcms/dotcms:${version}
    environment:
      CUSTOM_STARTER_URL: https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/${starter_date}/starter-${starter_date}.zip
EOF
done
git checkout -q main
echo
pushd >/dev/null
