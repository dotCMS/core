#!/usr/bin/env bash
# Main entry point for CI/CD diagnostics
# Usage: analyze-run.sh RUN_ID [options]

set -euo pipefail

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Load utilities
source "$SCRIPT_DIR/utils/workspace.sh"
source "$SCRIPT_DIR/utils/github-api.sh"
source "$SCRIPT_DIR/utils/evidence.sh"
source "$SCRIPT_DIR/utils/utilities.sh"

# Parse arguments
RUN_ID="${1:-}"
FORCE_CLEAN="${2:-}"

if [ -z "$RUN_ID" ]; then
    echo "Usage: analyze-run.sh RUN_ID [clean]"
    echo ""
    echo "Examples:"
    echo "  analyze-run.sh 19131365567"
    echo "  analyze-run.sh 19131365567 clean  # Force clean start"
    exit 1
fi

echo "🔍 Analyzing GitHub Actions run: $RUN_ID"
echo ""

# Step 1: Create/get workspace
echo "=== Step 1: Setting up workspace ==="
WORKSPACE=$(get_diagnostic_workspace "$RUN_ID" "$FORCE_CLEAN")
echo ""

# Step 2: Fetch run metadata
echo "=== Step 2: Fetching run metadata ==="
METADATA="$WORKSPACE/run-metadata.json"

if [ ! -f "$METADATA" ]; then
    echo "Downloading run metadata..."
    gh run view "$RUN_ID" --json conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle > "$METADATA"
    echo "✓ Downloaded metadata"
else
    echo "✓ Using cached metadata"
fi

echo ""
echo "Run Details:"
jq -r '"  Workflow: \(.workflowName)\n  Status: \(.conclusion)\n  Branch: \(.headBranch)\n  Event: \(.event)\n  URL: \(.url)"' "$METADATA"
echo ""

# Step 3: Fetch jobs details
echo "=== Step 3: Fetching jobs details ==="
JOBS="$WORKSPACE/jobs-detailed.json"

if [ ! -f "$JOBS" ]; then
    echo "Downloading jobs details..."
    gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" --paginate > "$JOBS"
    echo "✓ Downloaded jobs details"
else
    echo "✓ Using cached jobs details"
fi

# Count jobs
TOTAL_JOBS=$(jq '.total_count' "$JOBS")
FAILED_JOBS=$(jq '[.jobs[] | select(.conclusion == "failure")] | length' "$JOBS")

echo "  Total jobs: $TOTAL_JOBS"
echo "  Failed jobs: $FAILED_JOBS"
echo ""

if [ "$FAILED_JOBS" -eq 0 ]; then
    echo "✅ No failed jobs found in this run"
    exit 0
fi

# Show failed jobs
echo "Failed Jobs:"
jq -r '.jobs[] | select(.conclusion == "failure") | "  - \(.name) (ID: \(.id))"' "$JOBS"
echo ""

# Step 4: Download logs for first failed job
echo "=== Step 4: Downloading failed job logs ==="

FAILED_JOB_ID=$(jq -r '.jobs[] | select(.conclusion == "failure") | .id' "$JOBS" | head -1)
FAILED_JOB_NAME=$(jq -r '.jobs[] | select(.conclusion == "failure") | .name' "$JOBS" | head -1)

echo "Analyzing: $FAILED_JOB_NAME (ID: $FAILED_JOB_ID)"

LOG_FILE="$WORKSPACE/failed-job-${FAILED_JOB_ID}.txt"

if [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
    echo "Downloading logs..."
    gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > "$LOG_FILE"
    LOG_SIZE=$(wc -c < "$LOG_FILE" | xargs)
    echo "✓ Downloaded: $(numfmt --to=iec-i --suffix=B "$LOG_SIZE")"
else
    LOG_SIZE=$(wc -c < "$LOG_FILE" | xargs)
    echo "✓ Using cached logs: $(numfmt --to=iec-i --suffix=B "$LOG_SIZE")"
fi
echo ""

# Step 5: Extract evidence
echo "=== Step 5: Extracting failure evidence ==="

# Check if log is too large
if [ "$LOG_SIZE" -gt 10485760 ]; then
    echo "⚠️  Large log detected (>10MB) - extracting error sections only..."
    ERROR_FILE="$WORKSPACE/error-sections.txt"

    # Extract error sections (simple approach)
    grep -i -C 5 "error\|fail\|exception" "$LOG_FILE" > "$ERROR_FILE" || true

    if [ -s "$ERROR_FILE" ]; then
        LOG_TO_ANALYZE="$ERROR_FILE"
        echo "✓ Extracted error sections: $(wc -l < "$ERROR_FILE" | xargs) lines"
    else
        LOG_TO_ANALYZE="$LOG_FILE"
        echo "⚠️  No error sections extracted, will analyze full log"
    fi
else
    LOG_TO_ANALYZE="$LOG_FILE"
    echo "✓ Log size acceptable, will analyze full log"
fi
echo ""

# Step 6: Present evidence
echo "=== Step 6: Presenting diagnostic evidence ==="
EVIDENCE_FILE="$WORKSPACE/evidence.txt"

present_complete_diagnostic "$LOG_TO_ANALYZE" > "$EVIDENCE_FILE"

echo "✓ Evidence extracted to: $EVIDENCE_FILE"
echo ""

# Show evidence summary
echo "Evidence Summary:"
echo "----------------"
head -100 "$EVIDENCE_FILE"
echo ""
echo "... (see $EVIDENCE_FILE for complete evidence)"
echo ""

# Step 7: Summary
echo "=== Diagnostic Summary ==="
echo "Run ID: $RUN_ID"
echo "Workspace: $WORKSPACE"
echo "Files created:"
echo "  - $METADATA"
echo "  - $JOBS"
echo "  - $LOG_FILE"
echo "  - $EVIDENCE_FILE"
echo ""
echo "✅ Diagnostic data collection complete"
echo ""
echo "Next steps:"
echo "  1. Review evidence file: cat $EVIDENCE_FILE"
echo "  2. Analyze patterns and classify root cause"
echo "  3. Check for known issues in GitHub"
echo "  4. Create diagnosis report"