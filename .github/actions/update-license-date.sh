#!/bin/bash

# Script to update the LICENSE file Change Date line with current date
# Format: "Change Date:     Four years from Month DD, YYYY"

# Find the git repository root directory
REPO_ROOT=$(git rev-parse --show-toplevel)
if [ $? -ne 0 ]; then
    echo "Error: Not in a git repository"
    exit 1
fi

LICENSE_FILE="${REPO_ROOT}/LICENSE"

# Get current date in the required format (e.g., "June 01, 2025")
CURRENT_DATE=$(date "+%B %d, %Y")

# Create the new line with the current date
NEW_LINE="Change Date:     Four years from $CURRENT_DATE"

# Check if a Change Date line exists (case-insensitive)
if grep -qi '^Change Date:' "$LICENSE_FILE"; then
    # Replace the line (case-insensitive)
    awk -v new_line="$NEW_LINE" 'BEGIN{IGNORECASE=1} {if ($0 ~ /^Change Date:/) print new_line; else print $0}' "$LICENSE_FILE" > "${LICENSE_FILE}.tmp" && mv "${LICENSE_FILE}.tmp" "$LICENSE_FILE"
    RESULT=$?
else
    # Append the new line if not found
    echo "$NEW_LINE" >> "$LICENSE_FILE"
    RESULT=$?
fi

if [ $RESULT -eq 0 ]; then
    echo "Successfully updated LICENSE file"
else
    echo "Error updating LICENSE file"
    exit 1
fi