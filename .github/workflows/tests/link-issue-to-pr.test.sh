#!/usr/bin/env bash
#
# Behaviour tests for the issue-linking logic in issue_comp_link-issue-to-pr.yml,
# and for the line issue_comp_link-pr-to-issue.yml appends to the body on merge.
#
# The steps under test are extracted from the workflows themselves and executed
# with the GitHub API stubbed out. Nothing is copied here — if the regexes in the
# workflows change, these tests exercise the new ones.
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
MERGE_WORKFLOW="${2:-$REPO_ROOT/.github/workflows/issue_comp_link-pr-to-issue.yml}"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if ((BASH_VERSINFO[0] < 4)); then
  echo "ERROR: bash 4+ required (found $BASH_VERSION). See the header for the docker one-liner." >&2
  exit 2
fi

# Pull the `run: |` block belonging to a given step id out of a workflow ($3,
# default $WORKFLOW) and dedent it, substituting the Actions expressions it
# interpolates: `${{ inputs.x }}` becomes `${INPUT_x}`.
extract_step() {
  local step_id="$1" dest="$2" workflow="${3:-$WORKFLOW}"
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
  ' "$workflow" \
    | sed -e 's/\${{ env\.GH_TOKEN }}/$GH_TOKEN/g' \
          -e 's/\${{ github\.repository }}/$GITHUB_REPOSITORY/g' \
          -e 's/\${{ inputs\.\([a-z_]*\) }}/${INPUT_\1}/g' \
    > "$dest"

  if [[ ! -s "$dest" ]]; then
    echo "ERROR: could not extract step '$step_id' from $workflow" >&2
    exit 2
  fi
  if grep -qF "\${{" "$dest"; then
    echo "ERROR: step '$step_id' interpolates an expression extract_step does not substitute" >&2
    exit 2
  fi
}

extract_step check_existing_issues "$WORKDIR/step_body.sh"
extract_step extract_issue_number  "$WORKDIR/step_branch.sh"
extract_step determine_issue       "$WORKDIR/step_determine.sh"
extract_step link_pr               "$WORKDIR/step_link.sh"
extract_step link_pr_to_issue      "$WORKDIR/step_merge.sh" "$MERGE_WORKFLOW"

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
  export FIXTURE_TITLE="${5:-}"
  local out; out="$(mktemp)"
  (
    export FIXTURE_BODY="$body" PR_TITLE="$FIXTURE_TITLE" GITHUB_OUTPUT="$out"
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

expect_eq() {
  local desc="$1" want="$2" got="$3"
  if [[ "$got" == "$want" ]]; then
    pass=$((pass + 1)); printf '  ok   %s\n' "$desc"
  else
    fail=$((fail + 1))
    printf '  FAIL %s\n        want: %s\n        got : %s\n' "$desc" "$want" "$got"
  fi
}

# Last line of the body a stub recorded as PATCHed, i.e. the line the step appended.
patched_line() {
  local body
  [[ -f "$1" ]] || { echo "(body not patched)"; return; }
  body="$(<"$1")"
  echo "${body##*$'\n'}"
}

# Answers `gh api graphql ... --jq FILTER` by running the step's own FILTER over a
# closingIssuesReferences payload built from FIXTURE_CLOSING ("owner/repo#N ...").
# FIXTURE_CLOSING=error fails the call the way gh does: error JSON, non-zero exit.
graphql_stub() {
  local filter="" prev="" arg
  for arg in "$@"; do [[ "$prev" == "--jq" ]] && filter="$arg"; prev="$arg"; done
  if [[ "${FIXTURE_CLOSING:-}" == "error" ]]; then
    echo '{"errors":[{"type":"NOT_FOUND"}]}'
    return 1
  fi
  jq -n --arg refs "${FIXTURE_CLOSING:-}" '{data: {repository: {pullRequest: {closingIssuesReferences: {nodes: [
      $refs | splits(" +") | select(length > 0) | capture("^(?<repo>.+)#(?<num>[0-9]+)$")
      | {number: (.num | tonumber), repository: {nameWithOwner: .repo}}
    ]}}}}}' | jq -r "$filter"
}

