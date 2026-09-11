#!/usr/bin/env bash
#
# verify-cold-start.sh — acceptance checks for the compose file that
# @dotcms/create-app ships (../assets/docker-compose.yml).
#
# This checks the CLI's OWN bundled stack, not the shared
# docker/docker-compose-examples/single-node-demo-site example, which this work
# deliberately leaves untouched (see cli-design-decisions.md D4).
#
# It is the executable form of the manual procedure in
# specs/37262-create-app-docker-uve/quickstart.md steps 1-3. It exists because the
# behavior it checks (dependency ordering, restart on exit, port binding) cannot be
# unit-tested: it needs a real Docker daemon, real image pulls, and a multi-minute
# starter import. See issue #37262.
#
# Covers:
#   T005  cold start: db + opensearch healthy before dotcms; dotcms healthy unaided
#   T006  recovery:   dotcms exits on its own -> restarted by the policy, not left exited
#   T007  exposure:   8090 reachable on loopback, REFUSED on the LAN address
#   T008  regression: the CUSTOM_STARTER_URL line shape installed CLIs depend on
#
# Usage:
#   ./verify-cold-start.sh              # full run (destroys volumes, ~10 min cold)
#   ./verify-cold-start.sh --static     # config-only checks, no containers (fast)
#
# Exit 0 = all assertions passed.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$SCRIPT_DIR/../assets/docker-compose.yml}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-600}"
RESTART_GRACE="${RESTART_GRACE:-30}"
# dotCMS needs ~46s to boot, and T006 restarts it just before the exposure check.
HEALTH_GRACE="${HEALTH_GRACE:-240}"

PASS=0
FAIL=0

ok()   { printf '  \033[32mPASS\033[0m  %s\n' "$1"; PASS=$((PASS + 1)); }
bad()  { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAIL=$((FAIL + 1)); }
info() { printf '        %s\n' "$1"; }
section() { printf '\n\033[1m%s\033[0m\n' "$1"; }

compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

# Health state of a compose service's container, or "none" when it declares no
# healthcheck. Distinguishing "no healthcheck" from "unhealthy" matters: the bug
# being fixed is the absence of a healthcheck, not a failing one.
# NOTE: `compose ps -q` lists RUNNING containers only, so a killed container reads
# back as "missing" and the failure gets misdiagnosed as "container gone" when the
# real state is "exited and never restarted". Always use -a here.
health_of() {
    local svc="$1" cid
    cid="$(compose ps -aq "$svc" 2>/dev/null | head -1)"
    [ -n "$cid" ] || { echo "missing"; return; }
    docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$cid" 2>/dev/null || echo "missing"
}

state_of() {
    local svc="$1" cid
    cid="$(compose ps -aq "$svc" 2>/dev/null | head -1)"
    [ -n "$cid" ] || { echo "missing"; return; }
    docker inspect -f '{{.State.Status}}' "$cid" 2>/dev/null || echo "missing"
}

