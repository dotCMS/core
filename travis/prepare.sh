#!/bin/bash

RELATIVE_MY_PATH="`dirname \"$0\"`"              # relative
MY_PATH="`( cd \"$RELATIVE_MY_PATH\" && pwd )`"  # absolutized and normalized
if [ -z "$MY_PATH" ] ; then
  # error; for some reason, the path is not accessible
  # to the script (e.g. permissions re-evaled after suid)
  exit 1  # fail
fi

echo ""
echo "***************************"
echo "***************************"
echo "Running from: ${MY_PATH}"
echo "Running from ${RELATIVE_MY_PATH}"
echo "***************************"
echo "***************************"
echo ""

dockerFolder="/home/travis/build/dotCMS/core/docker/tests/integration"

echo "Elements in /home/travis/build/dotCMS/core"
for entry in "/home/travis/build/dotCMS/core/"/*
do
  echo "$entry"
done

echo "Elements in /home/travis/build/dotCMS/core/docker"
for entry in "/home/travis/build/dotCMS/core/docker/"/*
do
  echo "$entry"
done

echo "Elements in /home/travis/build/dotCMS/core/docker/tests"
for entry in "/home/travis/build/dotCMS/core/docker/tests/"/*
do
  echo "$entry"
done

echo "Elements in /home/travis/build/dotCMS/core/docker/tests/integration"
for entry in "/home/travis/build/dotCMS/core/docker/tests/integration/"/*
do
  echo "$entry"
done

# postgres-docker-compose.yml
sed -e '30s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml
sed -e '31s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml
sed -e '32s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml
sed -e '33s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml
sed -e '34s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml
sed -e '35s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml
sed -e '36s/.*/#&/' -i '' ${dockerFolder}/postgres-docker-compose.yml

# mysql-docker-compose.yml
sed -e '30s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml
sed -e '31s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml
sed -e '32s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml
sed -e '33s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml
sed -e '34s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml
sed -e '35s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml
sed -e '36s/.*/#&/' -i '' ${dockerFolder}/mysql-docker-compose.yml