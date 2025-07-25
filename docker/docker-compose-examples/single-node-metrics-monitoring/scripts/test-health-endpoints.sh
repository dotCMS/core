#!/bin/bash

# Health Endpoints Testing Script for dotCMS Metrics Monitoring Stack
# Tests all health check endpoints and provides timing information

set -e

MANAGEMENT_URL="${MANAGEMENT_URL:-http://localhost:8090}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to test endpoint with timing
test_endpoint() {
    local url="$1"
    local expected_status="$2"
    local description="$3"
    local expected_response="$4"
    
    echo -n "Testing $description..."
    
    # Capture both response time and content
    local response_file=$(mktemp)
    local response_time=$(curl -w "%{time_total}" -s -o "$response_file" -w "%{http_code}:%{time_total}" "$url" 2>/dev/null || echo "000:999")
    
    local status_code=$(echo "$response_time" | cut -d: -f1)
    local time_total=$(echo "$response_time" | cut -d: -f2)
    local response_content=$(cat "$response_file")
    
    rm -f "$response_file"
    
    if [[ "$status_code" == "$expected_status" ]]; then
        if [[ -n "$expected_response" ]]; then
            if [[ "$response_content" == "$expected_response" ]]; then
                printf " ${GREEN}✓${NC} (${time_total}s)\n"
                return 0
            else
                printf " ${RED}✗${NC} Wrong response: got '$response_content', expected '$expected_response'\n"
                return 1
            fi
        else
            printf " ${GREEN}✓${NC} (${time_total}s)\n"
            return 0
        fi
    else
        printf " ${RED}✗${NC} Status: $status_code (expected $expected_status)\n"
        [[ -n "$response_content" ]] && echo "Response: $response_content"
        return 1
    fi
}

# Function to test JSON endpoint
test_json_endpoint() {
    local url="$1"
    local description="$2"
    
    echo -n "Testing $description..."
    
    local response=$(curl -s -w "%{http_code}:%{time_total}" "$url" 2>/dev/null || echo "000:999")
    local status_code=$(echo "$response" | tail -1 | cut -d: -f1)
    local time_total=$(echo "$response" | tail -1 | cut -d: -f2)
    local content=$(echo "$response" | head -n -1)
    
    if [[ "$status_code" =~ ^(200|503)$ ]]; then
        # Try to validate JSON
        if echo "$content" | python3 -m json.tool >/dev/null 2>&1; then
            printf " ${GREEN}✓${NC} JSON (${time_total}s)\n"
            return 0
        else
            printf " ${YELLOW}⚠${NC} Invalid JSON (${time_total}s)\n"
            return 1
        fi
    else
        printf " ${RED}✗${NC} Status: $status_code\n"
        return 1
    fi
}

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  dotCMS Health Endpoints Test Suite     ${NC}"
echo -e "${BLUE}===========================================${NC}"
echo

# Test dotCMS Health Endpoints
echo -e "${YELLOW}📋 Testing dotCMS Health Endpoints${NC}"
echo "Management URL: $MANAGEMENT_URL"
echo

test_endpoint "$MANAGEMENT_URL/dotmgt/livez" "200" "Liveness Probe (/dotmgt/livez)" "alive"
test_endpoint "$MANAGEMENT_URL/dotmgt/readyz" "200" "Readiness Probe (/dotmgt/readyz)" "ready"
test_json_endpoint "$MANAGEMENT_URL/dotmgt/health" "Health Details (/dotmgt/health)"

echo

# Test Metrics Endpoint
echo -e "${YELLOW}📊 Testing Metrics Endpoint${NC}"
test_endpoint "$MANAGEMENT_URL/dotmgt/metrics" "200" "Prometheus Metrics (/dotmgt/metrics)"

echo

# Test Infrastructure Services
echo -e "${YELLOW}🔧 Testing Infrastructure Services${NC}"
echo "Prometheus URL: $PROMETHEUS_URL"
echo "Grafana URL: $GRAFANA_URL"
echo

test_endpoint "$PROMETHEUS_URL/-/healthy" "200" "Prometheus Health"
test_endpoint "$PROMETHEUS_URL/api/v1/targets" "200" "Prometheus Targets API"
test_endpoint "$GRAFANA_URL/api/health" "200" "Grafana Health"

echo

# Performance Summary
echo -e "${YELLOW}⚡ Performance Expectations${NC}"
echo "• Liveness checks should be < 100ms"
echo "• Readiness checks should be < 500ms" 
echo "• Metrics collection should be < 5s"
echo "• JSON health details should be < 1s"

echo

# Docker Health Check Test
echo -e "${YELLOW}🐳 Testing Docker Health Status${NC}"
if command -v docker-compose >/dev/null 2>&1; then
    if [[ -f "docker-compose.yml" ]]; then
        echo "Checking container health status..."
        docker-compose ps --format "table {{.Name}}\t{{.State}}\t{{.Status}}"
    else
        echo "docker-compose.yml not found in current directory"
    fi
else
    echo "docker-compose not available"
fi

echo

# Summary
echo -e "${BLUE}===========================================${NC}"
echo -e "${GREEN}✅ Health endpoints test completed!${NC}"
echo -e "${BLUE}===========================================${NC}"
echo
echo "Next steps:"
echo "1. Check Prometheus targets: $PROMETHEUS_URL/targets"
echo "2. View Grafana dashboard: $GRAFANA_URL/d/dotcms-overview"
echo "3. Monitor health status: watch curl -s $MANAGEMENT_URL/dotmgt/readyz" 