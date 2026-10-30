#!/usr/bin/env bash
#
# Tests for the Nx cache used by the Initial Artifact Build: maven-job's Start Nx Cache Server
# and Count Nx Cache Entries steps, the server they start
# (.github/actions/core-cicd/maven-job/nx-cache-server.mjs), and which callers may save.
#
# The steps' real `run:` blocks are pulled out of action.yml and executed, so the shell under
# test is the shell that ships. The server is then driven the way Nx drives it
# (GET/PUT /v1/cache/<hash>).
#
# Run:  bash .github/workflows/tests/nx-cache.test.sh
# Deps: bash, awk, curl, node.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
ACTION_DIR="$REPO_ROOT/.github/actions/core-cicd/maven-job"
WORKFLOWS="$REPO_ROOT/.github/workflows"
WORKDIR="$(mktemp -d)"
PORT=47399
cleanup() {
  pkill -f "nx-cache-server.mjs $PORT" 2>/dev/null
  [[ -n "${DUMMY:-}" ]] && kill "$DUMMY" 2>/dev/null
  chmod -R u+rwx "$WORKDIR" 2>/dev/null
  rm -rf "$WORKDIR"
}
trap cleanup EXIT

pass=0
fail=0
check() { # description, expected, actual
  if [[ "$2" == "$3" ]]; then pass=$((pass + 1)); printf '  ok   %s\n' "$1"
  else fail=$((fail + 1)); printf '  FAIL %s\n         -> expected [%s], got [%s]\n' "$1" "$2" "$3"; fi
}

# The `run: |` body of the maven-job step with the given id, de-indented.
run_block() {
  awk -v id="$1" '
    $0 ~ "- id: " id "$" { instep = 1; next }
    instep && /^    - / { exit }
    instep && /^      run: \|/ { inrun = 1; next }
    inrun && /^      [a-z-]+:/ { exit }
    inrun { sub(/^        /, ""); print }
  ' "$ACTION_DIR/action.yml"
}

# The `if:` line of the maven-job step with the given id.
step_if() {
  awk -v id="$1" '
    $0 ~ "- id: " id "$" { instep = 1; next }
    instep && /^    - / { exit }
    instep && /^      if:/ { print; exit }
  ' "$ACTION_DIR/action.yml"
}

start_step() { # cache dir; runs the Start Nx Cache Server step, output in $WORKDIR/step.log
  : > "$WORKDIR/github_env"
  NX_CACHE_DIR="$1" NX_CACHE_PORT="$PORT" GITHUB_ACTION_PATH="$ACTION_DIR" \
    GITHUB_ENV="$WORKDIR/github_env" RUNNER_TEMP="$WORKDIR" \
    bash -euo pipefail -c "$(run_block start-nx-cache)" > "$WORKDIR/step.log" 2>&1
}

count_step() { # cache dir; prints the entries output of Count Nx Cache Entries
  : > "$WORKDIR/github_output"
  NX_CACHE_DIR="$1" GITHUB_OUTPUT="$WORKDIR/github_output" \
    bash -euo pipefail -c "$(run_block count-nx-cache)" > /dev/null 2>&1
  sed -n 's/^entries=//p' "$WORKDIR/github_output"
}

status() { curl -s --max-time 5 -o /dev/null -w '%{http_code}' "$@"; }
URL="http://127.0.0.1:$PORT/v1/cache"
CACHE="$WORKDIR/nx-cache"

echo "Start Nx Cache Server step, port already taken"
# Something else holds the port and answers 404 to everything, as the server would for an
# unknown hash. Nx must not be pointed at it.
node -e "require('http').createServer((q, r) => r.writeHead(404).end()).listen($PORT, '127.0.0.1')" &
DUMMY=$!
sleep 0.5
mkdir -p "$WORKDIR/taken"
start_step "$WORKDIR/taken"
check "step exits 0" 0 $?
check "does not export a server URL" "" "$(cat "$WORKDIR/github_env")"
check "warns that the build runs without the cache" 1 \
  "$(grep -c 'building core-web without the Nx cache' "$WORKDIR/step.log")"
check "logs why the server stopped" 1 "$(grep -c 'cannot listen on 127.0.0.1' "$WORKDIR/step.log")"
kill "$DUMMY"; wait "$DUMMY" 2>/dev/null; DUMMY=
sleep 0.3

