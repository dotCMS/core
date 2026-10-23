#!/usr/bin/env bash
#
# Tests for .github/actions/core-cicd/maven-job/pre-pull-images.sh (#38189).
#
# The real script is executed with docker, sleep and timeout stubbed through PATH, so
# nothing is pulled and no test sleeps. The stub docker logs every call (with the
# DOCKER_CONFIG it saw) and is steered by env vars:
#   STUB_PRESENT=1       `docker image inspect` succeeds
#   STUB_MIRROR_FAILS=N  the first N mirror.gcr.io pulls fail (all, if N is large)
#   STUB_HUB_OK=1        a Docker Hub pull succeeds
#
# Run:  bash .github/workflows/tests/docker-pre-pull.test.sh
# Run in CI by cicd_pr_workflow-lint.yml, which executes every suite in this directory.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SCRIPT="$REPO_ROOT/.github/actions/core-cicd/maven-job/pre-pull-images.sh"
ENV_FILE="$REPO_ROOT/environments/environment.properties"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

pass=0
fail=0
ok()  { pass=$((pass + 1)); printf '  ok   %s\n' "$1"; }
bad() { fail=$((fail + 1)); printf '  FAIL %s\n' "$1"; [[ -n "${2:-}" ]] && printf '         -> %s\n' "$2"; return 0; }
check() { if [[ "$1" == "$2" ]]; then ok "$3"; else bad "$3" "expected '$2', got '$1'"; fi; }

