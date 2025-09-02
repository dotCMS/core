#!/bin/bash
# Analytics Docker Compose Startup Script
#
# Usage:
#   ./start-analytics.sh                      # Full stack (default)
#   ./start-analytics.sh --analytics-only     # Analytics only
#   ./start-analytics.sh --force-recreate     # Full stack, recreate containers
#   ./start-analytics.sh --help               # Show this help

set -e

show_help() {
    echo "Analytics Docker Compose Startup Script"
    echo ""
    echo "Usage:"
    echo "  ./start-analytics.sh                     Start full stack (default)"
    echo "  ./start-analytics.sh --analytics-only    Start analytics services only"
    echo "  ./start-analytics.sh --force-recreate    Start full stack, recreate containers"
    echo "  ./start-analytics.sh --analytics-only --force-recreate    Analytics only, recreate containers"
    echo "  ./start-analytics.sh --help              Show this help message"
    echo ""
    echo "Options:"
    echo "  --force-recreate      Force recreate containers (required for environment variable changes)"
    echo ""
    echo "Analytics Services:"
    echo "  - PostgreSQL (analytics data)"
    echo "  - Keycloak (authentication)"
    echo "  - dotCMS Analytics API"
    echo "  - Jitsu (event collection)"
    echo "  - Redis (cache)"
    echo "  - Cube (analytics queries)"
    echo "  - ClickHouse (data warehouse)"
    echo ""
    echo "dotCMS Services (included by default):"
    echo "  - dotCMS application"
    echo "  - PostgreSQL (dotCMS data)"
    echo "  - OpenSearch (search engine)"
    echo ""
    echo "Note: Use --force-recreate when environment variables have changed."
    echo "Access URLs will be displayed after startup."
}

# Parse arguments
ANALYTICS_ONLY=false
FORCE_RECREATE=false

for arg in "$@"; do
    case $arg in
        --help|-h)
            show_help
            exit 0
            ;;
        --analytics-only)
            ANALYTICS_ONLY=true
            ;;
        --force-recreate)
            FORCE_RECREATE=true
            ;;
        *)
            echo "Unknown argument: $arg"
            echo "Use --help for usage information."
            exit 1
            ;;
    esac
done

# Build docker-compose command
COMPOSE_CMD="docker-compose"
if [[ "$ANALYTICS_ONLY" == "false" ]]; then
    COMPOSE_CMD="$COMPOSE_CMD --profile full"
fi

COMPOSE_CMD="$COMPOSE_CMD up -d"
if [[ "$FORCE_RECREATE" == "true" ]]; then
    COMPOSE_CMD="$COMPOSE_CMD --force-recreate"
fi

# Execute the appropriate command
if [[ "$ANALYTICS_ONLY" == "true" ]]; then
    if [[ "$FORCE_RECREATE" == "true" ]]; then
        echo "📊 Starting analytics stack only (force recreating containers)..."
        echo "⚠️  This will recreate containers to pick up environment variable changes."
    else
        echo "📊 Starting analytics stack only..."
    fi
else
    if [[ "$FORCE_RECREATE" == "true" ]]; then
        echo "🚀 Starting full analytics + dotCMS stack (force recreating containers)..."
        echo "⚠️  This will recreate containers to pick up environment variable changes."
    else
        echo "🚀 Starting full analytics + dotCMS stack..."
    fi
fi

eval $COMPOSE_CMD

echo "✅ Stack started successfully!"
echo ""
echo "📋 Services running:"
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "🌐 Access URLs:"
echo "  - Keycloak Admin: http://localhost:61111 (admin:keycloak)"
echo "  - Analytics API: http://localhost:8088"
echo "  - Cube Analytics: http://localhost:4001"
echo "  - Jitsu Events: http://localhost:8081"
echo "  - ClickHouse: http://localhost:8124"

if [[ "$1" != "--analytics-only" ]]; then
    echo "  - dotCMS: http://localhost:8082 (admin:admin)"
    echo "  - Glowroot: http://localhost:4000"
fi