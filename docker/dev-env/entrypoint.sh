#!/bin/sh -e

ASSETS_BACKUP_FILE=/data/shared/assets.zip
DB_BACKUP_FILE=/data/shared/dotcms_db.sql.gz

setup_postgres () {
    echo "starting up postgres"
    if [ ! -d "/data/postgres" ]; then
        mv /var/lib/postgresql/14/main /data/postgres
    fi

    rm -rf /var/lib/postgresql/14/main
    ln -sf  /data/postgres /var/lib/postgresql/14/main

    /etc/init.d/postgresql start

    if su -c "psql -lqt" postgres | cut -d \| -f 1 | grep -qw dotcms; then
        # database exists
        echo "dotcms db exists, skipping import"
        echo "delete the /data/postgres folder to force a re-import"
        return 0
    fi

    # creating database
    su -c "psql -c \"CREATE database dotcms;\"" postgres
    su -c "psql -c \"CREATE USER dotcmsdbuser WITH PASSWORD 'password';\"" postgres
    su -c "psql -c \"ALTER DATABASE dotcms OWNER TO dotcmsdbuser;\"" postgres

    # import database
    cat $DB_BACKUP_FILE | gzip -d | PGPASSWORD=password psql -h 127.0.0.1 -Udotcmsdbuser dotcms
    return 0
}


setup_opensearch () {


    if [ ! -d "/data/opensearch" ]; then
        mv /usr/share/opensearch/data /data/opensearch
        chown dotcms.dotcms /data/opensearch
    fi

    rm -rf /usr/share/opensearch/data
    ln -sf  /data/opensearch /usr/share/opensearch/data


    echo "Starting OPENSEARCH"
    export JAVA_HOME=/usr/share/opensearch/jdk
    # Start up Elasticsearch
    su -c "ES_JAVA_OPTS=-Xmx1G /usr/share/opensearch/bin/opensearch" dotcms &
}


pull_dotcms_backups () {

    if [ -f "$ASSETS_BACKUP_FILE" ] && [ -f $DB_BACKUP_FILE ]; then
        echo "- DB and Assets backups exist, skipping"
        echo "- Delete $ASSETS_BACKUP_FILE and $DB_BACKUP_FILE to force a re-download"
        return 0
    fi




    if [ -z "$DOTCMS_SOURCE_ENVIRONMENT" ]; then
        echo "No dotCMS env to clone"
        return 1
    fi
    if [ -z "$DOTCMS_API_TOKEN" -a -z "$DOTCMS_USERNAME_PASSWORD" ]; then
        echo "No dotCMS auth available"
        return 1
    fi

    if [ -n  "$DOTCMS_API_TOKEN"  ]; then
        echo "Using Authorization: Bearer"
        AUTH_HEADER="Authorization: Bearer $DOTCMS_API_TOKEN"
    else
        echo "Using Authorization: Basic"
        AUTH_HEADER="Authorization: Basic $(echo -n $DOTCMS_USERNAME_PASSWORD | base64)" 
    fi

    # GET Backup

    #rm -f $ASSETS_BACKUP_FILE $DB_BACKUP_FILE

    # echo "Using Auth Header: $AUTH_HEADER"

    echo "Pulling Environment from $DOTCMS_SOURCE_ENVIRONMENT"
    mkdir -p /data/shared/assets
    chown -R dotcms.dotcms /data/shared

    if [ ! -f "$ASSETS_BACKUP_FILE" ]; then
        echo "Downloading ASSETS"
        #su -c "curl --http1.1 --keepalive-time 2 -k -H\"$AUTH_HEADER\" $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets | bsdtar -xvf- -C /data/shared/" dotcms
        #su -c "wget --header=\"$AUTH_HEADER\" -t 1 -O - $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets | bsdtar -xvkf- -C /data/shared/" dotcms
        su -c "wget --no-check-certificate --header=\"$AUTH_HEADER\" -t 1 -O $ASSETS_BACKUP_FILE  $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets " dotcms
    fi

    if [ ! -f "$DB_BACKUP_FILE" ]; then
        echo "Downloading database"

        #su -c "wget --header=\"$AUTH_HEADER\" -t 1 -O - $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb | gzip -d | PGPASSWORD=password psql -h 127.0.0.1 -Udotcmsdbuser dotcms" dotcms
        su -c "wget --no-check-certificate --header=\"$AUTH_HEADER\" -t 1 -O $DB_BACKUP_FILE $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb"

    fi

}

unpack_assets(){
  if [ -d "/data/shared/assets/1" ]; then
    echo "Assets Already Unpacked, skipping"
    return 0
  fi
  echo "UNZIPPING assets.zip"
  su -c "unzip -u /data/shared/assets.zip -d /data/shared"
}

# shellcheck disable=SC1072

start_dotcms () {
    export DB_BASE_URL=${DB_BASE_URL:-"jdbc:postgresql://127.0.0.1/dotcms"}
    export DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:-"https://127.0.0.1:9200"}
    /srv/entrypoint.sh
}


pull_dotcms_backups
echo ""
setup_postgres
echo ""
unpack_assets
echo ""
setup_opensearch
echo ""
start_dotcms
