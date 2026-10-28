#!/usr/bin/env bash
#
# Tests for the Nx cache used by the Initial Artifact Build: maven-job's Start Nx Cache Server
# step and the server it starts (.github/actions/core-cicd/maven-job/nx-cache-server.mjs).
#
# The step's real `run:` block is pulled out of action.yml and executed, so the shell under
# test is the shell that ships. It then talks to the server it started the way Nx does
# (GET/PUT /v1/cache/<hash>).
#
# Run:  bash .github/workflows/tests/nx-cache.test.sh
# Deps: bash, awk, curl, node.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
ACTION_DIR="$REPO_ROOT/.github/actions/core-cicd/maven-job"
WORKDIR="$(mktemp -d)"
PORT=47399
trap 'pkill -f "nx-cache-server.mjs $PORT" 2>/dev/null; rm -rf "$WORKDIR"' EXIT

pass=0
fail=0
check() { # description, expected, actual
  if [[ "$2" == "$3" ]]; then pass=$((pass + 1)); printf '  ok   %s\n' "$1"
  else fail=$((fail + 1)); printf '  FAIL %s\n         -> expected [%s], got [%s]\n' "$1" "$2" "$3"; fi
}

# The `run: |` body of the step with the given id, de-indented.
run_block() {
  awk -v id="$1" '
    $0 ~ "- id: " id "$" { instep = 1; next }
    instep && /^    - / { exit }
    instep && /^      run: \|/ { inrun = 1; next }
    inrun && /^      [a-z-]+:/ { exit }
    inrun { sub(/^        /, ""); print }
  ' "$ACTION_DIR/action.yml"
}

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
URL="http://127.0.0.1:$PORT/v1/cache"

echo "Start Nx Cache Server step"
CACHE="$WORKDIR/nx-cache"
mkdir -p "$CACHE"
printf 'from an earlier run' > "$CACHE/111"
printf 'not asked for' > "$CACHE/222"
: > "$WORKDIR/github_env"
NX_CACHE_DIR="$CACHE" NX_CACHE_PORT="$PORT" GITHUB_ACTION_PATH="$ACTION_DIR" \
  GITHUB_ENV="$WORKDIR/github_env" RUNNER_TEMP="$WORKDIR" \
  bash -euo pipefail -c "$(run_block start-nx-cache)" > "$WORKDIR/step.log" 2>&1
check "step exits 0" 0 $?
check "exports the server URL for the Maven step" \
  "NX_SELF_HOSTED_REMOTE_CACHE_SERVER=http://127.0.0.1:$PORT" "$(cat "$WORKDIR/github_env")"
check "restored entries are moved aside" "111 222" "$(ls "$CACHE-restored" | tr '\n' ' ' | sed 's/ $//')"

echo "Server"
check "GET of an unknown hash is 404" 404 "$(status "$URL/999")"
check "GET of a restored hash is 200" 200 "$(status "$URL/111")"
check "and returns its bytes" "from an earlier run" "$(curl -s "$URL/111")"
check "PUT of a new hash is 200" 200 "$(status -X PUT --data-binary 'built here' "$URL/333")"
check "GET returns what was PUT" "built here" "$(curl -s "$URL/333")"
check "PUT of a stored hash is 409" 409 "$(status -X PUT --data-binary 'other' "$URL/333")"
check "a stored artifact is not overwritten" "built here" "$(curl -s "$URL/333")"
check "a path outside /v1/cache/<hash> is 404" 404 "$(status --path-as-is "$URL/..%2F..%2Fetc%2Fpasswd")"
check "the saved directory holds only what this run used or built" "111 333" \
  "$(ls "$CACHE" | tr '\n' ' ' | sed 's/ $//')"

printf '\n%d passed, %d failed\n' "$pass" "$fail"
(( fail == 0 ))
