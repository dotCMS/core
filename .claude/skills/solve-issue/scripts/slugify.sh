#!/usr/bin/env bash
# Usage: slugify.sh <issue-number> "<issue-title>"
# Outputs: issue-123-fix-login-redirect-loop

set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "ERROR: Missing arguments." >&2
  echo "Usage: slugify.sh <issue-number> \"<issue-title>\"" >&2
  exit 1
fi

NUMBER="$1"
TITLE="$2"

SLUG=$(echo "$TITLE" \
  | tr '[:upper:]' '[:lower:]' \
  | sed 's/[^a-z0-9]/-/g' \
  | sed 's/-\{2,\}/-/g' \
  | sed 's/^-//;s/-$//' \
  | cut -c1-50 \
  | sed 's/-$//')

echo "issue-${NUMBER}-${SLUG}"
