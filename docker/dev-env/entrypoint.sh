#!/bin/sh -e


setup_postgres () {
    if [ ! -d "/data/postgres" ]; then
        # Start up Postgres
        mv /var/lib/postgresql/14/main /data/postgres
    fi
    rm -rf /var/lib/postgresql/14/main
    ln -sf  /data/postgres /var/lib/postgresql/14/main
    /etc/init.d/postgresql start

    if su -c "psql -lqt" postgres | cut -d \| -f 1 | grep -qw dotcms; then
        # database exists
        echo "dotcms db exists"
    else
        # creating database
        su -c "psql -c \"CREATE database dotcms;\"" postgres
        su -c "psql -c \"CREATE USER dotcmsdbuser WITH PASSWORD 'password';\"" postgres
        su -c "psql -c \"ALTER DATABASE dotcms OWNER TO dotcmsdbuser;\"" postgres
    fi


    # import database
    cat $DB_FILE | gzip -d | PGPASSWORD=password psql -h 127.0.0.1 -Udotcmsdbuser dotcms


}

setup_opensearch () {
    if [ ! -d "/data/opensearch" ]; then
        # set up opensearch
        mv /usr/share/opensearch/data /data/opensearch
        chown dotcms.dotcms /data/opensearch
    fi

    rm -rf /usr/share/opensearch/data
    ln -sf  /data/opensearch /usr/share/opensearch/data


    echo ""
    export JAVA_HOME=/usr/share/opensearch/jdk
    # Start up Elasticsearch
    su -c "ES_JAVA_OPTS=-Xmx1G /usr/share/opensearch/bin/opensearch" dotcms &
}


pull_dotcms_backups () {
    
    if [ -z "$DOTCMS_SOURCE_ENVIRONMENT" ]; then
        echo "No dotCMS env to clone"
        return 0
    fi
    if [ -z "$DOTCMS_API_TOKEN" -a -z "$DOTCMS_USERNAME_PASSWORD" ]; then
        echo "No dotCMS auth available"
        return 0
    fi

    if [ -n  "$DOTCMS_API_TOKEN"  ]; then
        echo "Using Auth: Token"
        AUTH_HEADER="Authorization: Bearer $DOTCMS_API_TOKEN"
    else
        echo "Using Auth: Basic"
        AUTH_HEADER="Authorization: Basic $(echo -n $DOTCMS_USERNAME_PASSWORD | base64)" 
    fi

    # GET Backup
    ASSETS_FILE=/data/shared/assets.zip
    DB_FILE=/data/shared/dotcms_db.sql.gz
    #rm -f $ASSETS_FILE $DB_FILE

    echo "Using Auth Header: $AUTH_HEADER"

    echo "Pulling Environment from $DOTCMS_SOURCE_ENVIRONMENT"
    mkdir -p /data/shared/assets
    chown -R dotcms.dotcms /data/shared

    if [ -f "$ASSETS_FILE" ]; then
        echo "- $ASSETS_FILE exists, skipping"
    else 
        echo "Downloading ASSETS"
        #su -c "curl --http1.1 --keepalive-time 2 -k -H\"$AUTH_HEADER\" $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets | bsdtar -xvf- -C /data/shared/" dotcms
        #su -c "wget --header=\"$AUTH_HEADER\" -t 1 -O - $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets | bsdtar -xvkf- -C /data/shared/" dotcms
        su -c "wget --header=\"$AUTH_HEADER\" -t 1 -O $ASSETS_FILE  $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets " dotcms
    fi

    if [ -f "$DB_FILE" ]; then
        echo "- $DB_FILE exists, skipping"
    else
        echo "Downloading database"

        #su -c "wget --header=\"$AUTH_HEADER\" -t 1 -O - $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb | gzip -d | PGPASSWORD=password psql -h 127.0.0.1 -Udotcmsdbuser dotcms" dotcms
        su -c "wget --header=\"$AUTH_HEADER\" -t 1 -O $DB_FILE $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb"

        echo "Downloaded the Files"
    fi

}







start_dotcms () {
    export DB_BASE_URL=${DB_BASE_URL:-"jdbc:postgresql://127.0.0.1/dotcms"}
    export DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:-"https://127.0.0.1:9200"}
    /srv/entrypoint.sh
}

pull_dotcms_backups 

setup_postgres

setup_opensearch

start_dotcms





