#!/bin/bash

# Request Draining Test Script with Authentication
# This script tests the request draining functionality with admin credentials

DOTCMS_URL="${DOTCMS_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin@dotcms.com}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
DURATION="${DURATION:-15000}"
NUM_REQUESTS="${NUM_REQUESTS:-3}"

echo "=== dotCMS Request Draining Test (Authenticated) ==="
echo "URL: $DOTCMS_URL"
echo "User: $ADMIN_USER"
echo "Request Duration: ${DURATION}ms"
echo "Number of Requests: $NUM_REQUESTS"
echo ""

# Function to get authentication token
get_auth_token() {
    echo "Getting authentication token..."
    
    # Login to get JWT token
    TOKEN_RESPONSE=$(curl -s -X POST \
         -H "Content-Type: application/json" \
         -d "{\"user\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" \
         "$DOTCMS_URL/api/v1/authentication/api-token")
    
    # Extract token from response
    TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['entity']['token'])" 2>/dev/null)
    
    if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
        echo "✓ Authentication successful"
        echo "$TOKEN"
    else
        echo "✗ Authentication failed"
        echo "Response: $TOKEN_RESPONSE"
        return 1
    fi
}

# Function to start a long-running request
start_long_request() {
    local request_id=$1
    local duration=$2
    local token=$3
    echo "Starting request $request_id (duration: ${duration}ms)..."
    
    curl -s -H "Authorization: Bearer $token" \
         -w "Request $request_id - HTTP %{http_code} - Total time: %{time_total}s\\n" \
         "$DOTCMS_URL/api/v1/system/request-draining-test/long-request?duration=$duration" \
         -o "/tmp/request_${request_id}_response.json" &
    
    echo $!  # Return the process ID
}

# Function to check shutdown status
check_status() {
    local token=$1
    curl -s -H "Authorization: Bearer $token" \
         "$DOTCMS_URL/api/v1/system/request-draining-test/status" 2>/dev/null | \
         python3 -m json.tool 2>/dev/null || echo "Failed to get status"
}

# Function to check health endpoints
check_health() {
    echo "Health Status:"
    echo -n "  /readyz: "
    curl -s "$DOTCMS_URL/readyz" || echo "FAILED"
    echo ""
    echo -n "  /livez: "
    curl -s "$DOTCMS_URL/livez" || echo "FAILED"
    echo ""
}

# Main test execution
main() {
    # Get authentication
    TOKEN=$(get_auth_token)
    if [ $? -ne 0 ]; then
        echo "Cannot proceed without authentication"
        exit 1
    fi
    
    echo ""
    echo "Step 1: Checking initial status..."
    check_status "$TOKEN"
    check_health
    
    echo ""
    echo "Step 2: Starting $NUM_REQUESTS long-running requests..."
    REQUEST_PIDS=()
    for i in $(seq 1 $NUM_REQUESTS); do
        PID=$(start_long_request $i $DURATION "$TOKEN")
        REQUEST_PIDS+=($PID)
        sleep 0.5  # Small delay between requests
    done
    
    echo ""
    echo "Step 3: All requests started. Checking status..."
    sleep 2  # Give requests time to start
    check_status "$COOKIE_JAR"
    check_health
    
    echo ""
    echo "Step 4: Now send SIGTERM to test request draining!"
    echo "Examples:"
    echo "  - Find container: docker ps | grep dotcms"
    echo "  - Stop container: docker stop <container_name>"
    echo "  - Or kill process: kill -TERM <dotcms_pid>"
    echo ""
    echo "Expected behavior:"
    echo "  1. /readyz should immediately fail (503)"
    echo "  2. /livez should continue to return 200"
    echo "  3. Shutdown should wait for these ${NUM_REQUESTS} requests to complete"
    echo "  4. Check logs for request draining messages"
    echo ""
    
    echo "Monitoring requests and health endpoints..."
    echo "Press Ctrl+C to stop monitoring"
    
    # Monitor the test
    MONITOR_COUNT=0
    while [ $MONITOR_COUNT -lt 60 ]; do  # Monitor for up to 60 iterations (5 minutes)
        sleep 5
        MONITOR_COUNT=$((MONITOR_COUNT + 1))
        
        echo ""
        echo "=== Monitor Check #$MONITOR_COUNT ==="
        check_health
        
        # Check if any requests are still running
        RUNNING_COUNT=0
        for PID in "${REQUEST_PIDS[@]}"; do
            if kill -0 $PID 2>/dev/null; then
                RUNNING_COUNT=$((RUNNING_COUNT + 1))
            fi
        done
        
        echo "Active test requests: $RUNNING_COUNT"
        
        if [ $RUNNING_COUNT -eq 0 ]; then
            echo "All test requests completed!"
            break
        fi
    done
    
    echo ""
    echo "Step 5: Final status check..."
    check_status "$TOKEN"
    check_health
    
    # Show request results
    echo ""
    echo "Request Results:"
    for i in $(seq 1 $NUM_REQUESTS); do
        if [ -f "/tmp/request_${i}_response.json" ]; then
            echo "Request $i response:"
            cat "/tmp/request_${i}_response.json" | python3 -m json.tool 2>/dev/null || cat "/tmp/request_${i}_response.json"
            echo ""
        fi
    done
    
    # Cleanup
    rm -f /tmp/request_*_response.json 2>/dev/null
    
    echo ""
    echo "Test completed! Check the dotCMS logs for detailed request draining behavior."
}

# Handle interruption
trap 'echo ""; echo "Test interrupted. Cleaning up..."; rm -f /tmp/request_*_response.json 2>/dev/null; exit 1' INT

# Run the test
main "$@" 