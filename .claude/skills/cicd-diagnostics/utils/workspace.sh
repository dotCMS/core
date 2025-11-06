#!/bin/bash
# Diagnostic Workspace Management Utilities
# Handles creation, caching, and organization of diagnostic artifacts

set -euo pipefail

# Create diagnostic workspace with timestamp
# Usage: create_diagnostic_workspace RUN_ID
# Returns: Path to diagnostic directory
create_diagnostic_workspace() {
    local run_id="$1"
    local diagnostic_dir=".claude/diagnostics/run-${run_id}-$(date +%Y%m%d-%H%M%S)"

    mkdir -p "$diagnostic_dir"
    echo "$diagnostic_dir"
}

# Find existing diagnostic workspace for a run ID
# Usage: find_existing_diagnostic RUN_ID
# Returns: Path to existing directory or empty string
find_existing_diagnostic() {
    local run_id="$1"
    find .claude/diagnostics -maxdepth 1 -type d -name "run-${run_id}-*" 2>/dev/null | head -1
}

# Get or create diagnostic workspace (with caching)
# Usage: get_diagnostic_workspace RUN_ID
# Returns: Path to diagnostic directory (existing or new)
get_diagnostic_workspace() {
    local run_id="$1"

    local existing_dir=$(find_existing_diagnostic "$run_id")

    if [ -n "$existing_dir" ]; then
        echo "✓ Found existing diagnostic session: $existing_dir" >&2
        echo "  (Logs and metadata will be reused from previous analysis)" >&2
        echo "$existing_dir"
    else
        local new_dir=$(create_diagnostic_workspace "$run_id")
        echo "✓ Created new diagnostic workspace: $new_dir" >&2
        echo "$new_dir"
    fi
}

# Save artifact to diagnostic workspace
# Usage: save_artifact DIAGNOSTIC_DIR FILENAME CONTENT
save_artifact() {
    local diagnostic_dir="$1"
    local filename="$2"
    local content="$3"

    echo "$content" > "$diagnostic_dir/$filename"
}

# Check if artifact exists in workspace
# Usage: artifact_exists DIAGNOSTIC_DIR FILENAME
# Returns: 0 if exists, 1 if not
artifact_exists() {
    local diagnostic_dir="$1"
    local filename="$2"

    [ -f "$diagnostic_dir/$filename" ] && [ -s "$diagnostic_dir/$filename" ]
}

# Get cached artifact or fetch new
# Usage: get_or_fetch_artifact DIAGNOSTIC_DIR FILENAME FETCH_COMMAND
# Returns: Path to artifact file
get_or_fetch_artifact() {
    local diagnostic_dir="$1"
    local filename="$2"
    local fetch_command="$3"

    local artifact_path="$diagnostic_dir/$filename"

    if artifact_exists "$diagnostic_dir" "$filename"; then
        echo "✓ Using cached artifact: $filename" >&2
        echo "$artifact_path"
    else
        echo "Fetching $filename..." >&2
        eval "$fetch_command" > "$artifact_path"
        echo "✓ Saved to: $artifact_path" >&2
        echo "$artifact_path"
    fi
}

# Ensure .gitignore includes diagnostic directories
# Usage: ensure_gitignore_diagnostics
ensure_gitignore_diagnostics() {
    if [ ! -f .gitignore ] || ! grep -q "\.claude/diagnostics/" .gitignore 2>/dev/null; then
        echo "" >> .gitignore
        echo "# Claude Code diagnostic outputs" >> .gitignore
        echo ".claude/diagnostics/" >> .gitignore
        echo "✓ Added .claude/diagnostics/ to .gitignore" >&2
    fi
}

# List all diagnostic workspaces
# Usage: list_diagnostic_workspaces
list_diagnostic_workspaces() {
    find .claude/diagnostics -maxdepth 1 -type d -name "run-*" 2>/dev/null | sort -r
}

# Get workspace age in hours
# Usage: get_workspace_age DIAGNOSTIC_DIR
# Returns: Age in hours
get_workspace_age() {
    local diagnostic_dir="$1"

    if [ ! -d "$diagnostic_dir" ]; then
        echo "-1"
        return
    fi

    local dir_timestamp=$(stat -f %m "$diagnostic_dir" 2>/dev/null || stat -c %Y "$diagnostic_dir" 2>/dev/null)
    local current_timestamp=$(date +%s)
    local age_seconds=$((current_timestamp - dir_timestamp))
    local age_hours=$((age_seconds / 3600))

    echo "$age_hours"
}

# Clean old diagnostic workspaces
# Usage: clean_old_diagnostics [MAX_AGE_HOURS] [MAX_COUNT]
# Defaults: 168 hours (7 days), keep 50 most recent
clean_old_diagnostics() {
    local max_age_hours="${1:-168}"
    local max_count="${2:-50}"

    echo "Cleaning diagnostic workspaces older than $max_age_hours hours..." >&2

    local workspaces=$(list_diagnostic_workspaces)
    local count=0
    local removed=0

    while IFS= read -r workspace; do
        [ -z "$workspace" ] && continue

        count=$((count + 1))
        local age=$(get_workspace_age "$workspace")

        if [ "$age" -ge "$max_age_hours" ] || [ "$count" -gt "$max_count" ]; then
            echo "  Removing: $workspace (age: ${age}h)" >&2
            rm -rf "$workspace"
            removed=$((removed + 1))
        fi
    done <<< "$workspaces"

    echo "✓ Cleaned $removed old diagnostic workspace(s)" >&2
}

# Get workspace summary
# Usage: get_workspace_summary DIAGNOSTIC_DIR
get_workspace_summary() {
    local diagnostic_dir="$1"

    if [ ! -d "$diagnostic_dir" ]; then
        echo "Workspace not found: $diagnostic_dir" >&2
        return 1
    fi

    echo "=== Diagnostic Workspace Summary ==="
    echo "Path: $diagnostic_dir"
    echo "Age: $(get_workspace_age "$diagnostic_dir") hours"
    echo "Size: $(du -sh "$diagnostic_dir" | cut -f1)"
    echo "Files:"
    ls -lh "$diagnostic_dir" | tail -n +2 | awk '{printf "  %-40s %10s\n", $9, $5}'
}

# Create standard diagnostic file structure
# Usage: init_diagnostic_structure DIAGNOSTIC_DIR
init_diagnostic_structure() {
    local diagnostic_dir="$1"

    mkdir -p "$diagnostic_dir"
    touch "$diagnostic_dir/error-summary.txt"
    touch "$diagnostic_dir/analysis-notes.txt"

    echo "✓ Initialized diagnostic structure in $diagnostic_dir" >&2
}