BIN="$WORKDIR/bin"
mkdir -p "$BIN"
cat > "$BIN/docker" <<'STUB'
#!/usr/bin/env bash
echo "docker $* [DOCKER_CONFIG=${DOCKER_CONFIG:-default}]" >> "$STUB_LOG"
case "$1" in
  image) [[ "${STUB_PRESENT:-0}" == "1" ]]; exit $? ;;
  tag)   exit 0 ;;
  pull)
    if [[ "$2" == mirror.gcr.io/* ]]; then
      n=$(cat "$STUB_COUNT" 2>/dev/null || echo 0); echo $((n + 1)) > "$STUB_COUNT"
      [[ "$n" -ge "${STUB_MIRROR_FAILS:-0}" ]]; exit $?
    fi
    [[ "${STUB_HUB_OK:-0}" == "1" ]]; exit $? ;;
esac
STUB
# shellcheck disable=SC2016  # $* and $STUB_LOG are meant to expand in the stub, not here
printf '#!/usr/bin/env bash\necho "sleep $*" >> "$STUB_LOG"\n' > "$BIN/sleep"
printf '#!/usr/bin/env bash\nshift\nexec "$@"\n' > "$BIN/timeout"
chmod +x "$BIN"/*

# run_case <keys> -> sets RC, OUT, LOG, SUMMARY
run_case() {
  export STUB_LOG="$WORKDIR/log" STUB_COUNT="$WORKDIR/count" GITHUB_STEP_SUMMARY="$WORKDIR/summary"
  : > "$STUB_LOG"; : > "$GITHUB_STEP_SUMMARY"; rm -f "$STUB_COUNT"
  OUT="$(PATH="$BIN:$PATH" PRE_PULL_IMAGES="$1" ENV_FILE="$ENV_FILE" bash "$SCRIPT" 2>&1)"
  RC=$?
  LOG="$(cat "$STUB_LOG")"
  SUMMARY="$(cat "$GITHUB_STEP_SUMMARY")"
}
count() { printf '%s\n' "$1" | grep -c -- "$2" || true; }

POSTGRES_REF="$(grep -m1 '^docker\.image\.postgres=' "$ENV_FILE" | cut -d= -f2-)"

echo "mirror succeeds on first try"
STUB_PRESENT=0 STUB_MIRROR_FAILS=0 STUB_HUB_OK=0 run_case "postgres"
check "$RC" "0" "exit 0"
check "$(count "$LOG" 'docker pull mirror.gcr.io/')" "1" "one mirror pull"
check "$(count "$LOG" "docker tag mirror.gcr.io/$POSTGRES_REF $POSTGRES_REF")" "1" "retagged to the exact canonical reference"
check "$(count "$LOG" 'docker pull pgvector')" "0" "no Hub pull"
check "$(count "$OUT" 'pre-pull postgres source=mirror')" "1" "notice says source=mirror"
check "$(count "$SUMMARY" 'pre-pull postgres source=mirror')" "1" "step summary says source=mirror"

echo "mirror fails 3 times, Hub succeeds"
STUB_PRESENT=0 STUB_MIRROR_FAILS=99 STUB_HUB_OK=1 run_case "postgres"
check "$RC" "0" "exit 0"
check "$(count "$LOG" 'docker pull mirror.gcr.io/')" "3" "three mirror attempts"
check "$(count "$LOG" 'sleep 10$')" "1" "sleep 10 after attempt 1"
check "$(count "$LOG" 'sleep 20$')" "1" "sleep 20 after attempt 2"
check "$(count "$LOG" "docker pull $POSTGRES_REF")" "1" "Hub pull of the canonical reference"
check "$(count "$LOG" 'docker tag')" "0" "no tag when the mirror never succeeded"
check "$(count "$OUT" 'source=hub')" "1" "notice says source=hub"

echo "mirror succeeds on the third attempt"
STUB_PRESENT=0 STUB_MIRROR_FAILS=2 STUB_HUB_OK=0 run_case "postgres"
check "$(count "$LOG" 'docker pull mirror.gcr.io/')" "3" "three mirror attempts"
check "$(count "$OUT" 'source=mirror')" "1" "source=mirror"

echo "everything fails"
STUB_PRESENT=0 STUB_MIRROR_FAILS=99 STUB_HUB_OK=0 run_case "postgres search"
check "$RC" "0" "exit code is still 0"
check "$(count "$OUT" 'source=none')" "2" "both keys report source=none"
check "$(count "$LOG" "docker pull $POSTGRES_REF")" "2" "Hub tried twice for postgres"

echo "image already present"
STUB_PRESENT=1 run_case "postgres search search.upgrade"
check "$RC" "0" "exit 0"
check "$(count "$LOG" 'docker pull')" "0" "no pull at all"
check "$(count "$OUT" 'source=present')" "3" "all three report present"

echo "unknown key"
STUB_PRESENT=0 run_case "nope"
check "$RC" "0" "exit 0"
check "$(count "$LOG" 'docker pull')" "0" "no pull"

echo "mirror pull is anonymous"
DOCKER_CONFIG="$WORKDIR/loggedin" STUB_PRESENT=0 STUB_MIRROR_FAILS=99 STUB_HUB_OK=1 run_case "postgres"
mirror_cfg="$(printf '%s\n' "$LOG" | grep 'docker pull mirror.gcr.io/' | sed -E 's/.*DOCKER_CONFIG=([^]]*)\]/\1/' | sort -u)"
hub_cfg="$(printf '%s\n' "$LOG" | grep "docker pull $POSTGRES_REF" | sed -E 's/.*DOCKER_CONFIG=([^]]*)\]/\1/' | sort -u)"
if [[ -n "$mirror_cfg" && "$mirror_cfg" != *"$WORKDIR/loggedin"* && "$mirror_cfg" != *default* ]]; then
  ok "mirror pull uses a fresh DOCKER_CONFIG, not the caller's"
else
  bad "mirror pull uses a fresh DOCKER_CONFIG, not the caller's" "saw '$mirror_cfg'"
fi
check "$hub_cfg" "$WORKDIR/loggedin" "Hub fallback keeps the caller's (logged-in) DOCKER_CONFIG"

echo "environment.properties"
# Comments may mention the mirror; no property value may.
if grep -v '^[[:space:]]*#' "$ENV_FILE" | grep -q 'mirror\.gcr\.io'; then bad "no mirror.gcr.io in environment.properties"; else ok "no mirror.gcr.io in environment.properties"; fi
for k in search search.upgrade postgres; do
  if grep -Eq "^docker\.image\.${k}=[^/]+/[^/]+:[^/]+$" "$ENV_FILE"; then ok "docker.image.$k is a canonical Hub reference"; else bad "docker.image.$k is a canonical Hub reference"; fi
done

echo
echo "passed: $pass  failed: $fail"
[[ "$fail" -eq 0 ]]
