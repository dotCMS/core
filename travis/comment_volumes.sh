#!/bin/bash

dockerFolder="/home/travis/build/dotCMS/core/docker/tests/integration"
postgresCompose="${dockerFolder}/postgres-docker-compose.yml"
mysqlCompose="${dockerFolder}/mysql-docker-compose.yml"

# We need to comment the volumen bind fragments as we won't mount any folder with google builds
# The easiest way is do in it by line number as we don'tar want to remove also the db volumen binds

# postgres-docker-compose.yml
sed -e "27s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "28s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "29s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "30s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}

# mysql-docker-compose.yml
sed -e "27s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "28s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "29s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "30s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}