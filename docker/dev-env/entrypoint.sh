#!/bin/bash -e

ASSETS_BACKUP_FILE=/data/assets.zip
DB_BACKUP_FILE=/data/dotcms_db.sql.gz
STARTER_ZIP=/data/starter.zip
export JAVA_HOME=/java
export ES_JAVA_OPTS=${ES_JAVA_OPTS:-"-Xmx512m"}
export DOTCMS_CLONE_TYPE=${DOTCMS_CLONE_TYPE:-"dump"}
export ALL_ASSETS=${ALL_ASSETS:-"false"}
export MAX_ASSET_SIZE=${MAX_ASSET_SIZE:-"100mb"}
export PG_VERSION=${PG_VERSION:-"18"}
export PATH=$PATH:$JAVA_HOME/bin:/usr/local/pgsql/bin:/usr/lib/postgresql/$PG_VERSION/bin/



setup_postgres () {
    echo "Starting Postgres Database"
    if [ ! -d "/data/postgres" ]; then
        mv /var/lib/postgresql/$PG_VERSION/main /data/postgres
    fi
    rm -rf /var/lib/postgresql/$PG_VERSION/main
    ln -sf  /data/postgres /var/lib/postgresql/$PG_VERSION/main


    /etc/init.d/postgresql start

    if su -c "psql -lqt" postgres | cut -d \| -f 1 | grep -qw dotcms; then
        # database exists
        echo "- dotCMS db exists, skipping import"
        echo "- Delete the /data/postgres folder to force a re-import"
        return 0
    fi

    # creating database
    su -c "psql -c \"CREATE database dotcms;\" 1> /dev/null" postgres
    su -c "psql -c \"CREATE USER dotcmsdbuser WITH PASSWORD 'password';\" 1> /dev/null" postgres
    su -c "psql -c \"ALTER DATABASE dotcms OWNER TO dotcmsdbuser;\" 1> /dev/null" postgres
    su -c "psql -c \"CREATE EXTENSION if not exists vector;\" dotcms 1> /dev/null" postgres

    if [  -f "$DB_BACKUP_FILE" ]; then
      echo "- Importing dotCMS db from backup"
      # import database
      cat $DB_BACKUP_FILE | gzip -d | PGPASSWORD=password psql -h 127.0.0.1 -Udotcmsdbuser dotcms
    fi

    return 0
}


setup_opensearch () {


    if [ ! -d "/data/opensearch" ]; then
        mv /usr/share/opensearch/data /data/opensearch
        chown dotcms.dotcms /data/opensearch
    fi

    rm -rf /usr/share/opensearch/data
    ln -sf  /data/opensearch /usr/share/opensearch/data
    chown dotcms.dotcms /data/opensearch

    echo "Starting OPENSEARCH"
    # Start up Elasticsearch
    #su -c "/usr/share/opensearch/bin/opensearch 1> /dev/null" dotcms &
    su -c "OPENSEARCH_JAVA_HOME=/usr /usr/share/opensearch/bin/opensearch " dotcms &
}


pull_dotcms_starter_zip () {
  echo "- Pulling starter.zip file"
    if [ ! -f "$STARTER_ZIP" ]; then
        su -c "rm -rf $STARTER_ZIP.tmp"
        echo "- Downloading Starter"
        su -c "wget --no-check-certificate --header=\"$AUTH_HEADER\" -t 1 -O $STARTER_ZIP.tmp  $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadStarterWithAssets\?oldAssets=$ALL_ASSETS\&maxSize=$MAX_ASSET_SIZE" dotcms
        if [ -s $STARTER_ZIP.tmp ]; then
          su -c "mv $STARTER_ZIP.tmp $STARTER_ZIP"
          export DOT_STARTER_DATA_LOAD=$STARTER_ZIP
        else
          su -c "rm -rf $STARTER_ZIP.tmp"
          echo "starter download failed, please check your credentials and try again"
          exit 1
        fi
    else
      echo "- $STARTER_ZIP exists.  Not re-downloading. Delete the starter.zip file if you would like to download a fresh starter"
    fi
    export DOT_STARTER_DATA_LOAD=$STARTER_ZIP

}

download_starter_url () {
  echo "- Downloading starter from $DOTCMS_STARTER_URL"
    if [ ! -f "$STARTER_ZIP" ]; then
        su -c "rm -rf $STARTER_ZIP.tmp"
        echo "- Downloading Starter"
        su -c "wget --no-check-certificate -t 1 -O $STARTER_ZIP.tmp $DOTCMS_STARTER_URL" dotcms
        if [ -s $STARTER_ZIP.tmp ]; then
          su -c "mv $STARTER_ZIP.tmp $STARTER_ZIP"
          export DOT_STARTER_DATA_LOAD=$STARTER_ZIP
        else
          su -c "rm -rf $STARTER_ZIP.tmp"
          echo "starter download failed, please check your credentials and try again"
          exit 1
        fi
    else
      echo "- $STARTER_ZIP exists.  Not re-downloading. Delete the starter.zip file if you would like to download a fresh starter"
    fi
    export DOT_STARTER_DATA_LOAD=$STARTER_ZIP
}

