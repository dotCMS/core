#!/bin/bash

# Graceful Shutdown Script for dotCMS Docker Environment
# This script ensures proper shutdown ordering to prevent database connection errors

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Configuration
DOTCMS_CONTAINER="${DOTCMS_CONTAINER:-dotcms}"
DB_CONTAINER="${DB_CONTAINER:-db}"
OPENSEARCH_CONTAINER="${OPENSEARCH_CONTAINER:-opensearch}"
COMPOSE_FILE="${COMPOSE_FILE:-docker/docker-compose-examples/single-node/docker-compose.yml}"
SHUTDOWN_TIMEOUT="${SHUTDOWN_TIMEOUT:-60}"
HEALTH_CHECK_INTERVAL="${HEALTH_CHECK_INTERVAL:-5}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to check if container is running
is_container_running() {
    local container_name="$1"
    docker-compose -f "$COMPOSE_FILE" ps -q "$container_name" 2>/dev/null | xargs docker inspect -f '{{.State.Running}}' 2>/dev/null | grep -q "true"
}

# Function to check if dotCMS is responding
is_dotcms_healthy() {
    local container_name="$1"
    docker-compose -f "$COMPOSE_FILE" exec -T "$container_name" curl -f -s http://localhost:8082/api/v1/appconfiguration >/dev/null 2>&1
}

# Function to wait for container to stop
wait_for_container_stop() {
    local container_name="$1"
    local timeout="$2"
    local count=0
    
    log_info "Waiting for $container_name to stop (timeout: ${timeout}s)..."
    
    while [ $count -lt $timeout ]; do
        if ! is_container_running "$container_name"; then
            log_success "$container_name has stopped"
            return 0
        fi
        sleep 1
        count=$((count + 1))
    done
    
    log_warn "$container_name did not stop within ${timeout}s"
    return 1
}

# Function to gracefully shutdown dotCMS
shutdown_dotcms() {
    log_info "Initiating graceful shutdown of dotCMS..."
    
    # Send SIGTERM to dotCMS container
    if is_container_running "$DOTCMS_CONTAINER"; then
        log_info "Sending SIGTERM to dotCMS container..."
        docker-compose -f "$COMPOSE_FILE" kill -s TERM "$DOTCMS_CONTAINER"
        
        # Wait for dotCMS to complete its shutdown process
        local count=0
        local max_wait=$((SHUTDOWN_TIMEOUT))
        
        log_info "Waiting for dotCMS to complete shutdown (max ${max_wait}s)..."
        
        while [ $count -lt $max_wait ]; do
            if ! is_container_running "$DOTCMS_CONTAINER"; then
                log_success "dotCMS has shut down gracefully"
                return 0
            fi
            
            # Check if dotCMS is still responding (indicates shutdown in progress)
            if is_dotcms_healthy "$DOTCMS_CONTAINER"; then
                log_info "dotCMS is still processing shutdown... (${count}s elapsed)"
            else
                log_info "dotCMS is no longer responding, waiting for container to stop... (${count}s elapsed)"
            fi
            
            sleep $HEALTH_CHECK_INTERVAL
            count=$((count + HEALTH_CHECK_INTERVAL))
        done
        
        log_warn "dotCMS did not shut down within ${max_wait}s, forcing stop..."
        docker-compose -f "$COMPOSE_FILE" stop "$DOTCMS_CONTAINER"
    else
        log_info "dotCMS container is not running"
    fi
}

# Function to shutdown database
shutdown_database() {
    log_info "Shutting down database..."
    
    if is_container_running "$DB_CONTAINER"; then
        # Give PostgreSQL time to finish any remaining operations
        log_info "Sending SIGTERM to database container..."
        docker-compose -f "$COMPOSE_FILE" kill -s TERM "$DB_CONTAINER"
        
        # Wait for database to stop gracefully
        if ! wait_for_container_stop "$DB_CONTAINER" 30; then
            log_warn "Forcing database stop..."
            docker-compose -f "$COMPOSE_FILE" stop "$DB_CONTAINER"
        fi
    else
        log_info "Database container is not running"
    fi
}

# Function to shutdown OpenSearch
shutdown_opensearch() {
    log_info "Shutting down OpenSearch..."
    
    if is_container_running "$OPENSEARCH_CONTAINER"; then
        docker-compose -f "$COMPOSE_FILE" kill -s TERM "$OPENSEARCH_CONTAINER"
        
        if ! wait_for_container_stop "$OPENSEARCH_CONTAINER" 30; then
            log_warn "Forcing OpenSearch stop..."
            docker-compose -f "$COMPOSE_FILE" stop "$OPENSEARCH_CONTAINER"
        fi
    else
        log_info "OpenSearch container is not running"
    fi
}

# Main shutdown function
main() {
    log_info "Starting graceful shutdown of dotCMS environment..."
    log_info "Using compose file: $COMPOSE_FILE"
    
    cd "$PROJECT_DIR"
    
    # Check if compose file exists
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Compose file not found: $COMPOSE_FILE"
        exit 1
    fi
    
    # Phase 1: Shutdown dotCMS first (this will drain requests and shutdown components)
    shutdown_dotcms
    
    # Phase 2: Shutdown supporting services after dotCMS is completely down
    log_info "dotCMS shutdown complete, now shutting down supporting services..."
    
    # Shutdown OpenSearch (less critical)
    shutdown_opensearch
    
    # Phase 3: Shutdown database last
    shutdown_database
    
    # Final cleanup - remove containers if requested
    if [ "${REMOVE_CONTAINERS:-false}" = "true" ]; then
        log_info "Removing containers..."
        docker-compose -f "$COMPOSE_FILE" rm -f
    fi
    
    log_success "Graceful shutdown completed!"
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: $0 [options]"
        echo ""
        echo "Options:"
        echo "  --help, -h          Show this help message"
        echo "  --remove            Remove containers after shutdown"
        echo ""
        echo "Environment Variables:"
        echo "  DOTCMS_CONTAINER    Name of dotCMS container (default: dotcms)"
        echo "  DB_CONTAINER        Name of database container (default: db)"
        echo "  OPENSEARCH_CONTAINER Name of OpenSearch container (default: opensearch)"
        echo "  COMPOSE_FILE        Path to docker-compose file"
        echo "  SHUTDOWN_TIMEOUT    Max time to wait for dotCMS shutdown (default: 60s)"
        echo "  HEALTH_CHECK_INTERVAL Check interval in seconds (default: 5s)"
        echo ""
        echo "Examples:"
        echo "  $0                  # Standard graceful shutdown"
        echo "  $0 --remove         # Shutdown and remove containers"
        echo "  SHUTDOWN_TIMEOUT=90 $0  # Use 90s timeout"
        exit 0
        ;;
    --remove)
        REMOVE_CONTAINERS=true
        ;;
    "")
        # No arguments, proceed with default behavior
        ;;
    *)
        log_error "Unknown argument: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac

# Run main function
main 