#!/usr/bin/env bash
#
# Behaviour tests for the issue-linking logic in issue_comp_link-issue-to-pr.yml.
#
# The two steps under test are pure text parsing, so they are extracted from the
# workflow itself and executed with the GitHub API stubbed out. Nothing is copied
# here — if the regexes in the workflow change, these tests exercise the new ones.
#
# Run:  bash .github/workflows/tests/link-issue-to-pr.test.sh
# Deps: bash 4+ (the `${var,,}` expansion in the workflow needs it — macOS ships
#       bash 3.2, so use `docker run --rm -v "$PWD":/w -w /w bash:5 \
#       bash .github/workflows/tests/link-issue-to-pr.test.sh` there), jq, awk.
#
# Not wired into CI: .github/filters.yaml does not route .github/workflows/** to
# any build, and this workflow has no checkout step to run a script from. Tracked
# for PR 2 of #36850, which adds the workflow lint job.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORKFLOW="${1:-$REPO_ROOT/.github/workflows/issue_comp_link-issue-to-pr.yml}"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if ((BASH_VERSINFO[0] < 4)); then
  echo "ERROR: bash 4+ required (found $BASH_VERSION). See the header for the docker one-liner." >&2
  exit 2
fi

# Pull the `run: |` block belonging to a given step id out of the workflow and
# dedent it, substituting the two Actions expressions it interpolates.
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
  ' "$WORKFLOW" \
    | sed -e 's/\${{ env\.GH_TOKEN }}/$GH_TOKEN/g' \
          -e 's/\${{ github\.repository }}/$GITHUB_REPOSITORY/g' \
    > "$dest"

  if [[ ! -s "$dest" ]]; then
    echo "ERROR: could not extract step '$step_id' from $WORKFLOW" >&2
    exit 2
  fi
}

extract_step check_existing_issues "$WORKDIR/step_body.sh"
extract_step extract_issue_number  "$WORKDIR/step_branch.sh"

export GITHUB_REPOSITORY="dotCMS/core"
export GH_TOKEN="stub-token"
export PR_URL="https://github.com/dotCMS/core/pull/37193"

pass=0
fail=0

# Asserts the outputs the body-parsing step writes for a given PR body.
# $3 is the expected `key=value` set, space separated, sorted by key.
body_case() {
  local desc="$1" body="$2" expect="$3"
  export FIXTURE_CONNECTED="${4:-}"
  local out; out="$(mktemp)"
  (
    export FIXTURE_BODY="$body" GITHUB_OUTPUT="$out"
    # Stub the two API calls the step makes: PR details over curl, the paginated
    # timeline over gh. FIXTURE_CONNECTED is the issue number a Development-section
    # link would yield — empty for every case that exercises body parsing.
    curl() { jq -n --arg b "$FIXTURE_BODY" '{body:$b}'; }
    gh() { [[ -n "${FIXTURE_CONNECTED:-}" ]] && echo "$FIXTURE_CONNECTED"; return 0; }
    # shellcheck disable=SC1090
    source "$WORKDIR/step_body.sh"
  ) >/dev/null 2>&1

  local got
  got="$(grep -E '^(has_linked_issues|linked_issue_number|is_cross_repo|link_method|is_closing_link)=' "$out" \
         | sort | tr '\n' ' ' | sed 's/ $//')"
  rm -f "$out"

  if [[ "$got" == "$expect" ]]; then
    pass=$((pass + 1)); printf '  ok   %s\n' "$desc"
  else
    fail=$((fail + 1))
    printf '  FAIL %s\n        want: %s\n        got : %s\n' "$desc" "$expect" "$got"
  fi
}

branch_case() {
  local branch="$1" expect="$2"
  local out; out="$(mktemp)"
  (
    export PR_BRANCH="$branch" GITHUB_OUTPUT="$out"
    # shellcheck disable=SC1090
    source "$WORKDIR/step_branch.sh"
  ) >/dev/null 2>&1

  local got; got="$(grep '^issue_number=' "$out" | cut -d= -f2)"
  rm -f "$out"

  if [[ "$got" == "$expect" ]]; then
    pass=$((pass + 1)); printf "  ok   branch '%s' -> '%s'\n" "$branch" "$expect"
  else
    fail=$((fail + 1)); printf "  FAIL branch '%s'\n        want: '%s'\n        got : '%s'\n" "$branch" "$expect" "$got"
  fi
}

