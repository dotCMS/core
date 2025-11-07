#!/usr/bin/env bash
# Fetch job details with caching (bash/zsh compatible)

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <RUN_ID> <WORKSPACE>"
    exit 1
fi

RUN_ID="$1"
WORKSPACE="$2"

# Validate inputs
if [ -z "$WORKSPACE" ]; then
    echo "ERROR: WORKSPACE parameter is required" >&2
    exit 1
fi

JOBS="$WORKSPACE/jobs-detailed.json"

# Fetch jobs if not cached
if [ ! -f "$JOBS" ]; then
    echo "Fetching job details..."
    gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" --paginate > "$JOBS"
    echo "✓ Job details saved to $JOBS"
else
    echo "✓ Using cached jobs: $JOBS"
fi

# Display failed jobs
echo ""
echo "=== Failed Jobs ==="
jq -r '.jobs[] | select(.conclusion == "failure") | "Name: \(.name)\nID: \(.id)\nConclusion: \(.conclusion)\n"' "$JOBS"