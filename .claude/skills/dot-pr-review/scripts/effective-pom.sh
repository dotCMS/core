#!/usr/bin/env bash
# Usage: effective-pom.sh --root <CORE_REPO> --head <HEAD_DIR> --base-sha <BASE_SHA> --run-dir <RUN_DIR>
#                         --modules <m1,m2> --changed <changed-files.txt> --out <bundle> [--skip <reason>]
#
# Resolves the effective POM of the changed Maven modules at the head and at the base, offline, and
# writes their unified diff to --out (Stage 4, only when RUN_MAVEN). The maven reviewer reads it to
# judge inherited configuration by what Maven resolves, not by the POM text.
#   --root <dir>       the core checkout (CORE_REPO); the base worktree is added from it
#   --head <dir>       the PR head (HEAD_DIR), or the working tree in local mode
#   --base-sha <sha>   the base of the diff, checked out detached at <run-dir>/base
#   --run-dir <dir>    the run directory; the resolved POMs and Maven's logs go to <run-dir>/maven/
#   --modules <list>   comma-separated module directories for -pl (`.` for the root POM); a run
#                      that --changed skips needs none
#   --changed <file>   changed-files.txt. A change to mvnw, mvnw.cmd or .mvn/** skips the run:
#                      resolving the head would run the PR's own build configuration
#   --out <file>       the bundle: the diff, or the single line `NOT COMPUTED: <reason>`
#   --skip <reason>    write `NOT COMPUTED: <reason>` without running Maven (a PR from a fork)
#
# Maven runs offline (-o): only artifacts already in ~/.m2 can load, maven-help-plugin at the
# version core's parent pins among them, and a resolve takes seconds.
# Each side's own path is replaced by <root> before the diff, so only configuration differences show.
# Prints `OK lines=<n>` (n changed lines; 0 when both sides resolve the same) or
# `NOT_COMPUTED <reason>`. Exit codes: 0 in both cases (a missing effective POM is reported in the
# bundle, not fatal), 2 usage error.
# Stage 10 removes the base worktree: git -C <root> worktree remove --force <run-dir>/base

set -euo pipefail

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0" >&2
  exit "${1:-2}"
}

ROOT="" HEAD_DIR="" BASE_SHA="" RUN="" MODULES="" CHANGED="" OUT="" SKIP=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="${2:-}"; shift 2 ;;
    --head) HEAD_DIR="${2:-}"; shift 2 ;;
    --base-sha) BASE_SHA="${2:-}"; shift 2 ;;
    --run-dir) RUN="${2:-}"; shift 2 ;;
    --modules) MODULES="${2:-}"; shift 2 ;;
    --changed) CHANGED="${2:-}"; shift 2 ;;
    --out) OUT="${2:-}"; shift 2 ;;
    --skip) SKIP="${2:-}"; shift 2 ;;
    -h|--help) usage 0 ;;
    *) echo "ERROR: unknown argument $1" >&2; usage ;;
  esac
done
[[ -n "$OUT" ]] || { echo "ERROR: --out is required" >&2; usage; }
mkdir -p "$(dirname "$OUT")"

not_computed() {
  printf 'NOT COMPUTED: %s\n' "$1" > "$OUT"
  echo "NOT_COMPUTED $1"
  exit 0
}

[[ -z "$SKIP" ]] || not_computed "$SKIP"
# the wrapper guard runs before the other flags are required: a PR that changes only mvnw or .mvn/
# has no changed pom.xml, so it arrives without --modules
[[ -n "$CHANGED" && -r "$CHANGED" ]] || { echo "ERROR: --changed must name a readable file" >&2; usage; }
if grep -Eq '^(mvnw|mvnw\.cmd|\.mvn/)' "$CHANGED"; then
  not_computed "the change touches mvnw or .mvn/, so resolving it would run its own build configuration"
fi
[[ -n "$ROOT" && -n "$HEAD_DIR" && -n "$BASE_SHA" && -n "$RUN" && -n "$MODULES" ]] \
  || { echo "ERROR: --root, --head, --base-sha, --run-dir and --modules are required" >&2; usage; }

mkdir -p "$RUN/maven"
BASE_DIR="$RUN/base"
if [[ ! -d "$BASE_DIR" ]]; then
  git -C "$ROOT" worktree add --detach "$BASE_DIR" "$BASE_SHA" > "$RUN/maven/worktree.log" 2>&1 \
    || not_computed "cannot check out the base: $(tail -n 1 "$RUN/maven/worktree.log")"
fi

# replace every occurrence of a literal path with <root>
strip_path() {
  awk -v p="$2" -v q="$3" '{
    while ((i = index($0, p)) > 0) $0 = substr($0, 1, i - 1) "<root>" substr($0, i + length(p))
    while (q != "" && (i = index($0, q)) > 0) $0 = substr($0, 1, i - 1) "<root>" substr($0, i + length(q))
    print
  }' "$1"
}

resolve() {
  local side="$1" label="$2" physical
  [[ -x "$side/mvnw" ]] || not_computed "no mvnw at the $label"
  # run from inside the checkout: core's build-cache extension writes target/ into the current directory.
  # The plugin is named in full, so Maven takes its version from core's pluginManagement and a ~/.m2
  # without it fails naming maven-help-plugin, not the `help` prefix.
  if ! (cd "$side" && ./mvnw -q -o -f pom.xml org.apache.maven.plugins:maven-help-plugin:effective-pom -pl "$MODULES" \
      -Doutput="$RUN/maven/$label.raw.xml") > "$RUN/maven/$label.log" 2>&1; then
    not_computed "the $label does not resolve offline: $(grep -m 1 -F '[ERROR]' "$RUN/maven/$label.log" || tail -n 1 "$RUN/maven/$label.log")"
  fi
  physical="$(cd "$side" && pwd -P)"
  # longer path first, so a path that contains the other is replaced whole
  if (( ${#physical} >= ${#side} )); then
    strip_path "$RUN/maven/$label.raw.xml" "$physical" "$side" > "$RUN/maven/$label.xml"
  else
    strip_path "$RUN/maven/$label.raw.xml" "$side" "$physical" > "$RUN/maven/$label.xml"
  fi
}

resolve "$HEAD_DIR" head
resolve "$BASE_DIR" base

set +e
diff -u --label "base $BASE_SHA" --label head "$RUN/maven/base.xml" "$RUN/maven/head.xml" > "$OUT"
rc=$?
set -e
(( rc <= 1 )) || not_computed "diff failed"
echo "OK lines=$(grep -c -E '^[-+][^-+]|^[-+]$' "$OUT" || true)"
