#!/bin/bash
set -e

DB_NAME="dotcms"

# Check if the DB_LOAD_DUMP environment variable is set and the file exists
if [[ -n "${DB_LOAD_DUMP_SQL}" && -f "${DB_LOAD_DUMP_SQL}" && -z ${CUSTOM_STARTER_URL} ]]; then
    echo "Importing database dump from ${DB_LOAD_DUMP_SQL}..."
    whereis psql
    whereis pg_dump
    which psql
    which pg_dump
    sleep 10
    export PGPASSWORD=${DB_PASSWORD}
    /usr/bin/psql -h "${DB_HOST}" -U "${DB_USERNAME}" -d "${DB_NAME}" -f "${DB_LOAD_DUMP_SQL}"

    echo "Dump successfully imported."
else
    echo "Dump file not found at ${DB_LOAD_DUMP_SQL}"
fi