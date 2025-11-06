#!/usr/bin/env bash
# Fetch workflow metadata with caching (bash/zsh compatible)

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <RUN_ID> <WORKSPACE>"
    exit 1
fi

RUN_ID="$1"
WORKSPACE="$2"

# Validate inputs
if [ -z "$WORKSPACE" ]; then
    echo "ERROR: WORKSPACE parameter is required" >&2
    exit 1
fi

METADATA="$WORKSPACE/run-metadata.json"

# Fetch metadata if not cached
if [ ! -f "$METADATA" ]; then
    echo "Fetching run metadata..."
    gh run view "$RUN_ID" --json conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle > "$METADATA"
    echo "✓ Metadata saved to $METADATA"
else
    echo "✓ Using cached metadata: $METADATA"
fi

# Display metadata
cat "$METADATA" | jq '.'