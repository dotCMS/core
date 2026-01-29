#!/bin/bash -e



cat > app.env << 'EOF'
#SHARED_DATA_DIR=/Users/will/git/dotcms/data
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

docker run --env-file app.env --rm -p8080:8082 -p8433:8433 dotcms/dotcms-test:1.0.0-SNAPSHOT

