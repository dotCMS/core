#!/usr/bin/env bash
#
# Repro driver for issue #36544 — pub/sub listener connection churn.
#
# WHAT THIS DOES
#   Repeatedly terminates the Postgres backend that holds the dedicated pub/sub
#   LISTEN connection, then reports how many times dotCMS rebuilt its listener.
#
# WHY THIS IS THE RIGHT TRIGGER
#   The production incident reached ~3,687 PGListener instantiations via a startup
#   crash-loop, which is slow and awkward to reproduce. But the churn arm of the bug
#   only needs the listener's connection to die repeatedly. Killing the backend
#   server-side reproduces exactly that, deterministically, in seconds — and it
#   decouples the churn from the crash-loop so candidate fixes can be A/B tested.
#
#   After each kill, the next publish() (any cache invalidation) calls
#   JDBCPubSubImpl.listener(), finds the listener not listening, and constructs a
#   brand new PGListener — borrowing a fresh pooled connection and re-issuing
#   LISTEN for every subscribed topic (JDBCPubSubImpl.java:63-69, 121-128, 291).
#
# THE METRIC THAT MATTERS
#   pg_stat_statements.calls for 'LISTEN cluster_actions' is a direct
#   PGListener-instantiation counter. In the incident it read 3,687.
#   If a fix works, this counter stops tracking the kill count 1:1.
#
# USAGE
#   docker compose up -d          # wait for both nodes to finish starting
#   ./churn.sh                    # default: 30 cycles, 5s apart
#   CYCLES=100 INTERVAL=2 ./churn.sh
#   ./churn.sh --baseline         # report only, kill nothing

set -euo pipefail

CYCLES="${CYCLES:-30}"
INTERVAL="${INTERVAL:-5}"
DB_SERVICE="${DB_SERVICE:-db}"
DB_USER="${DB_USER:-dotcmsdbuser}"
DB_NAME="${DB_NAME:-dotcms}"

# Drive cache invalidations so publish() gets called, which is what actually
# triggers listener rebuilds. Point this at a page on node 1.
LOAD_URL="${LOAD_URL:-http://localhost:8082/}"

psql_q() {
  docker compose exec -T "$DB_SERVICE" \
    psql -U "$DB_USER" -d "$DB_NAME" -At -c "$1"
}

# A PGListener backend is identified by its poll statement: runInternal() issues a
# bare `SELECT 1` on the held connection every PGLISTENER_SLEEP_BETWEEN_RUNS ms
# (JDBCPubSubImpl.java:198). Nothing else in dotCMS issues a bare `SELECT 1` on a
# loop, and Hikari's isValid() uses a protocol-level ping rather than a query, so
# this heuristic is reliable for this harness.
LISTENER_BACKEND_PRED="datname = '${DB_NAME}' AND query = 'SELECT 1' AND pid <> pg_backend_pid()"

listener_instantiations() {
  psql_q "SELECT COALESCE(SUM(calls), 0) FROM pg_stat_statements
          WHERE query ILIKE 'LISTEN %';"
}

validation_query_stats() {
  psql_q "SELECT COALESCE(SUM(calls), 0)::text || ' calls, ' ||
                 COALESCE(ROUND(MAX(mean_exec_time)::numeric, 1), 0)::text || 'ms avg, ' ||
                 COALESCE(ROUND(SUM(total_exec_time)::numeric / 1000, 1), 0)::text || 's total'
          FROM pg_stat_statements WHERE query ILIKE '%as test from inode%';"
}

total_connections() {
  psql_q "SELECT count(*) FROM pg_stat_activity WHERE datname = '${DB_NAME}';"
}

live_listener_backends() {
  psql_q "SELECT count(*) FROM pg_stat_activity WHERE ${LISTENER_BACKEND_PRED};"
}

report() {
  printf '  listener instantiations (LISTEN calls) : %s\n' "$(listener_instantiations)"
  printf '  live listener backends                 : %s\n' "$(live_listener_backends)"
  printf '  total connections to %-18s: %s\n' "$DB_NAME" "$(total_connections)"
  printf '  validation query                       : %s\n' "$(validation_query_stats)"
}

echo "=== issue #36544 pub/sub churn harness ==="
echo
echo "Baseline:"
report
echo

if [[ "${1:-}" == "--baseline" ]]; then
  echo "Baseline only; nothing killed."
  exit 0
fi

BASE_INSTANTIATIONS="$(listener_instantiations)"

echo "Running $CYCLES kill cycles at ${INTERVAL}s intervals."
echo "Expect the instantiation counter to climb ~1:1 with kills on unfixed code."
echo

for i in $(seq 1 "$CYCLES"); do
  killed="$(psql_q "SELECT count(pg_terminate_backend(pid))
                    FROM pg_stat_activity WHERE ${LISTENER_BACKEND_PRED};")"

  # Poke the system so a publish() happens and the listener gets rebuilt.
  # Any cache invalidation will do; a page request is the cheapest trigger.
  curl -sk -o /dev/null --max-time 10 "$LOAD_URL" || true

  sleep "$INTERVAL"

  printf '[cycle %3d/%s] killed=%s  instantiations=%s  live_listeners=%s  conns=%s\n' \
    "$i" "$CYCLES" "$killed" \
    "$(listener_instantiations)" "$(live_listener_backends)" "$(total_connections)"
done

END_INSTANTIATIONS="$(listener_instantiations)"

echo
echo "=== Result ==="
printf '  kill cycles                : %s\n' "$CYCLES"
printf '  listener instantiations    : %s -> %s (delta %s)\n' \
  "$BASE_INSTANTIATIONS" "$END_INSTANTIATIONS" \
  "$((END_INSTANTIATIONS - BASE_INSTANTIATIONS))"
echo
report
echo
cat <<'INTERPRET'
How to read this
  delta ~= cycles      Churn reproduced. Each listener death costs a fresh pooled
                       connection borrow plus a re-LISTEN of every topic, with no
                       backoff and no cap. This is the incident's mechanism.

  live listeners > 1   A listener was orphaned. Connections leak only in the window
                       between the PGListener constructor's borrow
                       (JDBCPubSubImpl.java:128) and Thread.start() (:69) — the
                       thread never runs, so run()'s finally never closes it.
                       Note the normal death paths do NOT leak; run()'s
                       finally { stopListening(); } covers them (:177-184).

  conns pinned at max  Pool exhausted. Cross-check the container logs for
                       "jdbc/dotCMSPool - Connection is not available".

  ProxyLeakTask warns  Expected, not a finding. The pub/sub connection is held
                       forever by design and DB_LEAK_DETECTION_THRESHOLD is 2s in
                       this harness. Do not treat it as leak evidence.
INTERPRET