# ---------------------------------------------------------------------------
# T008 + static half of T005 — assertions on the compose file itself.
# These run without Docker and are the cheapest guard on the highest-consequence
# regression in this change.
# ---------------------------------------------------------------------------
static_checks() {
    section "Static checks (compose file)"

    # T008 — CUSTOM_STARTER_URL line shape.
    #
    # updateDockerComposeStarterUrl() in core-web/libs/sdk/create-app/src/index.ts
    # rewrites this file with the regex below when --starter is passed, and THROWS
    # if nothing matches. Every already-installed CLI carries that code, so breaking
    # this line shape disables --starter for all of them with no release able to
    # reach them. Grep the RAW file, not `docker compose config`: the CLI regexes
    # the bytes on disk, not the normalized config.
    if grep -qE '^[[:space:]]*["'"'"']?CUSTOM_STARTER_URL["'"'"']?[[:space:]]*:[[:space:]]*.+$' "$COMPOSE_FILE"; then
        ok "T008 CUSTOM_STARTER_URL matches the rewrite regex installed CLIs use"
    else
        bad "T008 CUSTOM_STARTER_URL line shape broken — --starter would throw in every installed CLI"
    fi

    # Ordering is declared, not just observed. A passing runtime check can happen by
    # luck on a warm machine; the declaration is what makes it reproducible.
    local cfg
    cfg="$(compose config 2>/dev/null)"
    if [ -z "$cfg" ]; then
        bad "compose config failed to render — the file is invalid"
        return
    fi

    if printf '%s' "$cfg" | grep -A3 -E '^\s+db:' | grep -q 'condition: service_healthy' \
       || printf '%s' "$cfg" | python3 -c '
import sys,re
cfg = sys.stdin.read()
m = re.search(r"^  dotcms:.*?(?=^  \S|\Z)", cfg, re.S | re.M)
sys.exit(0 if m and "service_healthy" in m.group(0) else 1)
'; then
        ok "T005 dotcms depends_on declares a service_healthy condition"
    else
        bad "T005 dotcms depends_on has no service_healthy condition — it can start against a cold Postgres"
    fi

    for svc in db opensearch dotcms; do
        if printf '%s' "$cfg" | python3 -c "
import sys,re
cfg = sys.stdin.read()
m = re.search(r'^  $svc:.*?(?=^  \S|\Z)', cfg, re.S | re.M)
sys.exit(0 if m and 'healthcheck:' in m.group(0) else 1)
"; then
            ok "T005 $svc declares a healthcheck"
        else
            bad "T005 $svc has no healthcheck"
        fi

        if printf '%s' "$cfg" | python3 -c "
import sys,re
cfg = sys.stdin.read()
m = re.search(r'^  $svc:.*?(?=^  \S|\Z)', cfg, re.S | re.M)
sys.exit(0 if m and 'restart:' in m.group(0) else 1)
"; then
            ok "T005 $svc declares a restart policy"
        else
            bad "T005 $svc has no restart policy — it stays dead after an exit"
        fi
    done

    # T007 (static half) — 8090 must be published, and bound to loopback only.
    # The management port is authorized purely by the port a request arrives on:
    # no credential check, no IP allowlist (InfrastructureManagementFilter). A
    # wildcard binding puts /dotmgt/health and /dotmgt/metrics on the local network.
    if grep -qE '^[[:space:]]*-[[:space:]]*["'"'"']?127\.0\.0\.1:8090:8090' "$COMPOSE_FILE"; then
        ok "T007 8090 published on loopback only"
    elif grep -qE '^[[:space:]]*-[[:space:]]*["'"'"']?8090:8090' "$COMPOSE_FILE"; then
        bad "T007 8090 published on 0.0.0.0 — unauthenticated /dotmgt/* exposed to the network"
    else
        bad "T007 8090 not published — the CLI cannot use /dotmgt/readyz"
    fi
}

# ---------------------------------------------------------------------------
# T005 — cold start with no manual intervention.
# ---------------------------------------------------------------------------
runtime_cold_start() {
    section "Cold start (destroys volumes; this is the real test)"
    info "docker compose down -v"
    compose down -v >/dev/null 2>&1

    # Report whether this is a genuine cold start. With images already cached the
    # whole stack can come up in seconds, which is NOT the scenario users hit.
    if docker image inspect dotcms/dotcms:latest >/dev/null 2>&1; then
        info "NOTE: dotcms/dotcms:latest is already cached — this is a warm start."
        info "      The reported race (dotcms beating Postgres) is timing-dependent and"
        info "      may not reproduce here. Run 'docker rmi dotcms/dotcms:latest' first"
        info "      for a true cold start."
    fi

    info "docker compose up -d --wait --wait-timeout $WAIT_TIMEOUT"
    local started rc
    started=$(date +%s)
    compose up -d --wait --wait-timeout "$WAIT_TIMEOUT"
    rc=$?
    local elapsed=$(( $(date +%s) - started ))
    info "took ${elapsed}s, exit=$rc"

    # `--wait` is only as good as the healthchecks. For a service with NO healthcheck
    # it waits for "running", not "ready" — so on the unfixed file it exits 0 within
    # seconds and reports every service "Healthy" while dotCMS is still booting. That
    # false green is why --wait alone does not fix the CLI; it needs the healthchecks.
    if [ $rc -eq 0 ] && [ "$(health_of dotcms)" = "none" ]; then
        bad "T005 'up --wait' exited 0 in ${elapsed}s but dotcms has NO healthcheck — this is a false green, not readiness"
    elif [ $rc -eq 0 ]; then
        ok "T005 'up --wait' exited 0 — every service reached ready without help"
    else
        bad "T005 'up --wait' exited $rc — a service never became ready (this is the reported bug)"
    fi

    for svc in db opensearch dotcms; do
        local h s
        h="$(health_of "$svc")"; s="$(state_of "$svc")"
        case "$h" in
            healthy) ok "T005 $svc is healthy" ;;
            none)    bad "T005 $svc has no healthcheck (state=$s) — readiness is unobservable" ;;
            *)       bad "T005 $svc health=$h state=$s" ;;
        esac
    done

    # The reported symptom, asserted directly: dotcms must not have died on the way up.
    local cid restarts
    cid="$(compose ps -q dotcms 2>/dev/null)"
    if [ -n "$cid" ]; then
        restarts="$(docker inspect -f '{{.RestartCount}}' "$cid" 2>/dev/null || echo '?')"
        if [ "$restarts" = "0" ]; then
            ok "T005 dotcms never had to restart — it did not race its dependencies"
        else
            info "dotcms RestartCount=$restarts (it recovered, but it still lost the race)"
            ok "T005 dotcms is up (recovered via restart policy after $restarts restart(s))"
        fi
    fi
}

