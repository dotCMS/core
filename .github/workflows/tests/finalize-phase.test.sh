#!/usr/bin/env bash
#
# Behaviour tests for the "Prepare workflow data" step in cicd_comp_finalize-phase.yml.
#
# The step is extracted from the workflow itself and executed with the GitHub API (curl)
# stubbed, the way the runner executes a step with no `shell:` (`bash -e`), so the shell that
# ships is the shell under test. Every event value reaches the
# step through the environment, the way the job's `env:` block hands it over on the runner,
# so the extracted text must not carry any `${{ }}` expression: a branch name or a PR title
# is free text chosen by whoever opened the PR, and these cases include values with quotes
# and shell syntax, which must stay text and give a valid report.
#
# Run:  bash .github/workflows/tests/finalize-phase.test.sh
# Deps: bash 4+, jq, awk.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORKFLOW="${1:-$REPO_ROOT/.github/workflows/cicd_comp_finalize-phase.yml}"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if ((BASH_VERSINFO[0] < 4)); then
  echo "ERROR: bash 4+ required (found $BASH_VERSION)." >&2
  exit 2
fi

extract_step() {
  local step_id="$1" dest="$2"
  awk -v want="$step_id" '
    $0 ~ "^[[:space:]]*id:[[:space:]]*" want "[[:space:]]*$" { found = 1; next }
    found && !collecting && /^[[:space:]]*run:[[:space:]]*\|[[:space:]]*$/ {
      match($0, /^[[:space:]]*/); run_indent = RLENGTH
      collecting = 1; next
    }
    collecting {
      if ($0 ~ /^[[:space:]]*$/) { print ""; next }
      match($0, /^[[:space:]]*/)
      if (RLENGTH <= run_indent) exit
      print substr($0, run_indent + 3)
    }
  ' "$WORKFLOW" > "$dest"

  if [[ ! -s "$dest" ]]; then
    echo "ERROR: could not extract step '$step_id' from $WORKFLOW" >&2
    exit 2
  fi
}

extract_step prepare-workflow-data "$WORKDIR/step.sh"

pass=0
fail=0

check() {
  local desc="$1" ok="$2" detail="${3:-}"
  if [[ "$ok" == true ]]; then
    pass=$((pass + 1)); printf '  ok   %s\n' "$desc"
  else
    fail=$((fail + 1)); printf '  FAIL %s\n        %s\n' "$desc" "$detail"
  fi
}

echo "== The step takes its inputs from the environment =="
if grep -q '\${{' "$WORKDIR/step.sh"; then
  check "no Actions expression is pasted into the step" false "$(grep -n '\${{' "$WORKDIR/step.sh")"
else
  check "no Actions expression is pasted into the step" true
fi

# Runs the step in a fresh directory. Event values come from the variables the caller
# exports before calling; FIXTURE_JOBS is the jobs JSON the API stub answers with.
# Leaves the report in $REPORT and the step output file in $OUTPUT.
run_step() {
  local dir="$WORKDIR/run-$RANDOM-$RANDOM"
  mkdir -p "$dir"
  REPORT="$dir/workflow-data.json"
  OUTPUT="$dir/github-output"
  MARKER="$dir/marker"
  : > "$OUTPUT"
  (
    cd "$dir" || exit 9
    export GITHUB_OUTPUT="$OUTPUT" GITHUB_TOKEN="stub-token" MARKER
    export GITHUB_REPOSITORY="dotCMS/core" GITHUB_RUN_ID="41478931577" GITHUB_SHA="0123abcd"
    export BUILD_STEP_REPORTS_DIR="${BUILD_STEP_REPORTS_DIR:-$dir/no-reports}"
    curl() { printf '%s' "${FIXTURE_JOBS:-{\"jobs\":[]\}}"; }
    export -f curl
    bash -e "$WORKDIR/step.sh"
  ) > "$dir/log" 2>&1
  RUN_EXIT=$?
}

# Default PR event; cases override what they need.
pr_event() {
  export GITHUB_EVENT_NAME="pull_request" GITHUB_REF="refs/pull/38247/merge"
  export HEAD_REF="issue-729-workflow-input-env-vars" PR_HEAD_REF="issue-729-workflow-input-env-vars"
  export PR_HEAD_SHA="a073d56e08" PR_HEAD_AUTHOR="rjvelazco" PR_BASE_SHA="5a471b1f58" PR_BASE_AUTHOR="dotcms"
  export PR_ID="3300000001" PR_NUMBER="38247" PR_AUTHOR="rjvelazco" PR_MERGE_STATE="clean"
  export PR_TITLE="fix(ci): read workflow inputs from the environment"
  export NEEDS_DATA='{"build":{"result":"success"},"test":{"result":"success"}}'
  unset FIXTURE_JOBS BUILD_STEP_REPORTS_DIR
}

field() { jq -r "$1" "$REPORT" 2>/dev/null; }

