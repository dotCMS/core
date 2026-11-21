#!/usr/bin/env bash
set -euo pipefail

repo=$1
version=$2
target=$3

# Keep the standard release tag pattern in sync with release-qa-status.
tags=$(gh api "repos/$repo/releases?per_page=100" --paginate \
  --jq '.[] | select(.tag_name | test("^v[0-9]{2}\\.[0-9]{2}\\.[0-9]{2}-[0-9]{1,2}$")) | .tag_name')
previous=$(awk -v current="v$version" '$0 != current' <<< "$tags" | sort -V | tail -1)
if [[ -z "$previous" ]]; then
  echo "No previous standard release to compare against." >&2
  exit 0
fi

# Commit associations work for merge commits as well as squash merges.
commits=$(gh api "repos/$repo/compare/$previous...$target?per_page=100" --paginate --jq '.commits[].sha')
for sha in $commits; do
  if ! gh api "repos/$repo/commits/$sha/pulls?per_page=100" --paginate --jq '
    .[] | select(.merged_at != null and .base.ref == "main"
      and any(.labels[]; .name == "SDK Breaking Change")) | "#\(.number)"'; then
    echo "::error::Could not look up PRs for $sha." >&2
    exit 1
  fi
done | sort -u
