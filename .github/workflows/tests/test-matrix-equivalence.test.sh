#!/usr/bin/env bash
#
# Equivalence tests for moving "Setup Test Matrix" out of cicd_comp_test-phase.yml
# into cicd_comp_test-matrix.yml, so it runs in parallel with the build instead of
# between the build and the tests.
#
# Three kinds of assertion:
#
#   * Matrix equivalence - the real github-script body is pulled out of the workflow
#     YAML before the move (read from git at BASE_REF) and after it, run under node
#     against .github/test-matrix.yml for several input combinations, and the two
#     `matrix` / `has-tests` outputs must be byte-identical. Nothing is copied into
#     this file. When BASE_REF is not in the local history (a shallow clone) this
#     section is skipped, loudly.
#
#   * Input equivalence - for every caller, the test-selection inputs now passed to
#     the test-matrix job are the same keys with the same expressions the caller used
#     to pass to the test phase, so each caller still selects the same tests.
#
#   * Critical path - the matrix job must not wait for the build, and the test job
#     must still wait for both the build and the matrix.
#
#   * Full runs (#38336) - the nightly and a trunk dispatch with run-all-tests run every
#     non-e2e suite in test-matrix.yml. The expected suite list is read straight from
#     test-matrix.yml and the selector values straight from the callers' YAML; a trunk
#     push still tests nothing. Those two callers are the only ones whose selection
#     changed, so they are left out of the input-equivalence check.
#
#   * Fail loud (#38336) - run-all-tests with an empty matrix fails the matrix job, and
#     no legitimate empty matrix (a PR with nothing selected, a trunk push, phase none)
#     trips it.
#
# Run:  bash .github/workflows/tests/test-matrix-equivalence.test.sh
# Deps: bash 3.2+, git, node, mikefarah yq v4, jq. Run in CI by cicd_pr_workflow-lint.yml.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

# Last commit on which cicd_comp_test-phase.yml still held the setup-matrix job.
BASE_REF=307b851d9c

OLD_PHASE_PATH=.github/workflows/cicd_comp_test-phase.yml
NEW_PHASE="$REPO_ROOT/.github/workflows/cicd_comp_test-phase.yml"
NEW_MATRIX="$REPO_ROOT/.github/workflows/cicd_comp_test-matrix.yml"
CONFIG_YML="$REPO_ROOT/.github/test-matrix.yml"
CALLERS="cicd_1-pr cicd_2-merge-queue cicd_3-trunk cicd_4-nightly cicd_5-lts cicd_scheduled_opensearch-phase-sweep"

pass=0
fail=0
skipped=0
ok()   { pass=$((pass + 1)); printf '  ok   %s\n' "$1"; }
bad()  { fail=$((fail + 1)); printf '  FAIL %s\n' "$1"; [[ -n "${2:-}" ]] && printf '         -> %s\n' "$2"; return 0; }
skip() { skipped=$((skipped + 1)); printf '  SKIP %s\n' "$1"; }

for tool in git node yq jq; do
  command -v "$tool" >/dev/null 2>&1 || { echo "required tool missing: $tool"; exit 2; }
done

# The github-script body of the generate-matrix step, from stdin (a workflow file).
matrix_script() { yq '.jobs.setup-matrix.steps[] | select(.id == "generate-matrix") | .with.script'; }

# Run a github-script body the way the action does: `core` and `require` in scope, the
# ${{ inputs.* }} placeholders substituted. $1 = script file, $2 = config.json,
# $3 = space separated name=value pairs, $4 = opensearch phase. Prints the two outputs,
# or a single `failed: <message>` line when the script called core.setFailed.
run_script() {
  local script="$1" config="$2" pairs="$3" phase="$4" body="$WORKDIR/body.js" name value
  cp "$script" "$body"
  # Every input the script reads; one the case does not name keeps its workflow default, false.
  for name in run-all-tests jvm_unit_test cli frontend postman karate integration e2e skip-runner-cleanup; do
    value=false
    case " $pairs " in *" $name=true "*) value=true ;; esac
    sed -i.bak "s/\${{ inputs\.$name }}/$value/" "$body"
  done
  (cd "$(dirname "$config")" && OPENSEARCH_PHASE="$phase" node -e '
    const fs = require("fs");
    const out = {};
    let failed = null;
    const core = { setOutput: (k, v) => { out[k] = v; }, setFailed: m => { failed = m; } };
    console.log = () => {};
    const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
    new AsyncFunction("core", "require", fs.readFileSync(process.argv[1], "utf8"))(core, require)
      .then(() => process.stdout.write(failed !== null
        ? "failed: " + failed + "\n"
        : out.matrix + "\n" + out["has-tests"] + "\n"))
      .catch(e => { console.error(e); process.exit(1); });
  ' "$body")
}

