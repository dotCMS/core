#!/bin/bash

# Check if dotCMS container is running
if docker ps --format '{{.Names}}' | grep -q "dotcms"; then
  echo "🛑 Stopping Docker..."
  just dev-stop
else
  echo "▶️  Starting Docker on port ${1:-7070}..."
  just dev-run-debug-suspend ${1:-7070}
fi
