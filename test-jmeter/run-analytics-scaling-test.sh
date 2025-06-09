#!/bin/bash

# Analytics Events Scaling Test Script
# This script helps find the breaking point by gradually increasing load

set -e

# Default values
HOST="localhost"
PORT="8080"
SCHEME="http"
ANALYTICS_KEY="js.cluster1.customer1.loadtest"
TEST_DURATION="300"  # 5 minutes
RAMPUP="60"         # 1 minute ramp up
MAX_RESPONSE_TIME="5000"  # 5 seconds

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to display usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  -h, --help                    Show this help message"
    echo "  --host HOST                   Target host (default: localhost)"
    echo "  --port PORT                   Target port (default: 8080)"
    echo "  --scheme SCHEME               Protocol scheme (default: http)"
    echo "  --analytics-key KEY           Analytics key (default: js.cluster1.customer1.loadtest)"
    echo "  --test-duration SECONDS       Test duration in seconds (default: 300)"
    echo "  --rampup SECONDS              Ramp up time in seconds (default: 60)"
    echo "  --max-response-time MS        Max response time in milliseconds (default: 5000)"
    echo "  --single-test                 Run a single test with specified parameters"
    echo "  --threads THREADS             Number of threads for single test"
    echo "  --events-per-second EPS       Events per second for single test"
    echo "  --scaling-test                Run automated scaling test (default)"
    echo ""
    echo "Examples:"
    echo "  $0 --single-test --threads 50 --events-per-second 10"
    echo "  $0 --scaling-test --host myhost.com --port 443 --scheme https"
    echo "  $0 --host localhost --port 8080 --test-duration 600"
}

# Function to run a single JMeter test
run_single_test() {
    local threads=$1
    local events_per_second=$2
    local test_name="analytics-test-${threads}t-${events_per_second}eps"
    
    print_info "Running test: $threads threads, $events_per_second events/sec"
    print_info "Test duration: ${TEST_DURATION}s, Ramp-up: ${RAMPUP}s"
    
    # Run the test
    ../mvnw clean install -Djmeter.test.skip=false \
        -DjmeterScript=analytics-events.jmx \
        -Djmeter.host="$HOST" \
        -Djmeter.port="$PORT" \
        -Djmeter.scheme="$SCHEME" \
        -Djmeter.thread.number="$threads" \
        -Djmeter.events.per.second="$events_per_second" \
        -Djmeter.test.duration="$TEST_DURATION" \
        -Djmeter.rampup="$RAMPUP" \
        -Djmeter.analytics.key="$ANALYTICS_KEY" \
        -Djmeter.max.response.time="$MAX_RESPONSE_TIME" \
        -Djmeter.jvm.xmx="4g" \
        -Djmeter.jvm.xms="2g" \
        -Djmeter.jvm.metaspace="1g" \
        -pl :dotcms-test-jmeter
    
    # Create results directory AFTER Maven build (since clean removes target directory)
    mkdir -p "target/scaling-results"
    
    # Copy results with meaningful names
    if [ -d "target/jmeter/results" ]; then
        cp -r target/jmeter/results "target/scaling-results/results-${test_name}"
        print_success "Results saved to: target/scaling-results/results-${test_name}"
    fi
    
    if [ -d "target/jmeter/reports" ]; then
        cp -r target/jmeter/reports "target/scaling-results/reports-${test_name}"
        print_success "Reports saved to: target/scaling-results/reports-${test_name}"
    fi
}

# Function to analyze results
analyze_results() {
    local test_name=$1
    local results_dir="target/scaling-results/results-${test_name}"
    
    if [ -d "$results_dir" ]; then
        print_info "Analyzing results for $test_name..."
        
        # Find the CSV file
        local csv_file=$(find "$results_dir" -name "*.csv" | head -1)
        if [ -n "$csv_file" ]; then
            # Basic analysis using awk
            echo "=== Test Results Summary ==="
            echo "Total requests: $(tail -n +2 "$csv_file" | wc -l)"
            echo "Successful requests: $(tail -n +2 "$csv_file" | awk -F, '$8=="true" {count++} END {print count+0}')"
            echo "Failed requests: $(tail -n +2 "$csv_file" | awk -F, '$8=="false" {count++} END {print count+0}')"
            echo "Average response time: $(tail -n +2 "$csv_file" | awk -F, '{sum+=$2; count++} END {print sum/count}') ms"
            echo "Max response time: $(tail -n +2 "$csv_file" | awk -F, 'BEGIN{max=0} {if($2>max) max=$2} END {print max}') ms"
            echo "Min response time: $(tail -n +2 "$csv_file" | awk -F, 'BEGIN{min=999999} {if($2<min) min=$2} END {print min}') ms"
            echo "=========================="
        fi
    fi
}

