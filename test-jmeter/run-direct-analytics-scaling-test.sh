#!/bin/bash

# Direct Analytics Platform Scaling Test Script
# This script tests the direct analytics platform endpoint for maximum events per second and response times

set -e

# Default values
ANALYTICS_HOST="localhost"
ANALYTICS_PORT="8001"
ANALYTICS_SCHEME="http"
ANALYTICS_KEY="YOUR_ANALYTICS_KEY_HERE"
TEST_DURATION="300"  # 5 minutes
RAMPUP="60"         # 1 minute ramp up
MAX_RESPONSE_TIME="5000"  # 5 seconds
DOC_HOST="your-dotcms-instance.dotcms.cloud"
ENVIRONMENT_NAME="auth"
CLUSTER_NAME="example-cluster"
CUSTOMER_NAME="example-customer"

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
    echo "  --analytics-host HOST         Analytics platform host (default: analytics-dev.dotcms.site)"
    echo "  --analytics-port PORT         Analytics platform port (default: 443)"
    echo "  --analytics-scheme SCHEME     Analytics platform scheme (default: https)"
    echo "  --analytics-key KEY           Analytics key (default: YOUR_ANALYTICS_KEY_HERE)"
    echo "  --doc-host HOST               Document host (default: your-dotcms-instance.dotcms.cloud)"
    echo "  --environment-name ENV        Environment name (default: auth)"
    echo "  --cluster-name CLUSTER        Cluster name (default: example-cluster)"
    echo "  --customer-name CUSTOMER      Customer name (default: example-customer)"
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
    echo "  $0 --scaling-test --analytics-host analytics-prod.dotcms.site"
    echo "  $0 --analytics-key custom.key.here --test-duration 600"
}

# Function to run a single JMeter test
run_single_test() {
    local threads=$1
    local events_per_second=$2
    local test_name="direct-analytics-test-${threads}t-${events_per_second}eps"
    
    print_info "Running direct analytics test: $threads threads, $events_per_second events/sec"
    print_info "Target: $ANALYTICS_SCHEME://$ANALYTICS_HOST:$ANALYTICS_PORT/api/v1/event"
    print_info "Test duration: ${TEST_DURATION}s, Ramp-up: ${RAMPUP}s"
    
    # Run the test
    ../mvnw clean install -Djmeter.test.skip=false \
        -DjmeterScript=direct-analytics-events.jmx \
        -Danalytics.host="$ANALYTICS_HOST" \
        -Danalytics.port="$ANALYTICS_PORT" \
        -Danalytics.scheme="$ANALYTICS_SCHEME" \
        -Djmeter.thread.number="$threads" \
        -Djmeter.events.per.second="$events_per_second" \
        -Djmeter.test.duration="$TEST_DURATION" \
        -Djmeter.rampup="$RAMPUP" \
        -Djmeter.analytics.key="$ANALYTICS_KEY" \
        -Djmeter.max.response.time="$MAX_RESPONSE_TIME" \
        -Ddoc.host="$DOC_HOST" \
        -Denvironment.name="$ENVIRONMENT_NAME" \
        -Dcluster.name="$CLUSTER_NAME" \
        -Dcustomer.name="$CUSTOMER_NAME" \
        -Djmeter.jvm.xmx="4g" \
        -Djmeter.jvm.xms="2g" \
        -Djmeter.jvm.metaspace="1g" \
        -pl :dotcms-test-jmeter
    
    # Create results directory AFTER Maven build (since clean removes target directory)
    mkdir -p "target/direct-analytics-results"
    
    # Copy results with meaningful names
    if [ -d "target/jmeter/results" ]; then
        cp -r target/jmeter/results "target/direct-analytics-results/results-${test_name}"
        print_success "Results saved to: target/direct-analytics-results/results-${test_name}"
    fi
    
    if [ -d "target/jmeter/reports" ]; then
        cp -r target/jmeter/reports "target/direct-analytics-results/reports-${test_name}"
        print_success "Reports saved to: target/direct-analytics-results/reports-${test_name}"
    fi
}

