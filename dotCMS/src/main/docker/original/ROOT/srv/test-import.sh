#!/bin/bash -e

# This script can be used to test the importing functionality of dotCMS.  
# Use the environmental variables below to spin up a dotcms docker image 
# and clone different environments. If you are running locally, run a 
#
# `just build-quicker` 
#
# from the project root to quickly build a testable docker image


cat > app.env << 'EOF'
DOT_IMPORT_ENVIRONMENT=https://demo.dotcms.com
DOT_IMPORT_API_TOKEN=
DOT_IMPORT_USERNAME_PASSWORD=admin@dotcms.com:admin
DOT_IMPORT_DROP_DB=true


DB_BASE_URL=jdbc:postgresql://db.dotcms.site/dotcms
DB_DRIVER=org.postgresql.Driver
DB_PASSWORD=password
DB_USERNAME=dotcmsdbuser
DOT_ENABLE_SCRIPTING=true
DOT_ES_AUTH_BASIC_PASSWORD=admin
DOT_ES_AUTH_BASIC_USER=admin
DOT_ES_AUTH_TYPE=BASIC
DOT_ES_ENDPOINTS=https://es.dotcms.site:9200

EOF

docker run --env-file app.env -v $PWD/data:/data --rm -p8080:8082 -p8443:8443 dotcms/dotcms-test:1.0.0-SNAPSHOT

