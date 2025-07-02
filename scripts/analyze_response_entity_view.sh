#!/bin/bash

# Script to analyze REST resources using ResponseEntityView and their @ApiResponse annotations

echo "=== REST Resources Using ResponseEntityView Analysis ==="
echo ""

# Find all REST resource files using ResponseEntityView
FILES=$(find /Users/stevebolton/git/core-baseline/dotCMS/src/main/java -name "*.java" -type f -exec grep -l "@Path" {} \; | xargs grep -l "ResponseEntityView")

echo "Found $(echo "$FILES" | wc -l) REST resource files using ResponseEntityView:"
echo ""

for file in $FILES; do
    echo "=== File: $file ==="
    
    # Get the relative path from core-baseline
    RELATIVE_PATH=$(echo "$file" | sed 's|/Users/stevebolton/git/core-baseline/||')
    echo "Relative path: $RELATIVE_PATH"
    
    # Find methods returning ResponseEntityView
    echo ""
    echo "Methods returning ResponseEntityView:"
    grep -n "Response\.ok(new ResponseEntityView" "$file" | while read -r line; do
        LINE_NUM=$(echo "$line" | cut -d: -f1)
        echo "  Line $LINE_NUM: $line"
        
        # Look for the method signature above this line
        METHOD_LINE=$(awk -v n="$LINE_NUM" 'NR < n && /public Response/ {line=NR; content=$0} END {print line ": " content}' "$file")
        if [[ -n "$METHOD_LINE" ]]; then
            echo "    Method: $METHOD_LINE"
        fi
    done
    
    # Count @ApiResponse annotations
    API_RESPONSE_COUNT=$(grep -c "@ApiResponse" "$file")
    echo ""
    echo "@ApiResponse annotations found: $API_RESPONSE_COUNT"
    
    # Check for content specification in @ApiResponse
    CONTENT_SPECS=$(grep -c "content = @Content" "$file")
    echo "@ApiResponse with content specs: $CONTENT_SPECS"
    
    echo ""
    echo "---"
    echo ""
done