# ------------------------------------------------------------ matrix equivalence --

echo "== Matrix output is byte-identical before and after the move"

yq -o=json "$CONFIG_YML" > "$WORKDIR/config.json"
matrix_script < "$NEW_MATRIX" > "$WORKDIR/new.js"

if ! git -C "$REPO_ROOT" cat-file -e "$BASE_REF:$OLD_PHASE_PATH" 2>/dev/null; then
  skip "BASE_REF $BASE_REF not in local history (shallow clone): matrix equivalence not checked"
else
  git -C "$REPO_ROOT" show "$BASE_REF:$OLD_PHASE_PATH" | matrix_script > "$WORKDIR/old.js"

  # name | inputs (name=value, space separated) | opensearch phase
  while IFS='|' read -r name pairs phase; do
    old="$(run_script "$WORKDIR/old.js" "$WORKDIR/config.json" "$pairs" "$phase")"
    new="$(run_script "$WORKDIR/new.js" "$WORKDIR/config.json" "$pairs" "$phase")"
    if ! sed -n 1p <<<"$old" | jq -e '.include | type == "array"' >/dev/null 2>&1; then
      bad "$name" "old script did not produce a matrix"
    elif [[ "$old" == "$new" ]]; then
      ok "$name ($(sed -n 2p <<<"$old" | tr -d '\n'), $(sed -n 1p <<<"$old" | jq '.include | length') entries)"
    else
      bad "$name" "old and new matrix differ"
    fi
  done <<'CASES'
run-all-tests with every selector on, cleanup kept|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=true skip-runner-cleanup=false|
run-all-tests with every selector on, cleanup skippable|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=true skip-runner-cleanup=true|
PR with frontend only|jvm_unit_test=false cli=false frontend=true postman=false karate=false integration=false e2e=false skip-runner-cleanup=true|
PR with jvm and cli only|jvm_unit_test=true cli=true frontend=false postman=false karate=false integration=false e2e=false skip-runner-cleanup=true|
PR with backend changes (full PR set)|jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=true skip-runner-cleanup=true|
merge queue|jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=false skip-runner-cleanup=true|
nothing enabled (trunk push)|run-all-tests=false e2e=false skip-runner-cleanup=true|
run-all-tests but integration explicitly off|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=false e2e=false skip-runner-cleanup=false|
opensearch phase all (sweep)|run-all-tests=false integration=true e2e=false skip-runner-cleanup=false|all
opensearch phase 2 on a PR|jvm_unit_test=true integration=true postman=true karate=true cli=true frontend=true e2e=false skip-runner-cleanup=true|2
opensearch phase comma list|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=false skip-runner-cleanup=false|1,3
opensearch phase none|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=false skip-runner-cleanup=false|none
opensearch phase zero|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=false skip-runner-cleanup=false|0
opensearch phase garbage|run-all-tests=true jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=false skip-runner-cleanup=false|banana
CASES

  # Guard against a harness that agrees with itself by producing nothing: the full
  # PR case must carry the integration suites and no OS-phase suffix.
  probe="$(run_script "$WORKDIR/new.js" "$WORKDIR/config.json" \
    "jvm_unit_test=true cli=true frontend=true postman=true karate=true integration=true e2e=true skip-runner-cleanup=true" "")"
  if sed -n 1p <<<"$probe" | jq -e '[.include[].name] | any(startswith("Integration Tests")) and (any(test("OS Phase")) | not)' >/dev/null 2>&1; then
    ok "harness probe: full PR matrix has integration suites and no OS phase fan-out"
  else
    bad "harness probe: full PR matrix is not shaped as expected"
  fi
  probe="$(run_script "$WORKDIR/new.js" "$WORKDIR/config.json" "run-all-tests=false integration=true e2e=false skip-runner-cleanup=false" "all")"
  if sed -n 1p <<<"$probe" | jq -e '[.include[].name] | any(test("\\[OS Phase 3\\]"))' >/dev/null 2>&1; then
    ok "harness probe: phase 'all' fans the integration suites out per phase"
  else
    bad "harness probe: phase 'all' did not fan out"
  fi