echo "== Pull request event =="
pr_event
run_step
check "the step exits 0" "$([[ $RUN_EXIT -eq 0 ]] && echo true || echo false)" "exit $RUN_EXIT: $(tail -3 "$(dirname "$REPORT")/log")"
check "the report is valid JSON" "$(jq -e . "$REPORT" >/dev/null 2>&1 && echo true || echo false)"
check "branch is the head ref" "$([[ "$(field .branch)" == "issue-729-workflow-input-env-vars" ]] && echo true || echo false)" "got $(field .branch)"
check "head_name is the head ref" "$([[ "$(field .head_name)" == "issue-729-workflow-input-env-vars" ]] && echo true || echo false)"
check "pr fields are carried" "$([[ "$(field '.pr_number + .pr_author + .head_sha + .base_sha + .merge_sha')" == "38247rjvelazcoa073d56e085a471b1f580123abcd" ]] && echo true || echo false)" "got $(field '.pr_number + .pr_author + .head_sha + .base_sha + .merge_sha')"
check "run and repository come from the runner environment" "$([[ "$(field '.run_id + .source_repository + .trigger_event_name')" == "41478931577dotCMS/corepull_request" ]] && echo true || echo false)"
check "no build reports gives an empty list" "$([[ "$(field '.build_reports | length')" == "0" ]] && echo true || echo false)"
check "aggregate status SUCCESS" "$([[ "$(field .aggregate_status)" == "SUCCESS" ]] && echo true || echo false)"
check "no first_fail fields on success" "$([[ "$(field 'has("first_fail_step")')" == "false" ]] && echo true || echo false)"
check "aggregate_status is written to the step output" "$(grep -q '^aggregate_status=SUCCESS$' "$OUTPUT" && echo true || echo false)"

echo "== Event values with quotes and shell syntax are only text =="
pr_event
export HEAD_REF='issue-1-"$(touch "$MARKER")"; `touch "$MARKER"`'
export PR_HEAD_REF="$HEAD_REF"
export PR_TITLE='fix: "quoted" \ backslash $(touch "$MARKER")'
export PR_HEAD_AUTHOR='someone; touch "$MARKER"'
run_step
check "the report is still valid JSON" "$(jq -e . "$REPORT" >/dev/null 2>&1 && echo true || echo false)" "$(head -c 300 "$REPORT")"
check "head_name is the raw branch name" "$([[ "$(field .head_name)" == "$HEAD_REF" ]] && echo true || echo false)" "got $(field .head_name)"
check "branch is the raw branch name after its last slash" "$([[ "$(field .branch)" == "$HEAD_REF" ]] && echo true || echo false)" "got $(field .branch)"
check "pr_title is the raw title" "$([[ "$(field .pr_title)" == "$PR_TITLE" ]] && echo true || echo false)"
check "head_author is the raw login" "$([[ "$(field .head_author)" == "$PR_HEAD_AUTHOR" ]] && echo true || echo false)"
check "no command embedded in an event value ran" "$([[ ! -e "$MARKER" ]] && echo true || echo false)"

echo "== Build reports =="
pr_event
reports="$WORKDIR/reports-$RANDOM"
mkdir -p "$reports/build-reports-core/target" "$reports/build-reports-cli/target"
jq -n '{projectReports: [{name: "dotcms-core", status: "SUCCESS", error: null}]}' > "$reports/build-reports-cli/target/build-report.json"
jq -n '{projectReports: [{name: "dotcms-core", status: "FAILURE", error: "it \"broke\" on line 1; $(touch \"$MARKER\")"}]}' > "$reports/build-reports-core/target/build-report.json"
export BUILD_STEP_REPORTS_DIR="$reports"
run_step
check "the report is valid JSON with the failing error text" "$(jq -e . "$REPORT" >/dev/null 2>&1 && echo true || echo false)" "$(head -c 300 "$REPORT")"
check "both reports are listed with their step name" "$([[ "$(field '[.build_reports[].step_name] | sort | join(",")')" == "cli,core" ]] && echo true || echo false)" "got $(field '[.build_reports[].step_name]')"
check "aggregate status FAILURE" "$([[ "$(field .aggregate_status)" == "FAILURE" ]] && echo true || echo false)"
check "first failing step and module" "$([[ "$(field '.first_fail_step + "/" + .first_fail_module')" == "core/dotcms-core" ]] && echo true || echo false)" "got $(field '.first_fail_step + "/" + .first_fail_module')"
check "first failing error is carried as is" "$([[ "$(field .first_fail_error)" == 'it "broke" on line 1; $(touch "$MARKER")' ]] && echo true || echo false)" "got $(field .first_fail_error)"
check "no command embedded in a report ran" "$([[ ! -e "$MARKER" ]] && echo true || echo false)"
check "aggregate_status=FAILURE is written to the step output" "$(grep -q '^aggregate_status=FAILURE$' "$OUTPUT" && echo true || echo false)"

