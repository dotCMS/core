#!/bin/bash

# dotCMS Docker Stop Script with Extended Timeout
# This script stops dotCMS containers with a 45-second timeout to allow
# for graceful shutdown completion.

set -e

TIMEOUT=${1:-45}
CONTAINER_FILTER="name=dotcms"

echo "🛑 Stopping dotCMS containers with ${TIMEOUT}s timeout..."

# Find running dotCMS containers
CONTAINERS=$(docker ps -q --filter "$CONTAINER_FILTER")

if [ -z "$CONTAINERS" ]; then
    echo "ℹ️  No running dotCMS containers found"
    exit 0
fi

echo "📋 Found containers: $CONTAINERS"

# Stop containers with extended timeout
for container in $CONTAINERS; do
    echo "⏹️  Stopping container $container..."
    docker stop --time="$TIMEOUT" "$container"
    echo "✅ Container $container stopped"
done

echo "🎉 All dotCMS containers stopped successfully"

# Optional: Show recent logs from stopped containers
if [ "$2" = "--logs" ]; then
    echo ""
    echo "📄 Recent logs from stopped containers:"
    for container in $CONTAINERS; do
        echo "--- Container $container ---"
        docker logs --tail=20 "$container" 2>/dev/null || echo "No logs available"
        echo ""
    done
fi 