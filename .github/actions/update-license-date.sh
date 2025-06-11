#!/bin/bash

# Script to update the LICENSE file Change Date line with current date
# Format: "Change Date:     Four years from Month DD, YYYY"

LICENSE_FILE="../LICENSE"

# Get current date in the required format (e.g., "June 01, 2025")
CURRENT_DATE=$(date "+%B %d, %Y")

# Find the line containing "Change Date:" and extract the current date
CURRENT_LINE=$(grep "^Change Date:" "$LICENSE_FILE")

if [ -z "$CURRENT_LINE" ]; then
    echo "Error: Could not find 'Change Date:' line in LICENSE file"
    exit 1
fi

# Extract the date from the current line (everything after "Four years from ")
CURRENT_DATE_IN_FILE=$(echo "$CURRENT_LINE" | sed 's/.*Four years from //')

# Check if the date needs to be updated
if [ "$CURRENT_DATE_IN_FILE" = "$CURRENT_DATE" ]; then
    echo "LICENSE file already has the current date: $CURRENT_DATE"
    echo "No update needed."
    exit 0
fi

echo "Updating LICENSE file Change Date from '$CURRENT_DATE_IN_FILE' to '$CURRENT_DATE'"

# Create the new line with the current date
NEW_LINE="Change Date:     Four years from $CURRENT_DATE"

# Update the file using sed to replace the line that starts with "Change Date:"
sed -i.bak "s/^Change Date:.*/Change Date:     Four years from $CURRENT_DATE/" "$LICENSE_FILE"

if [ $? -eq 0 ]; then
    echo "Successfully updated LICENSE file"
    echo "Backup created as ${LICENSE_FILE}.bak"
else
    echo "Error updating LICENSE file"
    exit 1
fi