echo "== Status from the dependencies and from the jobs API =="
pr_event
export NEEDS_DATA='{"build":{"result":"cancelled"}}'
run_step
check "a cancelled dependency gives CANCELLED" "$([[ "$(field .aggregate_status)" == "CANCELLED" ]] && echo true || echo false)" "got $(field .aggregate_status)"
pr_event
export FIXTURE_JOBS='{"jobs":[{"name":"Build","status":"completed","conclusion":"failure"},{"name":"Finalize","status":"completed","conclusion":"failure"}]}'
run_step
check "a failed job reported by the API gives FAILURE" "$([[ "$(field .aggregate_status)" == "FAILURE" ]] && echo true || echo false)" "got $(field .aggregate_status)"
# A failure seen only through the jobs API has no failing module: the fields are carried
# empty, as they always were, and the reporting workflow reads them as such.
check "first_fail fields are present and empty when no build report failed" "$([[ "$(field '.first_fail_step + .first_fail_module + .first_fail_error')" == "" && "$(field 'has("first_fail_step")')" == "true" ]] && echo true || echo false)" "got $(field '[.first_fail_step, .first_fail_module, .first_fail_error]')"

echo "== Push event =="
pr_event
export GITHUB_EVENT_NAME="push" GITHUB_REF="refs/heads/main"
unset HEAD_REF PR_HEAD_REF PR_HEAD_SHA PR_HEAD_AUTHOR PR_BASE_SHA PR_BASE_AUTHOR PR_ID PR_NUMBER PR_AUTHOR PR_MERGE_STATE
export HEAD_REF="" PR_HEAD_REF="" PR_HEAD_SHA="" PR_HEAD_AUTHOR="" PR_BASE_SHA="" PR_BASE_AUTHOR="" PR_ID="" PR_NUMBER="" PR_AUTHOR="" PR_MERGE_STATE=""
run_step
check "branch is the ref after its last slash" "$([[ "$(field .branch)" == "main" ]] && echo true || echo false)" "got $(field .branch)"
check "pr_title is N/A" "$([[ "$(field .pr_title)" == "N/A" ]] && echo true || echo false)"
check "pr fields are empty strings" "$([[ "$(field '.pr_number + .head_name + .head_sha')" == "" ]] && echo true || echo false)"

echo "== Two failing modules in one step, and an error larger than one command-line argument =="
pr_event
reports="$WORKDIR/reports-$RANDOM"
mkdir -p "$reports/build-reports-cli/target" "$reports/build-reports-core/target" "$reports/build-reports-web/target"
long_error="$WORKDIR/long-error-$RANDOM.txt"
yes '[ERROR] a line of a long failure message' | head -c 1200000 > "$long_error"
long_error_length=$(wc -c < "$long_error" | tr -d ' ')
jq -n '{projectReports: [{name: "dotcms-cli", status: "SUCCESS", error: null}]}' > "$reports/build-reports-cli/target/build-report.json"
jq -n --rawfile e "$long_error" '{projectReports: [{name: "dotcms-core", status: "FAILURE", error: $e}, {name: "dotcms-integration", status: "FAILURE", error: "the later failure"}]}' > "$reports/build-reports-core/target/build-report.json"
jq -n '{projectReports: [{name: "dotcms-core-web", status: "FAILURE", error: "a failure in a later step"}]}' > "$reports/build-reports-web/target/build-report.json"
export BUILD_STEP_REPORTS_DIR="$reports"
run_step
check "the step exits 0" "$([[ $RUN_EXIT -eq 0 ]] && echo true || echo false)" "exit $RUN_EXIT: $(tail -2 "$(dirname "$REPORT")/log")"
check "the first failing module of the first failing step is named" "$([[ "$(field '.first_fail_step + "/" + .first_fail_module')" == "core/dotcms-core" ]] && echo true || echo false)" "got $(field '.first_fail_step + "/" + .first_fail_module')"
check "the whole error text is carried" "$([[ "$(field '.first_fail_error | length')" == "$long_error_length" ]] && echo true || echo false)" "got $(field '.first_fail_error | length') of $long_error_length"
check "all three reports are listed in name order" "$([[ "$(field '[.build_reports[].step_name] | join(",")')" == "cli,core,web" ]] && echo true || echo false)" "got $(field '[.build_reports[].step_name]')"

echo "== A build report that is not JSON =="
pr_event
reports="$WORKDIR/reports-$RANDOM"
mkdir -p "$reports/build-reports-core/target"
printf 'not json' > "$reports/build-reports-core/target/build-report.json"
export BUILD_STEP_REPORTS_DIR="$reports"
run_step
check "the step fails" "$([[ $RUN_EXIT -ne 0 ]] && echo true || echo false)" "exit $RUN_EXIT"
check "no report and no working file is left in the workspace" "$([[ ! -e "$REPORT" ]] && ! compgen -G "$(dirname "$REPORT")/workflow-*.json" >/dev/null && echo true || echo false)" "left: $(ls "$(dirname "$REPORT")" | tr '\n' ' ')"

echo
echo "pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
