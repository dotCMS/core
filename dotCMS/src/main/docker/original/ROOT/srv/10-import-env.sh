#!/bin/bash -e


## Drops the contents of the dotCMS database in preperation for a new import (only if requested)
drop_db_tables () {

    echo "- DOT_IMPORT_DROP_DB - attempting to drop db schema"
      # Extract hostname and database name from JDBC URL (jdbc:postgresql://host/dbname)
      DB_HOST="${DB_BASE_URL#jdbc:postgresql://}"  # Remove prefix -> host/dbname
      DB_HOST="${DB_HOST%%/*}"                      # Remove /dbname -> host
      DB_NAME="${DB_BASE_URL##*/}"                  # Remove everything before last / -> dbname

      # Export password for psql (avoids password prompt)
      export PGPASSWORD="${DB_PASSWORD}"

      psql -h "${DB_HOST}" -d "${DB_NAME}" -U "${DB_USERNAME}" -c "DROP SCHEMA public CASCADE;CREATE SCHEMA public;GRANT ALL ON SCHEMA public TO public;"
      # Clear the password from environment
      unset PGPASSWORD

}



## Imports the dotcms_db.sql.gz file into the postgres database specified by the DB_BASE_URL environment variable.
import_postgres () {

    if [ -s $DB_BACKUP_FILE ]; then

      if [ -z $DB_BASE_URL ]; then
          echo "DB_BASE_URL environment variable not set, cannont continue without importing database"
          return 0
      fi

      # Extract hostname and database name from JDBC URL (jdbc:postgresql://host/dbname)
      DB_HOST="${DB_BASE_URL#jdbc:postgresql://}"  # Remove prefix -> host/dbname
      DB_HOST="${DB_HOST%%/*}"                      # Remove /dbname -> host
      DB_NAME="${DB_BASE_URL##*/}"                  # Remove everything before last / -> dbname

      # Export password for psql (avoids password prompt)
      export PGPASSWORD="${DB_PASSWORD}"

      # Check if database already has data (inode table exists with records)
      INODE_COUNT=$(psql -h "${DB_HOST}" -d "${DB_NAME}" -U "${DB_USERNAME}" -qtAX -c \
        "SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'inode')
         THEN (SELECT count(*) FROM inode) ELSE 0 END" 2>/dev/null | tr -d '[:space:]')

      if [ -n "$INODE_COUNT" ] && [ "$INODE_COUNT" -gt 0 ]; then
          echo "- Database already contains data ($INODE_COUNT inodes), skipping import."
          return 0
      fi


      # Run the query using psql
      cat $DB_BACKUP_FILE | gzip -d | psql -h "${DB_HOST}" -d "${DB_NAME}" -U "${DB_USERNAME}" 

      # Clear the password from environment
      unset PGPASSWORD

    fi

}


## Downloads the assets.zip and dotcms_db.sql.gz files from the specified environment.
download_dotcms_db_assets () {

    # If these are 0 length files, delete them
    if [ ! -s $ASSETS_BACKUP_FILE ] ; then
        rm -rf $ASSETS_BACKUP_FILE
    fi

    if [ ! -s $DB_BACKUP_FILE ] ; then
        rm -rf $DB_BACKUP_FILE
    fi

    echo "- Pulling Environment from $DOT_IMPORT_ENVIRONMENT"

    if [ -n  "$DOT_IMPORT_API_TOKEN"  ]; then
        echo "- Using Authorization: Bearer"
        export AUTH_HEADER="Authorization: Bearer $DOT_IMPORT_API_TOKEN"
    elif [ -n  "$DOT_IMPORT_USERNAME_PASSWORD"  ]; then
        echo "- Using Authorization: Basic"
        export AUTH_HEADER="Authorization: Basic $(echo -n $DOT_IMPORT_USERNAME_PASSWORD | base64)"
    fi


    mkdir -p $SHARED_DATA_DIR/assets
    chown -R dotcms:dotcms $SHARED_DATA_DIR || echo "cannot chown"

    if [ -s "$ASSETS_BACKUP_FILE" ] && [ -s $DB_BACKUP_FILE ]; then

        echo "- DB and Assets backups exist, skipping"
        echo "- Delete $ASSETS_BACKUP_FILE and $DB_BACKUP_FILE to force a re-download"
        return
    fi

    if [ ! -s "$ASSETS_BACKUP_FILE" ]; then
        rm -rf $ASSETS_BACKUP_FILE.tmp
        echo "- Downloading ASSETS"
        echo "AUTH_HEADER: $AUTH_HEADER"
        echo "wget --no-check-certificate --header=\"$AUTH_HEADER\" -t 1 -O ${ASSETS_BACKUP_FILE}.tmp  ${DOT_IMPORT_ENVIRONMENT}/api/v1/maintenance/_downloadAssets\?oldAssets=${DOT_IMPORT_ALL_ASSETS}\&maxSize=${DOT_IMPORT_MAX_ASSET_SIZE}"
        
        wget --no-check-certificate --header="$AUTH_HEADER" -t 1 -O ${ASSETS_BACKUP_FILE}.tmp  ${DOT_IMPORT_ENVIRONMENT}/api/v1/maintenance/_downloadAssets\?oldAssets=${DOT_IMPORT_ALL_ASSETS}\&maxSize=${DOT_IMPORT_MAX_ASSET_SIZE}
        if [ -s ${ASSETS_BACKUP_FILE}.tmp ]; then
          mv ${ASSETS_BACKUP_FILE}.tmp $ASSETS_BACKUP_FILE
        else
          rm -rf ${ASSETS_BACKUP_FILE}.tmp
          echo "asset download failed, please check your credentials and try again"
          exit 1
        fi
    fi

    if [ ! -f "$DB_BACKUP_FILE" ]; then
        echo "- Downloading database"
        rm -rf ${DB_BACKUP_FILE}.tmp
        wget --no-check-certificate --header="$AUTH_HEADER" -t 1 -O ${DB_BACKUP_FILE}.tmp "${DOT_IMPORT_ENVIRONMENT}/api/v1/maintenance/_downloadDb" || exit 1
        if [ -s ${DB_BACKUP_FILE}.tmp ]; then
          mv ${DB_BACKUP_FILE}.tmp $DB_BACKUP_FILE
        else
          rm -rf ${DB_BACKUP_FILE}.tmp
          echo "database download failed, please check your credentials and try again"
          exit 1
        fi
    fi

    unset AUTH_HEADER

}

## Unpacks the assets.zip file if it exists and has not been unpacked
unpack_assets(){
  if [ ! -s "$ASSETS_BACKUP_FILE" ]; then
    return 0
  fi

  echo "- Extracting assets.zip"
  local tar_lang="${DOT_IMPORT_TAR_LANG:-C.UTF-8}"
  local DOT_IMPORT_IGNORE_ASSET_ERRORS=${DOT_IMPORT_IGNORE_ASSET_ERRORS:-"true"}


  if ! LANG="$tar_lang" bsdtar -xf "$ASSETS_BACKUP_FILE" -C "$SHARED_DATA_DIR"; then
    if [ "${DOT_IMPORT_IGNORE_ASSET_ERRORS}" = "true" ]; then
      echo "WARNING: assets extraction reported errors; continuing due to DOT_IMPORT_IGNORE_ASSET_ERRORS=true"
      return 0
    fi
    return 1
  fi
}



#### Script main()

export DOT_IMPORT_DROP_DB=${DOT_IMPORT_DROP_DB:-"false"}
export DOT_IMPORT_NON_LIVE_ASSETS=${DOT_IMPORT_NON_LIVE_ASSETS:-"false"}
export DOT_IMPORT_MAX_ASSET_SIZE=${DOT_IMPORT_MAX_ASSET_SIZE:-"100mb"}
export SHARED_DATA_DIR=${SHARED_DATA_DIR:-"/data/shared"}
export IMPORT_DATA_DIR=${IMPORT_DATA_DIR:-"$SHARED_DATA_DIR/import"}
export IMPORT_IN_PROCESS=$IMPORT_DATA_DIR/lock.txt
export IMPORT_COMPLETE=$IMPORT_DATA_DIR/import_complete.txt

if [ -z "$DOT_IMPORT_ENVIRONMENT" ]; then
    exit 0
fi

if [ -z "$DOT_IMPORT_API_TOKEN" -a -z "$DOT_IMPORT_USERNAME_PASSWORD" ]; then
    echo "- Set DOT_IMPORT_ENVIRONMENT, DOT_IMPORT_USERNAME_PASSWORD and/or DOT_IMPORT_API_TOKEN to import from another environment on first run"
    exit 0
fi

export DOT_IMPORT_HOST="${DOT_IMPORT_ENVIRONMENT#http://}"; DOT_IMPORT_HOST="${DOT_IMPORT_HOST#https://}"; DOT_IMPORT_HOST="${DOT_IMPORT_HOST%%/*}"; DOT_IMPORT_HOST="${DOT_IMPORT_HOST%%:*}"

# Exit normally if already cloned
if [ -f "$IMPORT_COMPLETE" ]; then
  echo "dotCMS environment already inited.  Delete ${IMPORT_COMPLETE} to import again."
  exit 0
fi

# lock other pods out if importing
if [ -f "$IMPORT_IN_PROCESS" ]; then
  # Get lock file age in minutes (portable for Linux and macOS)
  if stat -c %Y "$IMPORT_IN_PROCESS" >/dev/null 2>&1; then
    FILE_MTIME=$(stat -c %Y "$IMPORT_IN_PROCESS")  # Linux
  else
    FILE_MTIME=$(stat -f %m "$IMPORT_IN_PROCESS")  # macOS
  fi
  CURRENT_TIME=$(date +%s)
  LOCK_AGE_MINUTES=$(( (CURRENT_TIME - FILE_MTIME) / 60 ))

  # Check if import process file is older than 30 minutes (stale lock)
  if [ "$LOCK_AGE_MINUTES" -ge 30 ]; then
    echo "ERROR: Import process appears stale (lock file is ${LOCK_AGE_MINUTES} minutes old). Removing lock file."
    rm -f "$IMPORT_IN_PROCESS"
    exit 1
  fi
  echo "ERROR: Lock file found: ${IMPORT_IN_PROCESS} (${LOCK_AGE_MINUTES} minutes old)."
  echo " Delete lock file or wait until it's 30 minutes old and try again"
  echo " sleeping for 3m"
  sleep 180
  exit 1
fi

mkdir -p $IMPORT_DATA_DIR && touch $IMPORT_IN_PROCESS

HASHED_ENV=$(echo -n "$DOT_IMPORT_ENVIRONMENT" | md5sum | cut -d ' ' -f 1)

export ASSETS_BACKUP_FILE="${IMPORT_DATA_DIR}/${HASHED_ENV}_assets.zip"
export DB_BACKUP_FILE="${IMPORT_DATA_DIR}/${HASHED_ENV}_dotcms_db.sql.gz"

# Step 1. download db and assets (if needed)
download_dotcms_db_assets || { echo "Unable to download dotcms backup"; rm $IMPORT_IN_PROCESS; exit 1; }

# Step 2. wipe database clean (if requested)
if [ "$DOT_IMPORT_DROP_DB" = "true" ]; then
    drop_db_tables || { echo "unable to drop the dotcms db schema"; rm $IMPORT_IN_PROCESS; exit 1; }
fi

# Step 3. import postgres db
import_postgres || { echo "Unable to import postgres backup"; rm $IMPORT_IN_PROCESS; exit 1; }

# Step 4. unpack assets.zip
unpack_assets || { echo "Unable to unzip assets"; rm $IMPORT_IN_PROCESS; exit 1; }

# Step 5: exit sig 13 if the clone worked
if rm -f "$IMPORT_IN_PROCESS" && touch "$IMPORT_COMPLETE"; then
  echo "dotCMS Environment $DOT_IMPORT_HOST Imported, happily exiting."
  exit 13
fi

# Otherwise, die ugly
echo "Unable complete import"
exit 1
