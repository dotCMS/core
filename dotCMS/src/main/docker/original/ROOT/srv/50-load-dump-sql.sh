#!/bin/bash
set -e

DB_NAME="dotcms"

# Check if the DB_LOAD_DUMP environment variable is set and the file exists
if [[ -n "${DB_LOAD_DUMP_SQL}" && -f "${DB_LOAD_DUMP_SQL}" ]]; then
    echo "Importing database dump from ${DB_LOAD_DUMP_SQL}..."
    psql -h ${DB_BASE_URL} -U ${DB_USERNAME} -d ${DB_NAME} -f "${DB_LOAD_DUMP_SQL}"
    echo "Dump successfully imported."
else
    echo "Dump file not found at ${DB_LOAD_DUMP_SQL}"
fi