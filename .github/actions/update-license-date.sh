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

# Update the file using sed to replace the line that starts with "Change Date:"
sed -i "s/^Change Date:.*/$NEW_LINE/" "$LICENSE_FILE"

if [ $? -eq 0 ]; then
    echo "Successfully updated LICENSE file"
else
    echo "Error updating LICENSE file"
    exit 1
fi