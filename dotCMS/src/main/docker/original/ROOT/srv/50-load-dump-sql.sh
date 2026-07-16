#!/bin/bash
set -e


# Check if the DB_LOAD_DUMP environment variable is set and the file exists
if [[ -n "${DB_LOAD_DUMP_SQL}" && -f "${DB_LOAD_DUMP_SQL}" && -z ${CUSTOM_STARTER_URL} ]]; then
    echo "Importing database dump from ${DB_LOAD_DUMP_SQL}..."
    sleep 10
    export PGPASSWORD=${DB_PASSWORD}
    /usr/bin/psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_NAME}" -f "${DB_LOAD_DUMP_SQL}"
    unset PGPASSWORD
    echo "Dump successfully imported."
elif [[ -n ${DOT_STARTER_DATA_LOAD} ]]; then
    echo "Importing data from starter ${CUSTOM_STARTER_URL}..."
else
    echo "Dump file not found [${DB_LOAD_DUMP_SQL}]"
fi
