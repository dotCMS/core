#!/usr/bin/env bash
#
# Behaviour tests for the "Get Workflow Data" and "Prepare Slack Message Payload" steps in
# cicd_post-workflow-reporting.yml.
#
# Both steps are extracted from the workflow itself and executed the way the runner executes a
# step with no `shell:` (`bash -e`), so the shell that ships is the shell under test. The first
# step reads workflow-data.json (the report cicd_comp_finalize-phase.yml writes) and the
# second takes that step's outputs through `env:`, so neither may carry a `${{ }}` expression:
# the title, branch name, authors and error text in the report are free text from the
# reported run, and these cases carry quotes, backslashes, shell syntax and line breaks.
#
# Run:  bash .github/workflows/tests/post-workflow-reporting.test.sh
# Deps: bash 4+, jq, awk.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORKFLOW="${1:-$REPO_ROOT/.github/workflows/cicd_post-workflow-reporting.yml}"
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

extract_step workflow-data "$WORKDIR/step_data.sh"
extract_step prepare-slack-message "$WORKDIR/step_payload.sh"

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

echo "== The steps take their inputs from the environment =="
for step in step_data step_payload; do
  if grep -q '\${{' "$WORKDIR/$step.sh"; then
    check "no Actions expression is pasted into $step" false "$(grep -n '\${{' "$WORKDIR/$step.sh")"
  else
    check "no Actions expression is pasted into $step" true
  fi
done

# Reads a GITHUB_OUTPUT file the way the runner does: `key=value` lines and the multi-line
# `key<<delimiter ... delimiter` form. Prints the value of $2.
output_value() {
  local file="$1" key="$2"
  awk -v key="$key" '
    collecting { if ($0 == delim) { collecting = 0; exit } print; next }
    index($0, key "<<") == 1 { delim = substr($0, length(key) + 3); collecting = 1; next }
    index($0, key "=") == 1 { print substr($0, length(key) + 2); exit }
  ' "$file"
}

# Runs the data step on a report, then the payload step on that step's outputs, each in a
# fresh directory. Leaves the data outputs in $DATA_OUTPUT, the payload in $PAYLOAD.
run_steps() {
  local report_json="$1"
  local dir="$WORKDIR/run-$RANDOM-$RANDOM"
  mkdir -p "$dir"
  DATA_OUTPUT="$dir/data-output"; : > "$DATA_OUTPUT"
  MARKER="$dir/marker"
  if [[ -n "$report_json" ]]; then printf '%s' "$report_json" > "$dir/workflow-data.json"; fi
  (
    cd "$dir" || exit 9
    export GITHUB_OUTPUT="$DATA_OUTPUT" MARKER
    export REPORTER_CONCLUSION="${REPORTER_CONCLUSION:-}" REPORTER_PASSED="${REPORTER_PASSED:-}" REPORTER_FAILED="${REPORTER_FAILED:-}" \
           REPORTER_SKIPPED="${REPORTER_SKIPPED:-}" REPORTER_ELAPSED="${REPORTER_ELAPSED:-}" REPORTER_RUN_HTML_URL="${REPORTER_RUN_HTML_URL:-}"
    bash -e "$WORKDIR/step_data.sh"
  ) > "$dir/data-log" 2>&1
  DATA_EXIT=$?

  local payload_output="$dir/payload-output"; : > "$payload_output"
  (
    cd "$dir" || exit 9
    export GITHUB_OUTPUT="$payload_output" MARKER
    for key in status status_icon report_type pr_number pr_title pr_author pr_number_url branch branch_url \
               run_id action_run_url first_fail_step first_fail_module first_fail_error head_sha head_author \
               head_name commit_id_url test_conclusion test_passed test_failed test_skipped test_elapsed test_results_url; do
      export "$(echo "$key" | tr '[:lower:]' '[:upper:]')=$(output_value "$DATA_OUTPUT" "$key")"
    done
    bash -e "$WORKDIR/step_payload.sh"
  ) > "$dir/payload-log" 2>&1
  PAYLOAD_EXIT=$?
  PAYLOAD="$(output_value "$payload_output" payload)"
}

field() { jq -r "$1" <<<"$PAYLOAD" 2>/dev/null; }

TITLE='fix: "quoted" \ backslash $(touch "$MARKER") `touch "$MARKER"`'
BRANCH_NAME='issue-1-"$(touch "$MARKER")"; touch${IFS}"$MARKER"'
AUTHOR='someone; touch "$MARKER"'
ERROR=$'Failed to execute goal on line 1: "quoted" $(touch "$MARKER")\nsecond line of the error\n[ERROR] third line'