# Function to analyze results
analyze_results() {
    local test_name=$1
    local results_dir="target/direct-analytics-results/results-${test_name}"
    
    if [ -d "$results_dir" ]; then
        print_info "Analyzing results for $test_name..."
        
        # Find the CSV file
        local csv_file=$(find "$results_dir" -name "*.csv" | head -1)
        if [ -n "$csv_file" ]; then
            # Basic analysis using awk
            echo "=== Direct Analytics Test Results Summary ==="
            echo "Total requests: $(tail -n +2 "$csv_file" | wc -l)"
            echo "Successful requests: $(tail -n +2 "$csv_file" | awk -F, '$8=="true" {count++} END {print count+0}')"
            echo "Failed requests: $(tail -n +2 "$csv_file" | awk -F, '$8=="false" {count++} END {print count+0}')"
            echo "Average response time: $(tail -n +2 "$csv_file" | awk -F, '{sum+=$2; count++} END {print sum/count}') ms"
            echo "Max response time: $(tail -n +2 "$csv_file" | awk -F, 'BEGIN{max=0} {if($2>max) max=$2} END {print max}') ms"
            echo "Min response time: $(tail -n +2 "$csv_file" | awk -F, 'BEGIN{min=999999} {if($2<min) min=$2} END {print min}') ms"
            echo "Requests >1s: $(tail -n +2 "$csv_file" | awk -F, '$2>1000 {count++} END {print count+0}')"
            echo "Requests >5s: $(tail -n +2 "$csv_file" | awk -F, '$2>5000 {count++} END {print count+0}')"
            
            # Calculate events per second achieved
            local total_requests=$(tail -n +2 "$csv_file" | wc -l)
            local actual_eps=$(echo "scale=2; $total_requests / $TEST_DURATION" | bc)
            echo "Actual events per second: $actual_eps"
            echo "Target events per second: $events_per_second"
            echo "Efficiency: $(echo "scale=2; ($actual_eps / $events_per_second) * 100" | bc)%"
            echo "================================================="
        fi
    fi
}

# Function to run automated scaling test
run_scaling_test() {
    print_info "Starting Direct Analytics Platform scaling test..."
    print_info "Target: $ANALYTICS_SCHEME://$ANALYTICS_HOST:$ANALYTICS_PORT/api/v1/event"
    
    # Array of test configurations: threads,events_per_second
    local test_configs=(
        "50,200"    # Current working baseline
        "75,400"    # Previous known limit
        "100,600"   # Previous breaking point
        "150,800"   # Stress test level
        "200,1000"  # Target goal - 1K EPS
        "250,1200"  # Stretch goal
        "300,1500"  # Ultimate target
        "400,2000"  # Maximum capacity test
    )
    
    local summary_file="target/direct-analytics-results/direct-analytics-scaling-summary.txt"
    mkdir -p "target/direct-analytics-results"
    
    echo "Direct Analytics Platform Scaling Test Summary" > "$summary_file"
    echo "=============================================" >> "$summary_file"
    echo "Test started: $(date)" >> "$summary_file"
    echo "Target: $ANALYTICS_SCHEME://$ANALYTICS_HOST:$ANALYTICS_PORT/api/v1/event" >> "$summary_file"
    echo "Analytics Key: $ANALYTICS_KEY" >> "$summary_file"
    echo "Test duration: ${TEST_DURATION}s" >> "$summary_file"
    echo "Ramp-up time: ${RAMPUP}s" >> "$summary_file"
    echo "" >> "$summary_file"
    
    for config in "${test_configs[@]}"; do
        IFS=',' read -r threads events_per_second <<< "$config"
        local test_name="direct-analytics-test-${threads}t-${events_per_second}eps"
        
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
    
    print_success "Direct Analytics scaling test completed. Summary saved to: $summary_file"
    print_info "Individual test results available in: target/direct-analytics-results/"
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
        --analytics-host)
            ANALYTICS_HOST="$2"
            shift 2
            ;;
        --analytics-port)
            ANALYTICS_PORT="$2"
            shift 2
            ;;
        --analytics-scheme)
            ANALYTICS_SCHEME="$2"
            shift 2
            ;;
        --analytics-key)
            ANALYTICS_KEY="$2"
            shift 2
            ;;
        --doc-host)
            DOC_HOST="$2"
            shift 2
            ;;
        --environment-name)
            ENVIRONMENT_NAME="$2"
            shift 2
            ;;
        --cluster-name)
            CLUSTER_NAME="$2"
            shift 2
            ;;
        --customer-name)
            CUSTOMER_NAME="$2"
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

# Check if direct-analytics-events.jmx exists
if [ ! -f "src/test/jmeter/direct-analytics-events.jmx" ]; then
    print_error "Direct analytics JMeter test file not found: src/test/jmeter/direct-analytics-events.jmx"
    exit 1
fi

# Check if bc is available for calculations
if ! command -v bc &> /dev/null; then
    print_warning "bc command not found. Installing bc for calculations..."
    if command -v brew &> /dev/null; then
        brew install bc
    else
        print_warning "Please install bc manually for calculation features"
    fi
fi

# Run the appropriate test
if [ "$SINGLE_TEST" = true ]; then
    print_info "Running single direct analytics test: $THREADS threads, $EVENTS_PER_SECOND events/sec"
    run_single_test "$THREADS" "$EVENTS_PER_SECOND"
    analyze_results "direct-analytics-test-${THREADS}t-${EVENTS_PER_SECOND}eps"
else
    run_scaling_test
fi

print_success "Direct Analytics Platform test execution completed!"
print_info "Results can be found in: target/direct-analytics-results/"
print_info "JMeter reports can be found in: target/direct-analytics-results/reports-*/" 