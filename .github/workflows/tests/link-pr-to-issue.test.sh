#!/usr/bin/env bash
#
# Behaviour tests for the "Link PR to Issue" step in issue_comp_link-pr-to-issue.yml.
#
# The step is extracted from the workflow itself and executed with `gh` stubbed, so the
# shell that ships is the shell under test. The inputs reach the step through the
# environment (PR_NUMBER, PR_BRANCH, VALIDATE_MERGE), the same way the job's `env:` block
# hands them over on the runner, so the extracted text must not carry any `${{ }}`
# expression: a branch name is free text chosen by whoever opened the PR, and these cases
# include names with shell syntax, which must stay text.
#
# Run:  bash .github/workflows/tests/link-pr-to-issue.test.sh
# Deps: bash 4+, jq, awk.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORKFLOW="${1:-$REPO_ROOT/.github/workflows/issue_comp_link-pr-to-issue.yml}"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if ((BASH_VERSINFO[0] < 4)); then
  echo "ERROR: bash 4+ required (found $BASH_VERSION)." >&2
  exit 2
fi

# Pull the `run: |` block belonging to a given step id out of the workflow and dedent it.
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

extract_step link_pr_to_issue "$WORKDIR/step.sh"

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

# Runs the step for a branch name with the GitHub API stubbed.
#   $1 description, $2 branch, $3 expected exit code, $4 expected PATCH body ("" = no PATCH),
#   $5 PR body returned by the API (default "Some description"; "null" for a PR without one),
#   $6 merged flag returned by the API (default true), $7 VALIDATE_MERGE (default true)
link_case() {
  local desc="$1" branch="$2" expect_exit="$3" expect_body="$4"
  local fixture_body="${5-Some description}" fixture_merged="${6:-true}" validate="${7:-true}"
  local marker="$WORKDIR/marker-$RANDOM-$RANDOM" patch="$WORKDIR/patch-$RANDOM"
  local log="$WORKDIR/log"
  rm -f "$marker" "$patch"
  (
    export GITHUB_REPOSITORY="dotCMS/core" GH_TOKEN="stub-token"
    export PR_NUMBER=38247 PR_BRANCH="$branch" VALIDATE_MERGE="$validate"
    export MARKER="$marker" PATCH_FILE="$patch"
    export FIXTURE_BODY="$fixture_body" FIXTURE_MERGED="$fixture_merged"
    # Stub of `gh api`: the PR read, the issue read and the PR body PATCH.
    gh() {
      local method='' path='' body=''
      while (( $# )); do
        case "$1" in
          api) shift ;;
          --method) method="$2"; shift 2 ;;
          -H) shift 2 ;;
          -f) body="${2#body=}"; shift 2 ;;
          *) path="$1"; shift ;;
        esac
      done
      case "$method:$path" in
        GET:*/pulls/*)
          jq -n --arg b "$FIXTURE_BODY" --argjson m "$FIXTURE_MERGED" \
            '{merged: $m, body: (if $b == "null" then null else $b end)}' ;;
        GET:*/issues/999) return 1 ;;
        GET:*/issues/*) jq -n --arg n "${path##*/}" '{number: ($n | tonumber)}' ;;
        PATCH:*) printf '%s' "$body" > "$PATCH_FILE" ;;
        *) echo "unexpected gh call: $method $path" >&2; return 1 ;;
      esac
    }
    # shellcheck disable=SC1090
    source "$WORKDIR/step.sh"
  ) > "$log" 2>&1
  local got_exit=$?
  local got_body=''
  [[ -f "$patch" ]] && got_body="$(cat "$patch")"

  local ok=true detail=''
  [[ "$got_exit" == "$expect_exit" ]] || { ok=false; detail+="exit want $expect_exit got $got_exit; "; }
  [[ "$got_body" == "$expect_body" ]] || { ok=false; detail+="body want [$expect_body] got [$got_body]; "; }
  [[ ! -e "$marker" ]] || { ok=false; detail+="a command embedded in the branch name ran; "; }
  check "$desc" "$ok" "$detail"
}

echo "== Branch names that name an issue =="
link_case "issue-<n>- prefix" "issue-123-file-extension-check" 0 $'Some description\n\nThis PR fixes: #123'
link_case "<n>- prefix" "456-upgrade-actions" 0 $'Some description\n\nThis PR fixes: #456'
link_case "PR without a body" "issue-123-no-body" 0 'This PR fixes: #123' null
link_case "PR with an empty body" "issue-123-empty-body" 0 'This PR fixes: #123' ''

echo "== Branch names that do not =="
link_case "feature branch" "feature/no-issue" 2 ''
link_case "owner/<n>- form is not resolved by this step" "nicobytes/36850-upgrade" 2 ''
link_case "main" "main" 2 ''

echo "== Branch names carrying shell syntax are only text =="
link_case "command substitution" 'issue-123-$(touch "$MARKER")' 0 $'Some description\n\nThis PR fixes: #123'
link_case "backticks" 'issue-123-`touch "$MARKER"`' 0 $'Some description\n\nThis PR fixes: #123'
link_case "semicolon and IFS" 'issue-123-;touch${IFS}$MARKER' 0 $'Some description\n\nThis PR fixes: #123'
link_case "pipe" 'issue-123-x|touch${IFS}$MARKER' 0 $'Some description\n\nThis PR fixes: #123'
link_case "unresolvable issue with shell syntax" 'feature-$(touch "$MARKER")' 2 ''

echo "== Merge validation =="
link_case "unmerged PR is refused when validate_merge is on" "issue-123-unmerged" 1 '' "Some description" false true
link_case "unmerged PR is linked when validate_merge is off" "issue-123-unmerged" 0 $'Some description\n\nThis PR fixes: #123' "Some description" false false

echo "== Issue lookup =="
link_case "an issue the API cannot find" "issue-999-missing" 3 ''

echo
echo "pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
