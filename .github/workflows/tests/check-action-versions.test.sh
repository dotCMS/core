#!/usr/bin/env bash
#
# Behaviour tests for .github/scripts/check-action-versions.sh — the action-version
# drift guard added for #36850.
#
# Each case builds a throwaway tree of tiny workflow/action files and runs the guard
# against it, asserting the exit code and the report text. Fixtures use REAL action
# names because the guard's manifest is keyed on them.
#
# Run:  bash .github/workflows/tests/check-action-versions.test.sh
# Deps: bash 3.2+ (kept portable so it runs on stock macOS), grep, sed.
#
# Why this exists: .github/filters.yaml routes ~38 of the 51 files #36850 touches to
# no build, test or lint at all. This test plus the guard it covers are the only
# automated verification those files get. See specs/36850-upgrade-github-actions-node-24/
# contracts/check-action-versions.md for the contract these cases encode.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
GUARD="${1:-$REPO_ROOT/.github/scripts/check-action-versions.sh}"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if [[ ! -x "$GUARD" ]]; then
  echo "ERROR: guard not found or not executable: $GUARD" >&2
  exit 2
fi

CHECKOUT_SHA=3d3c42e5aac5ba805825da76410c181273ba90b1
CACHE_SHA=55cc8345863c7cc4c66a329aec7e433d2d1c52a9

pass=0
fail=0

# Writes $2 as .github/workflows/$1 inside a FRESH fixture root and echoes the root.
#
# The root must come from mktemp, not an incrementing counter: this function is
# always called via $(...), which runs in a subshell, so any counter it bumped
# would be lost and every case would share one directory — leaking fixtures
# between cases and masking real failures.
fixture() {
  local root; root="$(mktemp -d "$WORKDIR/caseXXXXXX")"
  mkdir -p "$root/.github/workflows"
  printf '%s\n' "$2" > "$root/.github/workflows/$1"
  echo "$root"
}

report() {
  local desc="$1" ok="$2" detail="${3:-}"
  if [[ "$ok" == "yes" ]]; then
    pass=$((pass + 1)); printf '  ok   %s\n' "$desc"
  else
    fail=$((fail + 1)); printf '  FAIL %s\n%s' "$desc" "${detail:+        $detail
}"
  fi
}

# expect_exit <desc> <expected-code> <root-or-args...>
expect_exit() {
  local desc="$1" want="$2"; shift 2
  local out; out="$("$GUARD" "$@" 2>&1)"; local got=$?
  if [[ "$got" == "$want" ]]; then
    report "$desc" yes
  else
    report "$desc" no "want exit $want, got $got
        output: $(printf '%s' "$out" | head -3 | tr '\n' '|')"
  fi
}

# expect_match <desc> <regex> <args...>  — guard must fail AND its report must match
expect_match() {
  local desc="$1" re="$2"; shift 2
  local out; out="$("$GUARD" "$@" 2>&1)"; local code=$?
  if [[ $code -eq 1 ]] && printf '%s' "$out" | grep -qE "$re"; then
    report "$desc" yes
  else
    report "$desc" no "exit=$code, expected 1 and /$re/
        output: $(printf '%s' "$out" | head -4 | tr '\n' '|')"
  fi
}

echo "== A1: version floor =="

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v4")
expect_match "below-floor checkout@v4 is a violation, with file:line" \
  '\.github/workflows/wf\.yml:4:.*checkout' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v7.0.1")
expect_exit "at-floor checkout@v7.0.1 passes" 0 "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v8")
expect_exit "ABOVE-floor checkout@v8 passes (floor, not equality)" 0 "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/setup-node@v2-beta")
expect_match "non-numeric tag @v2-beta is a violation" \
  'setup-node' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/cache/restore@v4
      - uses: actions/cache/save@v4")
expect_match "cache sub-path actions are governed independently" \
  'cache/(restore|save)' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/upload-artifact@v4
      - uses: pnpm/action-setup@v4")
expect_match "upload-artifact@v4 and pnpm/action-setup@v4 are violations" \
  'upload-artifact' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/cache/restore@v3")
expect_match "cache/restore@v3 (dead v1 cache service) is a violation" \
  'cache/restore@v3' "$r"

echo "== A2: SHA pin integrity =="

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@$CHECKOUT_SHA # v7.0.1")
expect_exit "correct SHA with correct comment passes" 0 "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@$CHECKOUT_SHA # v4.2.0")
expect_match "correct SHA with STALE comment is a violation" \
  'comment' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@$CHECKOUT_SHA")
expect_match "correct SHA with NO comment is a violation" \
  'comment' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@8ade135a41bc03ea155e62e844d188df1ea18608 # v7.0.1")
expect_match "unknown SHA is a violation even with a plausible comment" \
  'checkout' "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/cache@0057852bfaa89a56745cba8c7296529d2fc39830 # v4.3.0")
expect_match "the maven-job cache SHA pin is caught by SHA, not by comment" \
  'SHA-pinned to 0057852bfaa8' "$r"

echo "== A3: required inputs are explicit =="

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/download-artifact@v8.0.1
        with:
          name: maven-repo")
expect_match "download-artifact without digest-mismatch is a violation" \
  "digest-mismatch" "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/download-artifact@v8.0.1
        with:
          name: maven-repo
          digest-mismatch: warn")
