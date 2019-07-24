#!/bin/bash

dockerFolder="/home/travis/build/dotCMS/core/docker/tests/integration"
postgresCompose="${dockerFolder}/postgres-docker-compose.yml"
mysqlCompose="${dockerFolder}/mysql-docker-compose.yml"

# postgres-docker-compose.yml
sed -e "30s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "31s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "32s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "33s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "34s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "35s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}
sed -e "36s/.*/#&/" ${postgresCompose} >tmpfile && mv tmpfile ${postgresCompose}

# mysql-docker-compose.yml
sed -e "30s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "31s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "32s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "33s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "34s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "35s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}
sed -e "36s/.*/#&/" ${mysqlCompose} >tmpfile && mv tmpfile ${mysqlCompose}