# Asserts the issue the decision step settles on. With nothing linked from the body
# ($4, plus $5 for a cross-repo link) GitHub's closing references ($2) decide, and
# without those the branch number (37845).
determine_case() {
  local desc="$1" closing="$2" expect="$3" linked="${4:-}" linked_repo="${5:-}"
  local has_linked=false cross_repo=false
  [[ -n "$linked" ]] && has_linked=true
  [[ -n "$linked_repo" ]] && cross_repo=true
  local out; out="$(mktemp)"
  (
    export GITHUB_OUTPUT="$out" FIXTURE_CLOSING="$closing" BRANCH_ISSUE_NUMBER=37845 \
      HAS_LINKED_ISSUES="$has_linked" LINKED_ISSUE_NUMBER="$linked" \
      LINK_METHOD="${linked:+pr_body}" IS_CLOSING_LINK="" \
      IS_CROSS_REPO="$cross_repo" CROSS_REPO_OWNER_REPO="$linked_repo"
    gh() { graphql_stub "$@"; }
    set +u -e  # a run step without `shell:` executes as `bash -e`
    # shellcheck disable=SC1090
    source "$WORKDIR/step_determine.sh"
  ) >/dev/null 2>&1

  local got
  got="$(grep -E '^(final_issue_number|is_cross_repo|cross_repo_owner_repo)=' "$out" \
         | sort | tr '\n' ' ' | sed 's/ $//')"
  rm -f "$out"
  expect_eq "$desc" "$expect" "$got"
}

# Asserts the line the body-patch step appends for issue $2: "#N" is a same-repo
# issue, "owner/repo#N" a cross-repo one. $3 is the current body, $4 GitHub's
# closing references.
link_case() {
  local desc="$1" issue="$2" body="$3" closing="$4" expect="$5"
  local cross_repo=true patched="$WORKDIR/patched_body"
  [[ "$issue" == "#"* ]] && cross_repo=false
  rm -f "$patched"
  (
    export ISSUE_NUMBER="${issue##*#}" CROSS_REPO_OWNER_REPO="${issue%#*}" \
      IS_CROSS_REPO="$cross_repo" FIXTURE_BODY="$body" FIXTURE_CLOSING="$closing" \
      PATCHED="$patched"
    # GET returns the current body, PATCH records the new one.
    curl() {
      local prev="" arg data="" patch=false
      for arg in "$@"; do
        [[ "$prev" == "-X" && "$arg" == "PATCH" ]] && patch=true
        [[ "$prev" == "-d" ]] && data="$arg"
        prev="$arg"
      done
      if $patch; then jq -r .body <<<"$data" > "$PATCHED"; else jq -n --arg b "$FIXTURE_BODY" '{body: $b}'; fi
    }
    gh() { graphql_stub "$@"; }
    set +u -e  # a run step without `shell:` executes as `bash -e`
    # shellcheck disable=SC1090
    source "$WORKDIR/step_link.sh"
  ) >/dev/null 2>&1
  expect_eq "$desc" "$expect" "$(patched_line "$patched")"
}

# Asserts the line the merge-time workflow appends for a branch and body. $4 lists
# the issue numbers that exist in this repository; looking up any other is a 404.
merge_case() {
  local desc="$1" branch="$2" body="$3" core_issues="$4" expect="$5"
  local patched="$WORKDIR/patched_body"
  rm -f "$patched"
  (
    export INPUT_pr_number=37900 INPUT_pr_branch="$branch" INPUT_validate_merge=true \
      FIXTURE_BODY="$body" FIXTURE_CORE_ISSUES="$core_issues" PATCHED="$patched"
    # GET pulls/N is the merged PR, GET issues/N one of FIXTURE_CORE_ISSUES, PATCH
    # records the new body.
    gh() {
      local prev="" arg method="" path="" new_body=""
      for arg in "$@"; do
        case "$prev" in
          --method) method="$arg" ;;
          -f) [[ "$arg" == body=* ]] && new_body="${arg#body=}" ;;
        esac
        [[ "$arg" == repos/* || "$arg" == /repos/* ]] && path="$arg"
        prev="$arg"
      done
      if [[ "$method" == "PATCH" ]]; then
        printf '%s' "$new_body" > "$PATCHED"
      elif [[ "$path" == */pulls/* ]]; then
        jq -n --arg b "$FIXTURE_BODY" '{merged: true, body: $b}'
      elif [[ " $FIXTURE_CORE_ISSUES " == *" ${path##*/} "* ]]; then
        jq -n --argjson n "${path##*/}" '{number: $n}'
      else
        echo '{"message":"Not Found","status":"404"}'
        return 1
      fi
    }
    set +u -eo pipefail  # the step declares `shell: bash`, i.e. bash -eo pipefail
    # shellcheck disable=SC1090
    source "$WORKDIR/step_merge.sh"
  ) >/dev/null 2>&1
  expect_eq "$desc" "$expect" "$(patched_line "$patched")"
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

echo "== PR title: trailing issue suffix links without closing =="
body_case "title suffix (#37417)" \
  "Closes nothing yet — implementation lands in PR 2." \
  "has_linked_issues=true is_closing_link=false is_cross_repo=false link_method=pr_title_reference linked_issue_number=37417" \
  "" \
  "Spec: Dojo to Angular dotAI portlet migration (#37417)"

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

