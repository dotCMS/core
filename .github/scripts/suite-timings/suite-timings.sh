#!/usr/bin/env bash
#
# Median CI runtime of every MainSuite* integration-test job, sampled from the
# most recent successful PR Check runs.
#
# New integration-test coverage goes in the shortest-running compatible suite
# (docs/testing/INTEGRATION_TESTS.md -> "Registering Tests in a MainSuite").
# That suite is whichever one measures fastest right now, so it has to be read
# from CI rather than remembered: run this, register in the suite the last line
# names, and paste this output into the PR description as the evidence.
#
#   just suite-timings        # 3 samples (the documented minimum)
#   just suite-timings 6      # more samples, steadier medians
#
# Junit5Suite1 and OpenSearchUpgradeSuite are deliberately excluded: those are
# destinations by framework or environment requirement, never by runtime.
#
set -euo pipefail

REPO="${REPO:-dotCMS/core}"
WORKFLOW="${WORKFLOW:-cicd_1-pr.yml}"
SAMPLES="${1:-3}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh is required: brew install gh && gh auth login" >&2
  exit 1
fi

runs=$(gh run list --repo "$REPO" --workflow "$WORKFLOW" --status success \
         --limit "$SAMPLES" --json databaseId,headBranch,url \
         --jq '.[] | [.databaseId, .headBranch, .url] | @tsv')

if [ -z "$runs" ]; then
  echo "No successful '$WORKFLOW' runs found in $REPO." >&2
  echo "Report this and ask the developer which suite to use - do not guess a default." >&2
  exit 1
fi

# Only jobs that actually finished successfully are sampled, which is what keeps
# cancelled, skipped and build-cache-short-circuited jobs out of the medians.
{
  while IFS=$'\t' read -r id branch url; do
    echo "sampled: ${branch}  ${url}" >&2
    gh api "repos/${REPO}/actions/runs/${id}/jobs?per_page=100" --jq '
      .jobs[]
      | select(.conclusion == "success")
      | select(.name | test("Integration Tests - MainSuite"))
      | [(.name | sub(".*MainSuite ";"")), .started_at, .completed_at]
      | @tsv'
  done <<< "$runs"
} | python3 "${HERE}/median.py"
