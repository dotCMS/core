#!/bin/bash

dockerFolder="../docker/tests/integration"

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