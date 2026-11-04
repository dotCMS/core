#!/usr/bin/env bash
# Mounted only by the test-unlogged profile; entrypoint runs this only on an EMPTY volume.
set -euo pipefail
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<'SQL'
SELECT format('COMMENT ON DATABASE %I IS %L', datname, 'dotcms-disposable-unlogged-38427')
FROM pg_database WHERE datname ~ '^dotcms[1-4]?$'
\gexec
SQL