fi

# ------------------------------------------------------------ input equivalence --

echo "== Every caller selects the same tests as before"

SELECTION='^(run-all-tests|jvm_unit_test|cli|frontend|postman|karate|integration|e2e|opensearch_phase|skip-runner-cleanup)$'
selection_of() { yq -o=json -I0 "(.jobs.$2.with // {}) | to_entries | map(select(.key | test(\"$SELECTION\"))) | sort_by(.key)" "$1"; }

for caller in $CALLERS; do
  file=".github/workflows/$caller.yml"
  case "$caller" in
    cicd_3-trunk|cicd_4-nightly)
      # Changed on purpose (#38336): they now pass every selector. Checked under full runs.
      continue ;;
  esac
  if ! git -C "$REPO_ROOT" cat-file -e "$BASE_REF:$file" 2>/dev/null; then
    skip "$caller: BASE_REF $BASE_REF not in local history"
    continue
  fi
  git -C "$REPO_ROOT" show "$BASE_REF:$file" > "$WORKDIR/old-caller.yml"
  old="$(selection_of "$WORKDIR/old-caller.yml" test)"
  new="$(selection_of "$REPO_ROOT/$file" test-matrix)"
  # opensearch_phase is now derived once, in the matrix job, and read back from its outputs.
  rest="$(selection_of "$REPO_ROOT/$file" test)"
  rest_keys="$(jq -r '[.[].key] | join(",")' <<<"$rest")"
  if [[ "$old" == "$new" && "$old" != "[]" ]]; then
    ok "$caller: matrix job gets the selection inputs the test job used to get"
  else
    bad "$caller: selection inputs differ" "old: $old / new: $new"
  fi
  if [[ "$rest_keys" == "opensearch_phase" ]]; then
    ok "$caller: test job only forwards opensearch_phase from the matrix job"
  else
    bad "$caller: test job still passes selection inputs ($rest_keys)"
  fi
done

# ------------------------------------------------------------------- full runs --

echo "== Nightly and trunk dispatch run every non-e2e suite (#38336)"

# The selector values a caller passes to its test-matrix job, as name=value pairs.
# Literal booleans are taken as is; `${{ inputs.run-all-tests || X }}` is evaluated with
# $2 as the run-all-tests dispatch input (empty = not dispatched: a push or the schedule).
caller_pairs() {
  local run_all="$2" key value pairs=""
  while IFS='=' read -r key value; do
    case "$value" in
      true|false) ;;
      '${{ inputs.run-all-tests || true }}') value=true ;;
      '${{ inputs.run-all-tests || false }}') [[ "$run_all" == true ]] && value=true || value=false ;;
      *) continue ;;   # opensearch_phase: not a selector
    esac
    pairs="$pairs $key=$value"
  done < <(yq '.jobs.test-matrix.with | to_entries | .[] | .key + "=" + (.value | tostring)' "$1")
  printf '%s' "${pairs# }"
}

# Every suite name of every test type except e2e, straight from test-matrix.yml.
expected_full="$(yq '.test_types[] | select(.condition_input != "e2e") | .suites[].name' "$CONFIG_YML" | sort)"
expected_count="$(grep -c . <<<"$expected_full")"

full_run() {
  local label="$1" file="$REPO_ROOT/.github/workflows/$2.yml" pairs out got
  pairs="$(caller_pairs "$file" "$3")"
  out="$(run_script "$WORKDIR/new.js" "$WORKDIR/config.json" "$pairs" "")"
  got="$(sed -n 1p <<<"$out" | jq -r '.include[].name' 2>/dev/null | sort)"
  if [[ -n "$got" && "$got" == "$expected_full" ]]; then
    ok "$label: all $expected_count non-e2e suites ($pairs)"
  else
    bad "$label: matrix is not every non-e2e suite" \
      "missing [$(comm -23 <(echo "$expected_full") <(echo "$got") | paste -sd, -)] unexpected [$(comm -13 <(echo "$expected_full") <(echo "$got") | paste -sd, -)] ${out:0:200}"
  fi
}

full_run "nightly (schedule)" cicd_4-nightly ""
full_run "nightly (dispatch)" cicd_4-nightly true
full_run "trunk dispatch with run-all-tests" cicd_3-trunk true

