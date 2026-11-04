#!/usr/bin/env bash
# Real PostgreSQL checks in a unique, disposable container. Never discovers/reuses another DB.
set -euo pipefail
cd "$(dirname "$0")"
container="unlogged-self-test-$$-$RANDOM"
trap 'docker rm -fv "$container" >/dev/null 2>&1 || true' EXIT
# Use the same image as CI (or an explicitly supplied local image).
docker run -d --name "$container" --network none -e POSTGRES_HOST_AUTH_METHOD=trust \
  -e POSTGRES_DB=dotcms1 \
  -v "$PWD/zz-allow-disposable.sh:/docker-entrypoint-initdb.d/zz-allow-disposable.sh:ro" \
  "${UNLOGGED_TEST_IMAGE:-mirror.gcr.io/pgvector/pgvector:pg18}" >/dev/null
sql() { docker exec -i "$container" psql -X -v ON_ERROR_STOP=1 -U postgres -d dotcms1 "$@"; }
for attempt in {1..60}; do
  if sql -Atc "SELECT shobj_description(oid, 'pg_database') FROM pg_database WHERE datname = current_database()" \
      2>/dev/null | grep -qx dotcms-disposable-unlogged-38427; then break; fi
  if [[ $attempt == 60 ]]; then docker logs "$container"; exit 1; fi
  sleep 1
done
sql <<'SQL'
CREATE TABLE inode(id int PRIMARY KEY);
CREATE TABLE contentlet(id int PRIMARY KEY REFERENCES inode(id));
CREATE TABLE "quoted""table"(id int PRIMARY KEY, parent int REFERENCES "quoted""table"(id));
INSERT INTO inode VALUES (1);
INSERT INTO contentlet VALUES (1);
INSERT INTO "quoted""table" VALUES (1, 1);
CREATE SCHEMA untouched;
CREATE TABLE untouched.fixture(id int);
CREATE EXTENSION vector;
CREATE TABLE extension_fixture(id int);
ALTER EXTENSION vector ADD TABLE extension_fixture;
SQL
sql -f - < convert.sql
sql <<'SQL'
DO $$ BEGIN
  IF (SELECT count(*) FROM contentlet JOIN inode USING(id)) <> 1
     OR (SELECT count(*) FROM "quoted""table") <> 1 THEN
    RAISE EXCEPTION 'fixture rows were lost';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_class WHERE oid IN ('inode'::regclass, 'contentlet'::regclass,
                '"quoted""table"'::regclass) AND relpersistence <> 'u') THEN
    RAISE EXCEPTION 'application table was not converted';
  END IF;
  IF (SELECT relpersistence FROM pg_class WHERE oid = 'untouched.fixture'::regclass) <> 'p'
     OR (SELECT relpersistence FROM pg_class WHERE oid = 'extension_fixture'::regclass) <> 'p' THEN
    RAISE EXCEPTION 'out-of-scope table was changed';
  END IF;
  BEGIN
    INSERT INTO contentlet VALUES(2);
    RAISE EXCEPTION 'foreign key was lost';
  EXCEPTION WHEN foreign_key_violation THEN NULL;
  END;
END $$;
SQL
sql -f - < convert.sql 2>&1 | tee /tmp/"$container"-repeat.log
grep -q 'converted=0' /tmp/"$container"-repeat.log
rm /tmp/"$container"-repeat.log
# A valid table is rewritten before the cycle is discovered; the whole DO must roll back.
sql <<'SQL'
DROP EXTENSION vector CASCADE;
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
CREATE TABLE inode(id int PRIMARY KEY);
CREATE TABLE contentlet(id int PRIMARY KEY);
CREATE TABLE a(id int PRIMARY KEY, b int);
CREATE TABLE b(id int PRIMARY KEY, a int REFERENCES a(id));
ALTER TABLE a ADD FOREIGN KEY(b) REFERENCES b(id);
SQL
if sql -f - < convert.sql; then echo 'cycle unexpectedly accepted' >&2; exit 1; fi
sql <<'SQL'
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relpersistence <> 'p') THEN
    RAISE EXCEPTION 'failure did not roll back prior table rewrites';
  END IF;
END $$;
COMMENT ON DATABASE dotcms1 IS NULL;
SQL
if sql -f - < convert.sql; then echo 'unmarked target unexpectedly accepted' >&2; exit 1; fi
sql <<'SQL'
COMMENT ON DATABASE dotcms1 IS 'dotcms-disposable-unlogged-38427';
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
SQL
if sql -f - < convert.sql; then echo 'empty schema unexpectedly accepted' >&2; exit 1; fi
sql <<'SQL'
CREATE TABLE inode(id int PRIMARY KEY);
CREATE TABLE contentlet(id int PRIMARY KEY);
CREATE SCHEMA inherited;
CREATE TABLE inherited.child() INHERITS (inode);
SQL
if sql -f - < convert.sql; then echo 'inheritance unexpectedly accepted' >&2; exit 1; fi
sql -c 'DROP SCHEMA inherited CASCADE; CREATE MATERIALIZED VIEW unsupported AS SELECT 1 AS id'
if sql -f - < convert.sql; then echo 'materialized view unexpectedly accepted' >&2; exit 1; fi
sql -c 'DROP MATERIALIZED VIEW unsupported; CREATE TABLE partitioned(id int) PARTITION BY RANGE(id)'
if sql -f - < convert.sql; then echo 'partitioning unexpectedly accepted' >&2; exit 1; fi
sql -c 'DROP TABLE partitioned; CREATE ROLE limited LOGIN'
if sql -U limited -f - < convert.sql; then echo 'non-owner unexpectedly accepted' >&2; exit 1; fi
# A live lock must time out rather than hanging preparation indefinitely.
sql -c 'BEGIN; LOCK contentlet IN ACCESS EXCLUSIVE MODE; SELECT pg_sleep(12); COMMIT' >/dev/null &
locker=$!
for attempt in {1..50}; do
  if [[ $(sql -Atc "SELECT count(*) FROM pg_locks WHERE relation = 'contentlet'::regclass AND mode = 'AccessExclusiveLock' AND granted") == 1 ]]; then break; fi
  sleep 0.1
done
if sql -f - < convert.sql; then echo 'locked table unexpectedly converted' >&2; exit 1; fi
wait "$locker"
sql <<'SQL'
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_class WHERE oid IN ('inode'::regclass, 'contentlet'::regclass)
             AND relpersistence <> 'p') THEN
    RAISE EXCEPTION 'lock failure did not roll back table rewrites';
  END IF;
END $$;
SQL
echo 'UNLOGGED SQL self-checks passed (preservation, isolation, idempotence, rollback, target/readiness guards, unsupported kinds, permissions, lock timeout)'
