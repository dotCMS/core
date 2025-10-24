#!/bin/bash

# Validate Mermaid diagrams in markdown files
# Usage: ./scripts/validate-mermaid.sh <file>

set -e

FILE="$1"

if [ -z "$FILE" ]; then
    echo "Usage: $0 <markdown-file>"
    exit 1
fi

if [ ! -f "$FILE" ]; then
    echo "Error: File '$FILE' not found"
    exit 1
fi

echo "🔍 Validating Mermaid diagrams in: $FILE"
echo ""

ERRORS=0

# Check for @ symbols in mermaid blocks (except in sequence diagram messages)
echo "Checking for @ symbols..."
if awk '/```mermaid/,/```/ {if (/@[a-zA-Z]/ && !/participant/ && !/(-->>|->>)/) print NR": "$0}' "$FILE" | grep -q .; then
    echo "❌ Found @ symbols in Mermaid node text:"
    awk '/```mermaid/,/```/ {if (/@[a-zA-Z]/ && !/participant/ && !/(-->>|->>)/) print "  Line "NR": "$0}' "$FILE"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ No problematic @ symbols found"
fi
echo ""

# Check for curly braces in node text
echo "Checking for curly braces in node text..."
if awk '/```mermaid/,/```/ {if (/\{[a-zA-Z_]+\}/ && !/sequenceDiagram/) print NR": "$0}' "$FILE" | grep -q .; then
    echo "❌ Found curly braces in Mermaid node text:"
    awk '/```mermaid/,/```/ {if (/\{[a-zA-Z_]+\}/ && !/sequenceDiagram/) print "  Line "NR": "$0}' "$FILE"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ No problematic curly braces found"
fi
echo ""

# Check for pipe characters used as line breaks (not in edge labels)
echo "Checking for pipe characters used for line breaks..."
if awk '/```mermaid/,/```/ {if (/[A-Z]\{[^}]+\|[^}]+\}/) print NR": "$0}' "$FILE" | grep -q .; then
    echo "⚠️  Found potential pipe character issues (use <br/> instead):"
    awk '/```mermaid/,/```/ {if (/[A-Z]\{[^}]+\|[^}]+\}/) print "  Line "NR": "$0}' "$FILE"
    echo "  Note: Use <br/> for line breaks in decision nodes"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ No problematic pipe characters found"
fi
echo ""

# Summary
if [ $ERRORS -eq 0 ]; then
    echo "✅ All Mermaid diagrams passed validation!"
    exit 0
else
    echo "❌ Found $ERRORS type(s) of issues in Mermaid diagrams"
    echo ""
    echo "Common fixes:"
    echo "  - Replace '@claude' with 'Claude'"
    echo "  - Replace '{username}' with 'USERNAME'"
    echo "  - Replace '|' with '<br/>' for line breaks in nodes"
    exit 1
fi