pull_dotcms_backups () {


    # If these are 0 length files, delete them
    if [ ! -s $ASSETS_BACKUP_FILE ] ; then
        rm -rf $ASSETS_BACKUP_FILE
    fi

    if [ ! -s $DB_BACKUP_FILE ] ; then
        rm -rf $DB_BACKUP_FILE
    fi

    if [ ! -s $STARTER_ZIP ] ; then
        rm -rf $STARTER_ZIP
    fi

    if  [ -n "$DOTCMS_STARTER_URL" ] ; then
      download_starter_url
      return 0
    fi

    if [ -z "$DOTCMS_SOURCE_ENVIRONMENT" ]; then
        echo "- No dotCMS env to clone, starting normally"
        return 0
    fi
    if [ -z "$DOTCMS_API_TOKEN" -a -z "$DOTCMS_USERNAME_PASSWORD" ]; then
        echo "- Source environment specified, but no dotCMS auth available"
        return 1
    fi

    echo "Pulling Environment from $DOTCMS_SOURCE_ENVIRONMENT"

    if [ -n  "$DOTCMS_API_TOKEN"  ]; then
        echo "- Using Authorization: Bearer"
        export AUTH_HEADER="Authorization: Bearer $DOTCMS_API_TOKEN"
    elif [ -n  "$DOTCMS_USERNAME_PASSWORD"  ]; then
        echo "- Using Authorization: Basic"
        export AUTH_HEADER="Authorization: Basic $(echo -n $DOTCMS_USERNAME_PASSWORD | base64)"
    fi

    mkdir -p /data/shared/assets
    chown -R dotcms.dotcms /data/shared

    if  [ "$DOTCMS_CLONE_TYPE" == "starter" ] || [ "$DOTCMS_CLONE_TYPE" == "starter.zip" ] ; then
      pull_dotcms_starter_zip
      return 0
    fi





    if [ -f "$ASSETS_BACKUP_FILE" ] && [ -f $DB_BACKUP_FILE ]; then

        echo "- DB and Assets backups exist, skipping"
        echo "- Delete $ASSETS_BACKUP_FILE and $DB_BACKUP_FILE to force a re-download"
        return 0
    fi




    if [ ! -f "$ASSETS_BACKUP_FILE" ]; then
        su -c "rm -rf $ASSETS_BACKUP_FILE.tmp"
        echo "- Downloading ASSETS"
        su -c "wget --no-check-certificate --header=\"$AUTH_HEADER\" -t 1 -O $ASSETS_BACKUP_FILE.tmp  $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets\?oldAssets=$ALL_ASSETS\&maxSize=$MAX_ASSET_SIZE" dotcms
        if [ -s $ASSETS_BACKUP_FILE.tmp ]; then
          su -c "mv $ASSETS_BACKUP_FILE.tmp $ASSETS_BACKUP_FILE"
        else
          su -c "rm -rf $ASSETS_BACKUP_FILE.tmp"
          echo "asset download failed, please check your credentials and try again"
          exit 1
        fi
    fi

    if [ ! -f "$DB_BACKUP_FILE" ]; then
        echo "- Downloading database"
        su -c "rm -rf $DB_BACKUP_FILE.tmp"
        su -c "wget --no-check-certificate --header=\"$AUTH_HEADER\" -t 1 -O $DB_BACKUP_FILE.tmp $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb" dotcms
        if [ -s $DB_BACKUP_FILE.tmp ]; then
          su -c "mv $DB_BACKUP_FILE.tmp $DB_BACKUP_FILE"
        else
          su -c "rm -rf $DB_BACKUP_FILE.tmp"
          echo "database download failed, please check your credentials and try again"
          exit 1
        fi
    fi

}

## Unpacks the assets.zip file if it exists and has not been unpacked
unpack_assets(){
  if [ -d "/data/shared/assets/1" ]; then
    echo "Assets Already Unpacked, skipping.  If you would like to unpack them again, please delete the /data/shared/assets folder"
    return 0
  fi
  if [ ! -s "$ASSETS_BACKUP_FILE" ]; then
    return 0
  fi


  echo "Unzipping assets.zip"
  su -c "unzip -u $ASSETS_BACKUP_FILE -d /data/shared" || true
}


## Starts dotCMS
start_dotcms () {


    if [ "$DOTCMS_DEBUG" == "true" ];then
      echo "Setting java debug port to 8000. If you want to change the debug options,"
      echo "pass in your options using the CMS_JAVA_OPTS variable instead"
      export CMS_JAVA_OPTS="$CMS_JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:8000"
    fi

    export DB_BASE_URL=${DB_BASE_URL:-"jdbc:postgresql://127.0.0.1/dotcms"}
    export DOT_ES_ENDPOINTS=${DOT_ES_ENDPOINTS:-"https://127.0.0.1:9200"}
    export DOT_DOTCMS_CLUSTER_ID=${DOT_DOTCMS_CLUSTER_ID:-"dotcms_dev"}

    echo "Starting dotCMS using"
    echo " - CMS_JAVA_OPTS: $CMS_JAVA_OPTS"
    echo " - ES_JAVA_OPTS: $ES_JAVA_OPTS"
    echo " - DB_BASE_URL: $DB_BASE_URL"
    echo " - DOT_ES_ENDPOINTS: $DOT_ES_ENDPOINTS"
    echo " - DOT_DOTCMS_CLUSTER_ID: $DOT_DOTCMS_CLUSTER_ID"
    echo " - DOT_STARTER_DATA_LOAD: $DOT_STARTER_DATA_LOAD"
    . /srv/entrypoint.sh dotcms
}



pull_dotcms_backups && echo ""
setup_postgres && echo ""
unpack_assets && echo ""
setup_opensearch && echo ""
start_dotcms