echo "Start Nx Cache Server step"
mkdir -p "$CACHE"
printf 'from an earlier run' > "$CACHE/111"
printf 'not asked for' > "$CACHE/222"
start_step "$CACHE"
check "step exits 0" 0 $?
check "exports the server URL for the Maven step" \
  "NX_SELF_HOSTED_REMOTE_CACHE_SERVER=http://127.0.0.1:$PORT" "$(cat "$WORKDIR/github_env")"
check "restored entries are moved aside" "111 222" "$(ls "$CACHE-restored" | tr '\n' ' ' | sed 's/ $//')"

echo "Count Nx Cache Entries step, before Nx asked for anything"
check "an empty directory counts 0, so nothing is saved" 0 "$(count_step "$CACHE")"

echo "Server"
check "GET of an unknown hash is 404" 404 "$(status "$URL/999")"
check "GET of a restored hash is 200" 200 "$(status "$URL/111")"
check "and returns its bytes" "from an earlier run" "$(curl -s --max-time 5 "$URL/111")"
check "PUT of a new hash is 200" 200 "$(status -X PUT --data-binary 'built here' "$URL/333")"
check "GET returns what was PUT" "built here" "$(curl -s --max-time 5 "$URL/333")"
check "PUT of a stored hash is 409" 409 "$(status -X PUT --data-binary 'other' "$URL/333")"
check "a stored artifact is not overwritten" "built here" "$(curl -s --max-time 5 "$URL/333")"
check "a path outside /v1/cache/<hash> is 404" 404 "$(status --path-as-is "$URL/..%2F..%2Fetc%2Fpasswd")"

# A PUT that dies mid-body: it announces 100 bytes, sends 3, and the client gives up.
curl -s -o /dev/null -X PUT -H 'Content-Length: 100' --data-binary 'abc' --max-time 1 "$URL/444"
sleep 0.5
check "an aborted PUT stores nothing" 404 "$(status "$URL/444")"
check "and leaves no partial file behind" "" "$(ls "$CACHE" | grep -E '^444' || true)"

if [[ "$(id -u)" != 0 ]]; then
  # An artifact the server cannot read: the request fails, the server keeps serving.
  printf 'unreadable' > "$CACHE/555"
  chmod 000 "$CACHE/555"
  curl -s --max-time 5 -o /dev/null "$URL/555"
  rc=$?
  # 0 would be a short artifact passed off as complete, 28 a response left hanging.
  check "a failed GET ends the response with an error" yes \
    "$( (( rc != 0 && rc != 28 )) && echo yes || echo "no (curl exit $rc)")"
  check "a failed GET does not stop the server" "built here" "$(curl -s --max-time 5 "$URL/333")"
  rm -f "$CACHE/555"
fi

check "the saved directory holds only what this run used or built" "111 333" \
  "$(ls "$CACHE" | tr '\n' ' ' | sed 's/ $//')"

echo "Count Nx Cache Entries step, after the build"
check "counts the entries this run used or built" 2 "$(count_step "$CACHE")"

echo "Who may save"
check "Save Nx Cache requires save-nx-cache and a non-empty directory" 1 \
  "$(step_if save-cache-nx | grep -c "inputs.save-nx-cache == 'true'.*count-nx-cache.outputs.entries != '0'")"
check "the build phase passes its save-nx-cache input on" 1 \
  "$(grep -c 'save-nx-cache: \${{ inputs.save-nx-cache }}' "$WORKFLOWS/cicd_comp_build-phase.yml")"
check "trunk saves on a push to main only" 1 \
  "$(grep -c "save-nx-cache: \${{ github.event_name == 'push' && github.ref == 'refs/heads/main' }}" "$WORKFLOWS/cicd_3-trunk.yml")"
check "the nightly saves on its schedule only" 1 \
  "$(grep -c "save-nx-cache: \${{ github.event_name == 'schedule' }}" "$WORKFLOWS/cicd_4-nightly.yml")"
check "no other workflow sets save-nx-cache" "" \
  "$(grep -l 'save-nx-cache:' "$WORKFLOWS"/*.yml | grep -v -E '/cicd_(3-trunk|4-nightly|comp_build-phase)\.yml$' || true)"

printf '\n%d passed, %d failed\n' "$pass" "$fail"
(( fail == 0 ))
