#!/usr/bin/env bash
# Fetch failed job logs with caching (bash/zsh compatible)

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <WORKSPACE> <FAILED_JOB_ID>"
    exit 1
fi

WORKSPACE="$1"
FAILED_JOB_ID="$2"

# Validate inputs
if [ -z "$WORKSPACE" ]; then
    echo "ERROR: WORKSPACE parameter is required" >&2
    exit 1
fi

if [ -z "$FAILED_JOB_ID" ]; then
    echo "ERROR: FAILED_JOB_ID parameter is required" >&2
    exit 1
fi

LOG_FILE="$WORKSPACE/failed-job-${FAILED_JOB_ID}.txt"

# Download logs if not cached or empty
if [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
    echo "Downloading logs for job $FAILED_JOB_ID..."
    gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > "$LOG_FILE"

    if command -v numfmt &> /dev/null; then
        SIZE=$(wc -c < "$LOG_FILE" | numfmt --to=iec-i)
    else
        SIZE=$(wc -c < "$LOG_FILE" | awk '{print $1}')
    fi
    echo "✓ Downloaded: ${SIZE}B"
else
    if command -v numfmt &> /dev/null; then
        SIZE=$(wc -c < "$LOG_FILE" | numfmt --to=iec-i)
    else
        SIZE=$(wc -c < "$LOG_FILE" | awk '{print $1}')
    fi
    echo "✓ Using cached logs: ${SIZE}B"
fi

echo "$LOG_FILE"