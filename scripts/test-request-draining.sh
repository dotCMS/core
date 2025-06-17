#!/bin/bash

# Request Draining Test Script
# This script helps test the request draining functionality during shutdown

DOTCMS_URL="${DOTCMS_URL:-http://localhost:8080}"
DURATION="${DURATION:-10000}"
NUM_REQUESTS="${NUM_REQUESTS:-3}"

echo "=== dotCMS Request Draining Test ==="
echo "URL: $DOTCMS_URL"
echo "Request Duration: ${DURATION}ms"
echo "Number of Requests: $NUM_REQUESTS"
echo ""

# Function to start a long-running request
start_long_request() {
    local request_id=$1
    local duration=$2
    echo "Starting request $request_id (duration: ${duration}ms)..."
    
    curl -s -w "Request $request_id - HTTP %{http_code} - Total time: %{time_total}s\n" \
         "$DOTCMS_URL/api/v1/system/request-draining-test/long-request?duration=$duration" \
         -o "/tmp/request_${request_id}_response.json" &
    
    echo $! > "/tmp/request_${request_id}_pid"
}

# Function to check shutdown status
check_status() {
    echo "Checking shutdown status..."
    curl -s "$DOTCMS_URL/api/v1/system/request-draining-test/status" | \
        python3 -m json.tool 2>/dev/null || echo "Failed to get status"
}

# Function to simulate active requests without HTTP
simulate_requests() {
    local count=$1
    echo "Simulating $count active requests..."
    curl -s "$DOTCMS_URL/api/v1/system/request-draining-test/simulate-active-requests?count=$count" | \
        python3 -m json.tool 2>/dev/null || echo "Failed to simulate requests"
}

# Main test function
run_test() {
    echo "Step 1: Starting $NUM_REQUESTS long-running requests..."
    
    # Start multiple long-running requests in parallel
    for i in $(seq 1 $NUM_REQUESTS); do
        start_long_request $i $DURATION
        sleep 1  # Small delay between requests
    done
    
    echo ""
    echo "Step 2: All requests started. Checking initial status..."
    check_status
    
    echo ""
    echo "Step 3: Send SIGTERM to dotCMS process to test request draining"
    echo "Examples:"
    echo "  - Docker: docker stop <container_name>"
    echo "  - Process: kill -TERM <pid>"
    echo "  - Kubernetes: kubectl delete pod <pod_name>"
    echo ""
    echo "Step 4: Monitor the shutdown logs to see request draining in action"
    echo "Expected behavior:"
    echo "  1. Shutdown begins"
    echo "  2. Request draining starts"
    echo "  3. System waits for active requests to complete"
    echo "  4. Shutdown proceeds after requests finish or timeout"
    echo ""
    
    # Wait for requests to complete or be interrupted
    echo "Waiting for requests to complete..."
    for i in $(seq 1 $NUM_REQUESTS); do
        if [ -f "/tmp/request_${i}_pid" ]; then
            pid=$(cat "/tmp/request_${i}_pid")
            wait $pid 2>/dev/null
            rm -f "/tmp/request_${i}_pid"
        fi
    done
    
    echo ""
    echo "Step 5: Final status check..."
    check_status
    
    echo ""
    echo "Test completed! Check the dotCMS logs for request draining behavior."
    
    # Cleanup
    rm -f /tmp/request_*_response.json /tmp/request_*_pid
}

# Function to test timeout scenario
test_timeout() {
    echo "=== Testing Request Draining Timeout ==="
    echo "This test uses requests longer than the drain timeout (15s default)"
    echo ""
    
    local long_duration=20000  # 20 seconds - longer than default timeout
    
    echo "Starting 2 requests with ${long_duration}ms duration..."
    start_long_request 1 $long_duration
    start_long_request 2 $long_duration
    
    echo ""
    echo "Send SIGTERM now to test timeout behavior"
    echo "Expected: Shutdown should proceed after 15s timeout even with active requests"
    echo ""
    
    # Wait for requests
    for i in 1 2; do
        if [ -f "/tmp/request_${i}_pid" ]; then
            pid=$(cat "/tmp/request_${i}_pid")
            wait $pid 2>/dev/null
            rm -f "/tmp/request_${i}_pid"
        fi
    done
    
    echo "Timeout test completed!"
    rm -f /tmp/request_*_response.json /tmp/request_*_pid
}

# Parse command line arguments
case "${1:-test}" in
    "test")
        run_test
        ;;
    "timeout")
        test_timeout
        ;;
    "status")
        check_status
        ;;
    "simulate")
        simulate_requests "${2:-3}"
        ;;
    "help")
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  test       - Run standard request draining test (default)"
        echo "  timeout    - Test timeout scenario with long requests"
        echo "  status     - Check current shutdown status"
        echo "  simulate N - Simulate N active requests without HTTP"
        echo "  help       - Show this help"
        echo ""
        echo "Environment Variables:"
        echo "  DOTCMS_URL      - dotCMS base URL (default: http://localhost:8080)"
        echo "  DURATION        - Request duration in ms (default: 10000)"
        echo "  NUM_REQUESTS    - Number of requests (default: 3)"
        echo ""
        echo "Examples:"
        echo "  $0 test                    # Standard test"
        echo "  $0 timeout                 # Timeout test"
        echo "  DURATION=15000 $0 test     # 15-second requests"
        echo "  NUM_REQUESTS=5 $0 test     # 5 parallel requests"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac 