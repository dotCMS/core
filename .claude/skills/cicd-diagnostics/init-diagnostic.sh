#!/usr/bin/env bash
# Initialize diagnostic environment
# Usage: source init-diagnostic.sh <RUN_ID>
# Returns: Sets WORKSPACE variable and loads all utilities

# Get run ID from argument or exit
if [ -z "$1" ]; then
    echo "ERROR: Run ID required" >&2
    echo "Usage: source init-diagnostic.sh <RUN_ID>" >&2
    return 1 2>/dev/null || exit 1
fi

export RUN_ID="$1"

# Get repository root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || echo ".")

# Load all utility functions
source "$REPO_ROOT/.claude/skills/cicd-diagnostics/utils/workspace.sh"
source "$REPO_ROOT/.claude/skills/cicd-diagnostics/utils/github-api.sh"
source "$REPO_ROOT/.claude/skills/cicd-diagnostics/utils/evidence.sh"
source "$REPO_ROOT/.claude/skills/cicd-diagnostics/utils/utilities.sh"

# Verify utilities loaded
if ! command -v get_diagnostic_workspace &> /dev/null; then
    echo "ERROR: Utilities not loaded correctly" >&2
    return 1 2>/dev/null || exit 1
fi

# Create workspace and export
export WORKSPACE=$(get_diagnostic_workspace "$RUN_ID")

echo "✅ Diagnostic environment initialized"
echo "   RUN_ID: $RUN_ID"
echo "   WORKSPACE: $WORKSPACE"