#!/usr/bin/env bash
# Analyze a single GitHub Actions run with AI-guided diagnostics
# Usage: ./analyze-single-run.sh <RUN_ID> [--clean]
#
# Options:
#   --clean   Remove existing workspace and download fresh data

# Don't use 'set -e' to prevent SIGPIPE from killing the script
set -uo pipefail

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Load utilities using absolute paths
source "$SCRIPT_DIR/utils/workspace.sh"
source "$SCRIPT_DIR/utils/github-api.sh"
source "$SCRIPT_DIR/utils/evidence.sh"
source "$SCRIPT_DIR/utils/utilities.sh"

# Verify utilities loaded
if ! command -v get_diagnostic_workspace &> /dev/null; then
    echo "ERROR: Utilities not loaded correctly"
    exit 1
fi

echo "✅ All utilities loaded successfully"
echo ""

# Parse arguments
RUN_ID=""
CLEAN_MODE=""

for arg in "$@"; do
    case $arg in
        --clean)
            CLEAN_MODE="clean"
            ;;
        *)
            if [ -z "$RUN_ID" ]; then
                RUN_ID="$arg"
            fi
            ;;
    esac
done

if [ -z "$RUN_ID" ]; then
    echo "Usage: $0 <RUN_ID> [--clean]"
    echo ""
    echo "Options:"
    echo "  --clean   Remove existing workspace and download fresh data"
    echo ""
    echo "Example: $0 19131365567"
    echo "Example: $0 19131365567 --clean"
    exit 1
fi

echo "=== CI/CD Diagnostics - Run $RUN_ID ==="
if [ -n "$CLEAN_MODE" ]; then
    echo "Mode: Fresh analysis (cleaning existing workspace)"
fi
echo ""

# Step 1: Create workspace
echo "Step 1: Creating diagnostic workspace..."
WORKSPACE=$(get_diagnostic_workspace "$RUN_ID" "$CLEAN_MODE")
echo "Workspace: $WORKSPACE"
echo ""

# Step 2: Fetch run metadata
echo "Step 2: Fetching workflow run metadata..."
METADATA="$WORKSPACE/run-metadata.json"
if [ ! -f "$METADATA" ]; then
    gh run view "$RUN_ID" --json conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle > "$METADATA"
    echo "✅ Downloaded run metadata"
else
    echo "✅ Using cached run metadata"
fi

echo ""
echo "=== Run Metadata ==="
cat "$METADATA" | jq '{
    workflowName,
    conclusion,
    status,
    event,
    headBranch,
    url,
    displayTitle
}'
echo ""

# Step 3: Get jobs details
echo "Step 3: Fetching job details..."
JOBS="$WORKSPACE/jobs-detailed.json"
if [ ! -f "$JOBS" ]; then
    gh api "/repos/dotCMS/core/actions/runs/$RUN_ID/jobs" --paginate > "$JOBS"
    echo "✅ Downloaded job details"
else
    echo "✅ Using cached job details"
fi

# Find failed jobs
echo ""
echo "=== Failed Jobs ==="
FAILED_JOBS=$(jq -r '.jobs[] | select(.conclusion == "failure") | {name, id, conclusion} | @json' "$JOBS")

if [ -z "$FAILED_JOBS" ]; then
    echo "❌ No failed jobs found in this run"
    CONCLUSION=$(jq -r '.conclusion' "$METADATA")
    echo "Run conclusion: $CONCLUSION"
    exit 0
fi

echo "$FAILED_JOBS" | jq -r '. | "- \(.name) (ID: \(.id))"'
echo ""

# Step 4: Download first failed job logs
FAILED_JOB_ID=$(echo "$FAILED_JOBS" | head -1 | jq -r '.id')
FAILED_JOB_NAME=$(echo "$FAILED_JOBS" | head -1 | jq -r '.name')

echo "=== Analyzing Failed Job ==="
echo "Job: $FAILED_JOB_NAME"
echo "ID: $FAILED_JOB_ID"
echo ""

LOG_FILE="$WORKSPACE/failed-job-${FAILED_JOB_ID}.txt"

if [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
    echo "Downloading job logs..."
    gh api "/repos/dotCMS/core/actions/jobs/$FAILED_JOB_ID/logs" > "$LOG_FILE"
    echo "✅ Downloaded: $(wc -c < "$LOG_FILE" | numfmt --to=iec-i)B"
else
    echo "✅ Using cached logs: $(wc -c < "$LOG_FILE" | numfmt --to=iec-i)B"
fi
echo ""

# Step 5: Present evidence
echo "Step 5: Extracting diagnostic evidence..."
echo ""

# Check log size
LOG_SIZE=$(wc -c < "$LOG_FILE")
LOG_SIZE_MB=$((LOG_SIZE / 1048576))

echo "Log size: ${LOG_SIZE_MB}MB"

if [ "$LOG_SIZE" -gt 10485760 ]; then
    echo "⚠️  Large log detected - extracting error sections only..."
    ERROR_FILE="$WORKSPACE/error-sections.txt"
    extract_error_sections_only "$LOG_FILE" "$ERROR_FILE"
    LOG_TO_ANALYZE="$ERROR_FILE"
    echo "✅ Error sections extracted: $(wc -c < "$ERROR_FILE" | numfmt --to=iec-i)B"
else
    LOG_TO_ANALYZE="$LOG_FILE"
    echo "✅ Full log will be analyzed"
fi
echo ""

# Present complete evidence
echo "=== Generating Evidence Package ==="
EVIDENCE_FILE="$WORKSPACE/evidence.txt"
present_complete_diagnostic "$LOG_TO_ANALYZE" > "$EVIDENCE_FILE"
echo "✅ Evidence saved to: $EVIDENCE_FILE"
echo ""

# Display evidence summary
echo "=== Evidence Summary ==="
head -100 "$EVIDENCE_FILE"
echo ""
echo "... (see $EVIDENCE_FILE for complete evidence) ..."
echo ""

# Step 6: Save analysis metadata
cat > "$WORKSPACE/analysis-metadata.json" <<EOF
{
  "run_id": "$RUN_ID",
  "workspace": "$WORKSPACE",
  "failed_job_id": "$FAILED_JOB_ID",
  "failed_job_name": "$FAILED_JOB_NAME",
  "log_file": "$LOG_FILE",
  "log_size_bytes": $LOG_SIZE,
  "evidence_file": "$EVIDENCE_FILE",
  "analysis_timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF

echo "=== Diagnostic Complete ==="
echo ""
echo "Workspace: $WORKSPACE"
echo "Evidence: $EVIDENCE_FILE"
echo ""
echo "Next: AI will analyze the evidence and generate comprehensive report"
echo ""

# Return workspace path for AI to continue analysis
echo "$WORKSPACE"