expect_exit "download-artifact with digest-mismatch: warn passes" 0 "$r"

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/download-artifact@v8.0.1
        with:
          digest-mismatch: error")
expect_exit "digest-mismatch: error also satisfies A3" 0 "$r"

# Regression: in composite actions and in steps that lead with `- name:`, `uses:`
# and `with:` are SIBLING keys at the same indentation. An earlier A3 lookahead
# stopped at the first line whose indent was <= the `uses:` indent, so it never
# looked inside `with:` and reported every such site as missing the input.
r=$(fixture wf.yml "runs:
  using: composite
  steps:
    - name: Download
      uses: actions/download-artifact@v8.0.1
      with:
        digest-mismatch: warn
        name: maven-repo")
expect_exit "A3 sees into with: when uses:/with: are siblings (composite form)" 0 "$r"

r=$(fixture wf.yml "runs:
  using: composite
  steps:
    - name: Download
      uses: actions/download-artifact@v8.0.1
      with:
        name: maven-repo")
expect_match "...and still flags the sibling form when the input is absent" \
  'digest-mismatch' "$r"

# An `if:` may sit between `uses:` and `with:`.
r=$(fixture wf.yml "runs:
  using: composite
  steps:
    - name: Download
      uses: actions/download-artifact@v8.0.1
      if: inputs.build_run_id
      with:
        digest-mismatch: warn
        name: ctx")
expect_exit "A3 tolerates an if: between uses: and with:" 0 "$r"

# The input must belong to THIS step, not a later one.
r=$(fixture wf.yml "runs:
  using: composite
  steps:
    - name: Download
      uses: actions/download-artifact@v8.0.1
      with:
        name: maven-repo
    - name: Other
      uses: actions/upload-artifact@v7.0.1
      with:
        digest-mismatch: warn")
expect_match "A3 does not borrow the input from a later step" \
  'digest-mismatch' "$r"

echo "== Scope: actions outside the manifest are not policed =="

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: docker/build-push-action@v6.15.0
      - uses: actions/setup-python@v4
      - uses: aws-actions/configure-aws-credentials@v1")
expect_exit "out-of-manifest actions pass (deferred to #37194)" 0 "$r"

echo "== Docs mode =="

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v7.0.1")
mkdir -p "$r/.github/actions/demo"
printf -- '- uses: actions/checkout@v2\n' > "$r/.github/actions/demo/README.md"
expect_exit "stale README ref is ignored without --include-docs" 0 "$r"
expect_match "stale README ref is caught with --include-docs" \
  'README\.md' --include-docs "$r"

echo "== Reporting =="

r=$(fixture aaa.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v4")
mkdir -p "$r/.github/workflows"
printf 'jobs:\n  b:\n    steps:\n      - uses: actions/upload-artifact@v4\n' > "$r/.github/workflows/zzz.yml"
out="$("$GUARD" "$r" 2>&1)"
first="$(printf '%s' "$out" | grep -oE '(aaa|zzz)\.yml' | head -1)"
n="$(printf '%s' "$out" | grep -cE '^.*\.yml:[0-9]+:')"
if [[ "$first" == "aaa.yml" && "$n" -eq 2 ]]; then
  report "two violations in different files, ordered by path" yes
else
  report "two violations in different files, ordered by path" no \
    "first=$first count=$n (want aaa.yml / 2)"
fi

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v4")
out="$("$GUARD" --format github "$r" 2>&1)"
if printf '%s' "$out" | grep -qE '^::error file=[^,]+,line=[0-9]+::'; then
  report "--format github emits ::error annotations" yes
else
  report "--format github emits ::error annotations" no "output: $(printf '%s' "$out" | head -2 | tr '\n' '|')"
fi

echo "== Usage and manifest errors exit 2, never 0 =="

r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v7.0.1")
expect_exit "unknown flag exits 2" 2 --not-a-flag "$r"
expect_exit "unreadable path exits 2" 2 "$WORKDIR/does-not-exist"

bad="$WORKDIR/bad-manifest.tsv"
printf 'actions/checkout\tNOT-A-NUMBER\tv7.0.1\t%s\t\n' "$CHECKOUT_SHA" > "$bad"
out="$(CHECK_ACTION_VERSIONS_MANIFEST="$bad" "$GUARD" "$r" 2>&1)"; code=$?
if [[ $code -eq 2 ]]; then
  report "malformed manifest exits 2 (never 0)" yes
else
  report "malformed manifest exits 2 (never 0)" no "exit=$code output: $(printf '%s' "$out" | head -2 | tr '\n' '|')"
fi

echo "== Clean tree =="
r=$(fixture wf.yml "jobs:
  a:
    steps:
      - uses: actions/checkout@v7.0.1
      - uses: actions/cache@$CACHE_SHA # v6.1.0
      - uses: actions/upload-artifact@v7.0.1
      - uses: pnpm/action-setup@v6.0.10
      - uses: actions/setup-node@v7.0.0
      - uses: actions/github-script@v8
      - uses: dorny/paths-filter@v4.0.3
      - uses: docker/login-action@v4.6.0
      - uses: dawidd6/action-download-artifact@v24")
expect_exit "a fully-swept tree passes" 0 "$r"

echo
echo "pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