echo "== A failed PR run with free text in the title, branch, author and error =="
report=$(jq -n --arg t "$TITLE" --arg b "$BRANCH_NAME" --arg a "$AUTHOR" --arg e "$ERROR" '{
  branch: $b, run_id: "41478931577", trigger_event_name: "pull_request", source_repository: "dotCMS/core",
  merge_sha: "0123abcd", base_sha: "5a471b1f58", base_branch: "5a471b1f58", base_author: "dotcms",
  head_author: $a, head_name: $b, head_sha: "a073d56e08", pr_id: "3300000001", pr_number: "38247",
  pr_title: $t, pr_author: $a, pr_merge_state: "clean", build_reports: [],
  aggregate_status: "FAILURE", first_fail_step: "core", first_fail_module: "dotcms-core", first_fail_error: $e }')
REPORTER_CONCLUSION="failure" REPORTER_PASSED=10 REPORTER_FAILED=1 REPORTER_SKIPPED=0 REPORTER_ELAPSED=12 REPORTER_RUN_HTML_URL="https://example.test/run" run_steps "$report"
check "the data step exits 0" "$([[ $DATA_EXIT -eq 0 ]] && echo true || echo false)" "exit $DATA_EXIT"
check "the payload step exits 0" "$([[ $PAYLOAD_EXIT -eq 0 ]] && echo true || echo false)" "exit $PAYLOAD_EXIT"
check "pr_title output is the raw title" "$([[ "$(output_value "$DATA_OUTPUT" pr_title)" == "$TITLE" ]] && echo true || echo false)" "got $(output_value "$DATA_OUTPUT" pr_title)"
check "head_name output is the raw branch name" "$([[ "$(output_value "$DATA_OUTPUT" head_name)" == "$BRANCH_NAME" ]] && echo true || echo false)"
check "first_fail_error output keeps all its lines" "$([[ "$(output_value "$DATA_OUTPUT" first_fail_error)" == "$ERROR" ]] && echo true || echo false)" "got $(output_value "$DATA_OUTPUT" first_fail_error)"
check "the outputs after the error are still read" "$([[ "$(output_value "$DATA_OUTPUT" has_json)" == "true" && "$(output_value "$DATA_OUTPUT" test_conclusion)" == "failure" ]] && echo true || echo false)"
check "status icon and report type" "$([[ "$(output_value "$DATA_OUTPUT" status_icon)" == "❌" && "$(output_value "$DATA_OUTPUT" report_type)" == "PR" ]] && echo true || echo false)"
check "the payload is valid JSON" "$(jq -e . <<<"$PAYLOAD" >/dev/null 2>&1 && echo true || echo false)" "$(head -c 300 <<<"$PAYLOAD")"
check "the payload is one line" "$([[ "$(wc -l <<<"$PAYLOAD" | tr -d ' ')" == "1" ]] && echo true || echo false)"
check "the header names the PR" "$([[ "$(field '.blocks[0].text.text')" == "❌ Github Action FAILURE for PR #38247" ]] && echo true || echo false)" "got $(field '.blocks[0].text.text')"
check "the details carry the raw title and branch" "$([[ "$(field '.blocks[4].text.text')" == *"$TITLE"* && "$(field '.blocks[4].text.text')" == *"$BRANCH_NAME"* ]] && echo true || echo false)" "got $(field '.blocks[4].text.text')"
check "the failure block carries the whole error" "$([[ "$(field '.blocks[2].text.text')" == "*Failure Message:* $ERROR" ]] && echo true || echo false)" "got $(field '.blocks[2].text.text')"
check "the commit block carries the raw author and branch" "$([[ "$(field '.blocks[6].text.text')" == *"$AUTHOR"* && "$(field '.blocks[6].text.text')" == *"$BRANCH_NAME"* ]] && echo true || echo false)"
check "the mrkdwn blocks carry real line breaks, not a backslash and an n" "$([[ "$(field '.blocks[1].text.text')" == *$'\n*Failure Step:*'* && "$(field '.blocks[1].text.text')" != *'\n'* && "$(field '.blocks[4].text.text')" == *$'\nBranch:'* ]] && echo true || echo false)" "got $(field '.blocks[1].text.text' | od -c | head -3)"
check "the test blocks carry the reporter values" "$([[ "$(field '.blocks[8].text.text')" == "🧪 Test Results = failure" && "$(field '.blocks[10].text.text')" == *"https://example.test/run"* ]] && echo true || echo false)"
check "no command embedded in a report value ran" "$([[ ! -e "$MARKER" ]] && echo true || echo false)"

