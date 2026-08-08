#!/bin/bash
# Experiments Docker Compose Startup Script
#
# Usage:
#   ./start-experiments.sh                      # Full stack (default)
#   ./start-experiments.sh --experiments-only   # Experiments only
#   ./start-experiments.sh --force-recreate     # Full stack, recreate containers
#   ./start-experiments.sh --help               # Show this help

set -e

show_help() {
    echo "Experiments Docker Compose Startup Script"
    echo ""
    echo "Usage:"
    echo "  ./start-experiments.sh                                      Start full stack (default)"
    echo "  ./start-experiments.sh --experiments-only                   Start experiments services only"
    echo "  ./start-experiments.sh --force-recreate                     Start full stack, recreate containers"
    echo "  ./start-experiments.sh --experiments-only --force-recreate  Experiments only, recreate containers"
    echo "  ./start-experiments.sh --help                               Show this help message"
    echo ""
    echo "Options:"
    echo "  --force-recreate      Force recreate containers (required for environment variable changes)"
    echo ""
    echo "Experiments Services:"
    echo "  - PostgreSQL (experiments data)"
    echo "  - Keycloak (authentication)"
    echo "  - dotCMS Experiments API"
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
EXPERIMENTS_ONLY=false
FORCE_RECREATE=false

for arg in "$@"; do
    case $arg in
        --help|-h)
            show_help
            exit 0
            ;;
        --experiments-only)
            EXPERIMENTS_ONLY=true
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

# Build docker compose command (v2 plugin; fallback to legacy docker-compose)
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi
if [[ "$EXPERIMENTS_ONLY" == "false" ]]; then
    COMPOSE_CMD="$COMPOSE_CMD --profile full"
fi

COMPOSE_CMD="$COMPOSE_CMD up -d"
if [[ "$FORCE_RECREATE" == "true" ]]; then
    COMPOSE_CMD="$COMPOSE_CMD --force-recreate"
fi

# Execute the appropriate command
if [[ "$EXPERIMENTS_ONLY" == "true" ]]; then
    if [[ "$FORCE_RECREATE" == "true" ]]; then
        echo "📊 Starting Experiments stack only (force recreating containers)..."
        echo "⚠️  This will recreate containers to pick up environment variable changes."
    else
        echo "📊 Starting Experiments stack only..."
    fi
else
    if [[ "$FORCE_RECREATE" == "true" ]]; then
        echo "🚀 Starting full Experiments + dotCMS stack (force recreating containers)..."
        echo "⚠️  This will recreate containers to pick up environment variable changes."
    else
        echo "🚀 Starting full Experiments + dotCMS stack..."
    fi
fi

eval $COMPOSE_CMD

echo "✅ Stack started successfully!"
echo ""
echo "📋 Services running:"
$COMPOSE_CMD ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "🌐 Access URLs:"
echo "  - Keycloak Admin: http://localhost:61111 (admin:keycloak)"
echo "  - Experiments API: http://localhost:8088"
echo "  - Cube Analytics: http://localhost:4001"
echo "  - Jitsu Events: http://localhost:8081"
echo "  - ClickHouse: http://localhost:8124"

if [[ "$1" != "--experiments-only" ]]; then
    echo "  - dotCMS: http://localhost:8082 (admin:admin)"
    echo "  - Glowroot: http://localhost:4000"
fi