EMPTY=$'{"include":[]}\nfalse'
pairs="$(caller_pairs "$REPO_ROOT/.github/workflows/cicd_3-trunk.yml" "")"
out="$(run_script "$WORKDIR/new.js" "$WORKDIR/config.json" "$pairs" "")"
[[ "$out" == "$EMPTY" ]] \
  && ok "trunk push: empty matrix, has-tests false, not failed ($pairs)" \
  || bad "trunk push did not produce a quiet empty matrix" "$out"

# ------------------------------------------------------------------- fail loud --

echo "== run-all-tests with an empty matrix fails; legitimate empty matrices do not"

# name | inputs | opensearch phase | expect (fail or quiet-empty)
while IFS='|' read -r name pairs phase expect; do
  out="$(run_script "$WORKDIR/new.js" "$WORKDIR/config.json" "$pairs" "$phase")"
  if [[ "$expect" == fail ]]; then
    [[ "$out" == "failed: run-all-tests is true but the test matrix is empty."* ]] \
      && ok "$name: fails with a clear error" \
      || bad "$name: did not fail" "$out"
  else
    [[ "$out" == "$EMPTY" ]] \
      && ok "$name: empty matrix, not failed" \
      || bad "$name: expected a quiet empty matrix" "$out"
  fi
done <<'CASES'
run-all-tests alone (the nightly call before #38336)|run-all-tests=true e2e=false skip-runner-cleanup=false||fail
run-all-tests alone (the trunk dispatch call before #38336)|run-all-tests=true e2e=false skip-runner-cleanup=true||fail
run-all-tests alone under a phase sweep|run-all-tests=true e2e=false|all|fail
PR with no tests selected|jvm_unit_test=false cli=false frontend=false postman=false karate=false integration=false e2e=false skip-runner-cleanup=true||quiet-empty
PR with no tests selected, OpenSearch phase none|jvm_unit_test=false cli=false frontend=false postman=false karate=false integration=false e2e=false skip-runner-cleanup=true|none|quiet-empty
trunk push|run-all-tests=false e2e=false skip-runner-cleanup=true||quiet-empty
CASES

# --------------------------------------------------------------- critical path --

echo "== The matrix is off the critical path, the tests are not"

for caller in $CALLERS; do
  file="$REPO_ROOT/.github/workflows/$caller.yml"
  uses="$(yq '.jobs.test-matrix.uses' "$file")"
  [[ "$uses" == "./.github/workflows/cicd_comp_test-matrix.yml" ]] \
    && ok "$caller: test-matrix job calls cicd_comp_test-matrix.yml" \
    || bad "$caller: test-matrix job uses '$uses'"

  mneeds="$(yq -o=json -I0 '.jobs.test-matrix.needs' "$file")"
  jq -e 'index("build") | not' <<<"$mneeds" >/dev/null \
    && ok "$caller: test-matrix does not wait for the build ($mneeds)" \
    || bad "$caller: test-matrix waits for the build" "$mneeds"

  tneeds="$(yq -o=json -I0 '.jobs.test.needs' "$file")"
  jq -e 'index("build") and index("test-matrix")' <<<"$tneeds" >/dev/null \
    && ok "$caller: test waits for the build and the matrix ($tneeds)" \
    || bad "$caller: test needs are $tneeds"

  yq -e '.jobs.test.with.matrix == "${{ needs.test-matrix.outputs.matrix }}" and .jobs.test.with.has-tests == "${{ needs.test-matrix.outputs.has-tests }}"' "$file" >/dev/null \
    && ok "$caller: test passes the matrix and has-tests through" \
    || bad "$caller: test does not pass matrix/has-tests from test-matrix"

  # A failed matrix job skips `test`, which finalize alone would not read as a failure,
  # so finalize has to see the matrix job itself.
  fneeds="$(yq -o=json -I0 '.jobs.finalize.needs' "$file")"
  jq -e 'index("test-matrix")' <<<"$fneeds" >/dev/null \
    && ok "$caller: finalize sees the matrix job ($fneeds)" \
    || bad "$caller: finalize does not need test-matrix" "$fneeds"
done

yq -e '.jobs | has("setup-matrix") | not' "$NEW_PHASE" >/dev/null \
  && ok "test phase no longer holds a setup-matrix job" \
  || bad "test phase still has a setup-matrix job"

echo
printf '%d passed, %d failed, %d skipped\n' "$pass" "$fail" "$skipped"
[[ "$fail" -eq 0 ]]