echo "== Decision: GitHub's closing references keep their repository =="
# Regression guard for #37845: this fallback read only the number, so another
# repository's issue N was handled as this repository's unrelated #N.
determine_case "closing ref in another repository" "dotCMS/private-issues#642" \
  "cross_repo_owner_repo=dotCMS/private-issues final_issue_number=642 is_cross_repo=true"
determine_case "closing ref in this repository" "dotCMS/core#36850" \
  "cross_repo_owner_repo= final_issue_number=36850 is_cross_repo=false"
determine_case "this repository in other letter case" "dotcms/CORE#36850" \
  "cross_repo_owner_repo= final_issue_number=36850 is_cross_repo=false"
determine_case "no closing ref falls back to the branch" "" \
  "cross_repo_owner_repo= final_issue_number=37845 is_cross_repo=false"
determine_case "failed lookup falls back to the branch" "error" \
  "cross_repo_owner_repo= final_issue_number=37845 is_cross_repo=false"
determine_case "cross-repo body link passes through" "" \
  "cross_repo_owner_repo=dotCMS/private-issues final_issue_number=42 is_cross_repo=true" \
  "42" "dotCMS/private-issues"

echo "== Body patch: the appended line names the issue's repository =="
link_case "same-repo issue gets a bare #N" "#123" "Some description" "" \
  "This PR fixes: #123"
link_case "cross-repo issue keeps owner/repo" "dotCMS/private-issues#642" "Some description" "" \
  "This PR fixes: dotCMS/private-issues#642"
link_case "cross-repo issue on an empty body" "dotCMS/private-issues#642" "" "" \
  "This PR fixes: dotCMS/private-issues#642"
link_case "cross-repo issue the body already closes" "dotCMS/private-issues#42" \
  "Fixes dotCMS/private-issues#42" "" "(body not patched)"
link_case "cross-repo issue the body closes by URL" "dotCMS/private-issues#99" \
  "Closes https://github.com/dotCMS/private-issues/issues/99" "" "(body not patched)"
link_case "cross-repo issue GitHub already lists as closing" "dotCMS/private-issues#642" \
  "Fixes [the tracking issue](https://github.com/dotCMS/private-issues/issues/642)" \
  "dotCMS/private-issues#642" "(body not patched)"
link_case "same-repo issue the body already closes" "#123" "Fixes #123" "" "(body not patched)"

echo "== Merge-time line (issue_comp_link-pr-to-issue.yml) keeps the issue's repository =="
# Regression guard for #37845: a branch named after another repository's issue
# ("issue-642-...") got "This PR fixes: #642", which is this repository's #642.
merge_case "branch named after a cross-repo issue the body closes" "issue-642-fix-something" \
  "Closes dotCMS/private-issues#642" "642" "This PR fixes: dotCMS/private-issues#642"
merge_case "closed with 'Fixes:'" "issue-642-fix-something" \
  "Fixes: dotCMS/private-issues#642" "642" "This PR fixes: dotCMS/private-issues#642"
merge_case "closed by issue URL" "issue-642-fix-something" \
  "- Fixes: https://github.com/dotCMS/private-issues/issues/642" "642" \
  "This PR fixes: dotCMS/private-issues#642"
merge_case "keyword in capitals, number-only branch" "642-fix-something" \
  "This CLOSES dotCMS/private-issues#642." "642" "This PR fixes: dotCMS/private-issues#642"
merge_case "cross-repo issue is not looked up in this repository" "issue-642-fix-something" \
  "Closes dotCMS/private-issues#642" "" "This PR fixes: dotCMS/private-issues#642"
merge_case "same-repo issue keeps the bare #N" "issue-37845-fix-something" \
  "Fixes #37845" "37845" "This PR fixes: #37845"
merge_case "branch only, body says nothing" "37845-fix-something" \
  "Just prose." "37845" "This PR fixes: #37845"
merge_case "owner/repo form pointing at this repository" "issue-37845-fix-something" \
  "Closes dotCMS/core#37845" "37845" "This PR fixes: #37845"
merge_case "cross-repo ref with a different number" "issue-37845-fix-something" \
  "Fixes dotCMS/private-issues#99" "37845" "This PR fixes: #37845"
merge_case "branch number is a prefix of the cross-repo one" "issue-642-fix-something" \
  "Closes dotCMS/private-issues#6420" "642" "This PR fixes: #642"
merge_case "mention without a closing keyword" "issue-642-fix-something" \
  "See dotCMS/private-issues#642" "642" "This PR fixes: #642"
merge_case "same-repo issue that does not exist" "issue-37845-fix-something" \
  "Just prose." "" "(body not patched)"
merge_case "branch without an issue number" "fix-something" \
  "Closes dotCMS/private-issues#642" "642" "(body not patched)"

echo
echo "pass=$pass fail=$fail"
[[ $fail -eq 0 ]]