echo "== PR body: closing keywords close the issue on merge =="
body_case "Fixes #123" \
  "Some description
Fixes #123" \
  "has_linked_issues=true is_cross_repo=false link_method=pr_body linked_issue_number=123"
body_case "Closes #456" "Closes #456" \
  "has_linked_issues=true is_cross_repo=false link_method=pr_body linked_issue_number=456"
body_case "resolved: #789" "resolved: #789" \
  "has_linked_issues=true is_cross_repo=false link_method=pr_body linked_issue_number=789"
body_case "cross-repo 'Fixes org/repo#42'" "Fixes dotCMS/private-issues#42" \
  "has_linked_issues=true is_cross_repo=true link_method=cross_repo_body linked_issue_number=42"
body_case "cross-repo full URL" "Closes https://github.com/dotCMS/private-issues/issues/99" \
  "has_linked_issues=true is_cross_repo=true link_method=cross_repo_url linked_issue_number=99"
body_case "owner/repo form pointing at this repo" "Closes dotCMS/core#77" \
  "has_linked_issues=true is_cross_repo=false link_method=pr_body linked_issue_number=77"

echo "== PR body: non-closing references link without closing =="
body_case "Refs #36850" \
  "Parent issue: #36850

Refs #36850" \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_body_reference linked_issue_number=36850"
body_case "Part of #100" "Part of #100" \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_body_reference linked_issue_number=100"
body_case "Related to #101" "Related to #101" \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_body_reference linked_issue_number=101"
body_case "References #102" "References #102" \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_body_reference linked_issue_number=102"
body_case "contributes to #103" "contributes to #103" \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_body_reference linked_issue_number=103"
body_case "ref: #104" "ref: #104" \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_body_reference linked_issue_number=104"

echo "== PR body: non-closing references are same-repo only, by design =="
# Cross-repo and full-URL forms are supported for *closing* keywords only. Nobody has
# needed a non-closing cross-repo link, and each extra form is another branch in a
# merge gate. These stay unlinked until someone actually needs them.
body_case "cross-repo reference does not link" "Refs dotCMS/private-issues#55" \
  "has_linked_issues=false is_cross_repo=false"
body_case "reference by full URL does not link" "Part of https://github.com/dotCMS/core/issues/66" \
  "has_linked_issues=false is_cross_repo=false"

echo "== PR body: a closing keyword always outranks a reference =="
body_case "Refs #1 alongside Fixes #2" \
  "Refs #1
Fixes #2" \
  "has_linked_issues=true is_cross_repo=false link_method=pr_body linked_issue_number=2"

echo "== Development section: a sidebar link outranks the body and closes on merge =="
# Regression guard for the unpaginated timeline lookup: the "connected" event on
# PR #37193 was the 33rd of 37, past the endpoint's 30-per-page default, so the
# workflow read a sidebar-linked PR as unlinked and reported is_closing_link=false
# for an issue GitHub was about to close. gh api --paginate is what fixes it.
body_case "sidebar link beats a non-closing body reference" "Refs #36850" \
  "has_linked_issues=true is_cross_repo=false link_method=development_section linked_issue_number=36850" \
  "36850"
body_case "sidebar link with an unrelated body" "No references at all here." \
  "has_linked_issues=true is_cross_repo=false link_method=development_section linked_issue_number=42" \
  "42"

echo "== PR body: bare mentions are still not a link =="
body_case "prose mention only" "Parent issue: #36850 and follow-up #37194" \
  "has_linked_issues=false is_cross_repo=false"
body_case "nothing linked" "Just prose, nothing linked." \
  "has_linked_issues=false is_cross_repo=false"
body_case "'referenced #12' is not a keyword" "This was referenced #12" \
  "has_linked_issues=false is_cross_repo=false"

echo "== Branch names =="
branch_case "nicobytes/36850-upgrade-github-actions-to-node-24-runtime-majors" "36850"
branch_case "oidacra/37132-picker-per-host" "37132"
branch_case "36850-upgrade-github-actions" "36850"
branch_case "issue-37186-content-drive-user-cache-sizing" "37186"
branch_case "nicobytes/issue-36850-something" "36850"
branch_case "feature-issue-123" "123"
branch_case "gh-readonly-queue/main/pr-37131-2f60886e31b919f59779faf9ea5d356bc8d4d1c8" ""
branch_case "nicobytes/assetpicker-new-sidebar-ui" ""
branch_case "release/26.08.24" ""
branch_case "main" ""

echo
echo "pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