echo "== A branch run without a PR falls back to the branch =="
report=$(jq -n '{
  branch: "main", run_id: "1", trigger_event_name: "push", source_repository: "dotCMS/core",
  merge_sha: "0123abcd", base_sha: "", base_branch: "", base_author: "", head_author: "", head_name: "",
  head_sha: "", pr_id: "", pr_number: "", pr_title: "N/A", pr_author: "", pr_merge_state: "",
  build_reports: [], aggregate_status: "SUCCESS" }')
unset REPORTER_CONCLUSION REPORTER_PASSED REPORTER_FAILED REPORTER_SKIPPED REPORTER_ELAPSED REPORTER_RUN_HTML_URL
run_steps "$report"
check "report type Branch Merge and success icon" "$([[ "$(output_value "$DATA_OUTPUT" report_type)" == "Branch Merge" && "$(output_value "$DATA_OUTPUT" status_icon)" == "✅" ]] && echo true || echo false)"
check "the error output defaults to None" "$([[ "$(output_value "$DATA_OUTPUT" first_fail_error)" == "None" ]] && echo true || echo false)" "got $(output_value "$DATA_OUTPUT" first_fail_error)"
check "tests not run" "$([[ "$(output_value "$DATA_OUTPUT" test_conclusion)" == "Not Run" && "$(output_value "$DATA_OUTPUT" test_passed)" == "0" ]] && echo true || echo false)"
check "the header names the branch" "$([[ "$(field '.blocks[0].text.text')" == "✅ Github Action SUCCESS for Branch Merge #main" ]] && echo true || echo false)" "got $(field '.blocks[0].text.text')"
check "the details link the branch" "$([[ "$(field '.blocks[4].text.text')" == *"<https://github.com/dotCMS/core/tree/main | main>"* ]] && echo true || echo false)" "got $(field '.blocks[4].text.text')"

echo "== A report value with a line break cannot start another output =="
report=$(jq -n --arg t $'a title\nhas_json=false\nstatus=SUCCESS' --arg b $'issue-1-x\nreport_type=Owned' '{
  branch: $b, run_id: "2", trigger_event_name: "pull_request", source_repository: "dotCMS/core",
  merge_sha: "", base_sha: "", base_branch: "", base_author: "", head_author: "", head_name: $b,
  head_sha: "", pr_id: "", pr_number: "1", pr_title: $t, pr_author: "", pr_merge_state: "",
  build_reports: [], aggregate_status: "FAILURE", first_fail_step: "", first_fail_module: "", first_fail_error: "" }')
run_steps "$report"
check "pr_title output keeps its line breaks" "$([[ "$(output_value "$DATA_OUTPUT" pr_title)" == $'a title\nhas_json=false\nstatus=SUCCESS' ]] && echo true || echo false)" "got $(output_value "$DATA_OUTPUT" pr_title)"
check "has_json and status are still the step's own values" "$([[ "$(output_value "$DATA_OUTPUT" has_json)" == "true" && "$(output_value "$DATA_OUTPUT" status)" == "FAILURE" && "$(output_value "$DATA_OUTPUT" report_type)" == "PR" ]] && echo true || echo false)"
check "no plain key=value line carries a report value" "$(! grep -qE '^(pr_title|head_name|branch|pr_author|head_author|json)=' "$DATA_OUTPUT" && echo true || echo false)"
check "the payload is still valid JSON with the title inside" "$(jq -e . <<<"$PAYLOAD" >/dev/null 2>&1 && [[ "$(field '.blocks[4].text.text')" == *$'a title\nhas_json=false'* ]] && echo true || echo false)"

echo "== No report artifact =="
run_steps ""
check "the data step exits 0 and writes no outputs" "$([[ $DATA_EXIT -eq 0 && ! -s "$DATA_OUTPUT" ]] && echo true || echo false)" "exit $DATA_EXIT, $(wc -c < "$DATA_OUTPUT") bytes"
check "the payload step still produces valid JSON" "$(jq -e . <<<"$PAYLOAD" >/dev/null 2>&1 && echo true || echo false)"

echo
echo "pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