# Function to run automated scaling test
run_scaling_test() {
    print_info "Starting automated scaling test..."
    print_info "Target: $SCHEME://$HOST:$PORT"
    
    # Array of test configurations: threads,events_per_second
    local test_configs=(
        "5,1"     # Light load
        "10,2"    # Moderate load
        "20,5"    # Higher load
        "50,10"   # High load
        "100,20"  # Very high load
        "200,50"  # Extreme load
        "500,100" # Maximum load
    )
    
    local summary_file="target/scaling-results/scaling-test-summary.txt"
    mkdir -p "target/scaling-results"
    
    echo "Analytics Events Scaling Test Summary" > "$summary_file"
    echo "====================================" >> "$summary_file"
    echo "Test started: $(date)" >> "$summary_file"
    echo "Target: $SCHEME://$HOST:$PORT" >> "$summary_file"
    echo "Test duration: ${TEST_DURATION}s" >> "$summary_file"
    echo "Ramp-up time: ${RAMPUP}s" >> "$summary_file"
    echo "" >> "$summary_file"
    
    for config in "${test_configs[@]}"; do
        IFS=',' read -r threads events_per_second <<< "$config"
        local test_name="analytics-test-${threads}t-${events_per_second}eps"
        
        print_info "=== Running test configuration: $threads threads, $events_per_second events/sec ==="
        
        # Run the test
        if run_single_test "$threads" "$events_per_second"; then
            print_success "Test completed successfully"
            echo "$test_name: SUCCESS" >> "$summary_file"
            
            # Analyze results
            analyze_results "$test_name"
        else
            print_error "Test failed"
            echo "$test_name: FAILED" >> "$summary_file"
        fi
        
        # Wait between tests
        print_info "Waiting 30 seconds before next test..."
        sleep 30
    done
    
    echo "" >> "$summary_file"
    echo "Test completed: $(date)" >> "$summary_file"
    
    print_success "Scaling test completed. Summary saved to: $summary_file"
    print_info "Individual test results available in: target/scaling-results/"
}

# Parse command line arguments
SCALING_TEST=true
SINGLE_TEST=false
THREADS=""
EVENTS_PER_SECOND=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        --host)
            HOST="$2"
            shift 2
            ;;
        --port)
            PORT="$2"
            shift 2
            ;;
        --scheme)
            SCHEME="$2"
            shift 2
            ;;
        --analytics-key)
            ANALYTICS_KEY="$2"
            shift 2
            ;;
        --test-duration)
            TEST_DURATION="$2"
            shift 2
            ;;
        --rampup)
            RAMPUP="$2"
            shift 2
            ;;
        --max-response-time)
            MAX_RESPONSE_TIME="$2"
            shift 2
            ;;
        --single-test)
            SINGLE_TEST=true
            SCALING_TEST=false
            shift
            ;;
        --threads)
            THREADS="$2"
            shift 2
            ;;
        --events-per-second)
            EVENTS_PER_SECOND="$2"
            shift 2
            ;;
        --scaling-test)
            SCALING_TEST=true
            SINGLE_TEST=false
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Validate single test parameters
if [ "$SINGLE_TEST" = true ]; then
    if [ -z "$THREADS" ] || [ -z "$EVENTS_PER_SECOND" ]; then
        print_error "Single test requires --threads and --events-per-second parameters"
        usage
        exit 1
    fi
fi

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_error "Please run this script from the test-jmeter directory"
    exit 1
fi

# Check if analytics-events.jmx exists
if [ ! -f "src/test/jmeter/analytics-events.jmx" ]; then
    print_error "Analytics events JMeter test file not found: src/test/jmeter/analytics-events.jmx"
    exit 1
fi

# Run the appropriate test
if [ "$SINGLE_TEST" = true ]; then
    print_info "Running single test: $THREADS threads, $EVENTS_PER_SECOND events/sec"
    run_single_test "$THREADS" "$EVENTS_PER_SECOND"
    analyze_results "analytics-test-${THREADS}t-${EVENTS_PER_SECOND}eps"
else
    run_scaling_test
fi

print_success "Test execution completed!" 