# ---------------------------------------------------------------------------
# T006 — recovery. Compose restart policies react to container EXIT, not to health
# status. But they also do NOT react to an EXTERNALLY initiated stop: Docker treats
# `docker kill` as a user-requested stop and deliberately declines to restart, so
# asserting a restart after `docker kill` can never pass.
#
#   measured 2026-08-31 (docker 29.4):
#     self-exit    -> RestartCount=4, status=running   <- policy applied
#     docker kill  -> RestartCount=0, status=exited    <- policy skipped
#
# So we make the container exit ON ITS OWN, which is also what actually happened in
# #37262 (dotcms died because Postgres was not accepting connections yet).
# PID 1 is tini and the JVM is a descendant of it, so the JVM can be signalled from
# inside the container; tini then reaps it and exits. Signalling PID 1 directly would
# not work — the kernel refuses SIGKILL to PID 1 from within its own PID namespace.
# ---------------------------------------------------------------------------
runtime_restart() {
    section "Recovery after an unexpected exit"
    local cid before
    cid="$(compose ps -q dotcms 2>/dev/null)"
    if [ -z "$cid" ]; then
        bad "T006 no dotcms container to crash"
        return
    fi
    before="$(docker inspect -f '{{.RestartCount}}' "$cid" 2>/dev/null || echo 0)"

    info "killing the JVM inside $cid (self-exit, not docker kill)"
    if ! docker exec "$cid" bash -c 'pkill -9 -f "^/.*java" || pkill -9 java' >/dev/null 2>&1; then
        info "pkill returned non-zero (process may already be gone); continuing"
    fi

    local waited=0 s
    while [ $waited -lt $RESTART_GRACE ]; do
        sleep 3; waited=$((waited + 3))
        s="$(state_of dotcms)"
        [ "$s" = "running" ] && [ "$(docker inspect -f '{{.RestartCount}}' "$cid" 2>/dev/null || echo 0)" -gt "$before" ] && break
    done

    s="$(state_of dotcms)"
    local after
    after="$(docker inspect -f '{{.RestartCount}}' "$cid" 2>/dev/null || echo 0)"
    if [ "$s" = "running" ] && [ "$after" -gt "$before" ]; then
        ok "T006 dotcms exited and was restarted by the policy (${waited}s, RestartCount ${before}->${after})"
    elif [ "$s" = "running" ]; then
        bad "T006 dotcms is running but RestartCount did not move (${before}->${after}) — the JVM kill did not take, so the restart policy was never exercised"
    else
        bad "T006 dotcms state=$s after ${RESTART_GRACE}s — it stayed dead, exactly as reported"
    fi
}

# ---------------------------------------------------------------------------
# T007 — the management port answers on loopback and NOT on the LAN.
# ---------------------------------------------------------------------------
# Waits for dotcms to be healthy again.
#
# T006 deliberately crashes the container immediately before this, and dotCMS takes ~46s to boot.
# Without this wait, T007 probed a container that was 3 seconds into a restart and reported
# "the management port is not published" — accusing a correct compose file of a defect it does
# not have. Verified 2026-09-01: against a healthy stack the same probe returns 200 on loopback
# and is refused on the LAN address, exactly as AC-011 requires.
#
# The wait lives here rather than at the end of T006 so this check does not depend on what ran
# before it.
wait_until_healthy() {
    local svc="$1" waited=0
    while [ $waited -lt "$HEALTH_GRACE" ]; do
        [ "$(health_of "$svc")" = "healthy" ] && return 0
        sleep 5; waited=$((waited + 5))
    done

    return 1
}

runtime_exposure() {
    section "Management port exposure"

    if ! wait_until_healthy dotcms; then
        bad "T007 dotcms did not return to healthy within ${HEALTH_GRACE}s — cannot test exposure"
        return
    fi

    # Probe livez, not readyz. This is an EXPOSURE test — it asks whether the port is
    # reachable on loopback, and livez is what the container healthcheck guarantees.
    # readyz lags it: measured 2026-08-31, readyz returned 503 for a few seconds after
    # `up --wait` already reported the container Healthy, which would flake this check.
    local loopback_answers=1
    if curl -fsS --max-time 10 http://127.0.0.1:8090/dotmgt/livez >/dev/null 2>&1; then
        ok "T007 /dotmgt/livez answers on 127.0.0.1:8090"
        loopback_answers=0
    else
        bad "T007 /dotmgt/livez unreachable on 127.0.0.1:8090 — the management port is not published"
    fi

    local lan="${LAN_ADDR:-}"
    if [ -z "$lan" ] && command -v ipconfig >/dev/null 2>&1; then
        lan="$(ipconfig getifaddr en0 2>/dev/null || true)"
    fi
    [ -n "$lan" ] || lan="$(hostname -I 2>/dev/null | awk '{print $1}')"

    # A security assertion that cannot run must FAIL, not pass quietly. This check is the only
    # thing standing behind AC-011 — that unauthenticated /dotmgt/health and /dotmgt/metrics are
    # not on the network — and an earlier version of this function skipped it with `info` while
    # the suite still printed "0 failed". A green summary that omits this check is worse than a
    # red one, because nobody goes looking.
    if [ -z "$lan" ]; then
        bad "T007 no LAN address found — AC-011 is UNVERIFIED, not satisfied. Pass LAN_ADDR=<ip> to check it explicitly."
        return
    fi

    # Guard against a vacuous pass: if 8090 is not published at all, the LAN refusal
    # below is trivially true and proves nothing about the binding. Only treat the
    # refusal as meaningful once loopback actually answers.
    #
    # This reuses the probe above rather than re-issuing one, and it must use the SAME
    # endpoint. It previously probed readyz while the check above used livez, and readyz
    # lags livez by a few seconds after a restart — so straight after T006 the guard saw a
    # 503, declared 8090 unpublished, and silently skipped the AC-011 assertion while the
    # suite still reported "16 passed, 0 failed".
    if [ "$loopback_answers" -ne 0 ]; then
        info "T007 8090 does not answer on loopback either — skipping the LAN check as vacuous"
        return
    fi

    if curl -fsS --max-time 5 "http://$lan:8090/dotmgt/livez" >/dev/null 2>&1; then
        bad "T007 8090 ANSWERS on $lan — unauthenticated /dotmgt/health and /dotmgt/metrics are on the network"
    else
        ok "T007 8090 refused on $lan (loopback-only, as required)"
    fi
}

# ---------------------------------------------------------------------------

main() {
    printf '\033[1mverify-cold-start.sh\033[0m — %s\n' "$COMPOSE_FILE"

    static_checks

    if [ "${1:-}" = "--static" ]; then
        info "--static given; skipping runtime checks"
    else
        runtime_cold_start
        runtime_restart
        runtime_exposure
    fi

    section "Summary"
    printf '  %d passed, %d failed\n\n' "$PASS" "$FAIL"
    [ "$FAIL" -eq 0 ]
}

main "$@"
