#!/bin/bash

# JMeter Results Analysis Script
# Analyzes JMeter CSV results to provide comprehensive performance insights

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/target/jmeter/results"

# Help function
show_help() {
    cat << EOF
JMeter Results Analysis Script

USAGE:
    $0 [OPTIONS] [RESULT_FILE]

OPTIONS:
    -h, --help              Show this help message
    -l, --latest            Analyze the latest result file
    -a, --all               Analyze all result files
    -d, --directory DIR     Specify results directory (default: target/jmeter/results)
    -o, --output FILE       Save analysis to file
    -v, --verbose           Verbose output
    --json                  Output results in JSON format
    --threshold RT          Response time threshold in ms (default: 5000)

EXAMPLES:
    $0 --latest                                    # Analyze latest test
    $0 target/jmeter/results/20250604-test.csv    # Analyze specific file
    $0 --all --output analysis.txt                # Analyze all tests, save to file
    $0 --latest --json --threshold 1000           # JSON output with 1s threshold

EOF
}

# Parse command line arguments
LATEST=false
ALL=false
VERBOSE=false
JSON=false
OUTPUT_FILE=""
THRESHOLD=5000
RESULT_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -l|--latest)
            LATEST=true
            shift
            ;;
        -a|--all)
            ALL=true
            shift
            ;;
        -d|--directory)
            RESULTS_DIR="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --json)
            JSON=true
            shift
            ;;
        --threshold)
            THRESHOLD="$2"
            shift 2
            ;;
        -*)
            echo "Unknown option $1"
            show_help
            exit 1
            ;;
        *)
            RESULT_FILE="$1"
            shift
            ;;
    esac
done

# Logging functions
log_info() {
    if [[ "$JSON" != "true" ]]; then
        echo -e "${BLUE}[INFO]${NC} $1"
    fi
}

log_success() {
    if [[ "$JSON" != "true" ]]; then
        echo -e "${GREEN}[SUCCESS]${NC} $1"
    fi
}

log_warning() {
    if [[ "$JSON" != "true" ]]; then
        echo -e "${YELLOW}[WARNING]${NC} $1"
    fi
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Check if bc is available
check_dependencies() {
    if ! command -v bc &> /dev/null; then
        log_error "bc (calculator) is required but not installed. Please install it."
        exit 1
    fi
}

# Get latest result file
get_latest_result() {
    if [[ ! -d "$RESULTS_DIR" ]]; then
        log_error "Results directory not found: $RESULTS_DIR"
        exit 1
    fi
    
    local latest=$(ls -t "$RESULTS_DIR"/*.csv 2>/dev/null | head -1)
    if [[ -z "$latest" ]]; then
        log_error "No CSV result files found in $RESULTS_DIR"
        exit 1
    fi
    echo "$latest"
}

# Validate result file
validate_result_file() {
    local file="$1"
    
    if [[ ! -f "$file" ]]; then
        log_error "Result file not found: $file"
        exit 1
    fi
    
    if [[ ! -s "$file" ]]; then
        log_error "Result file is empty: $file"
        exit 1
    fi
    
    # Check if file has valid CSV structure
    local header_count=$(head -1 "$file" | awk -F',' '{print NF}')
    if [[ $header_count -lt 10 ]]; then
        log_error "Invalid CSV format: $file"
        exit 1
    fi
}

# Analyze single result file
analyze_result() {
    local file="$1"
    local temp_dir=$(mktemp -d)
    
    # Extract data to temp files
    tail -n +2 "$file" > "$temp_dir/data.csv"
    
    # Basic counts
    local total_requests=$(wc -l < "$temp_dir/data.csv")
    local successful_requests=$(awk -F',' '$8=="true"' "$temp_dir/data.csv" | wc -l)
    local failed_requests=$((total_requests - successful_requests))
    
    # Response time analysis
    awk -F',' '{print $2}' "$temp_dir/data.csv" | sort -n > "$temp_dir/response_times.txt"
    
    local min_response=$(head -1 "$temp_dir/response_times.txt")
    local max_response=$(tail -1 "$temp_dir/response_times.txt")
    local avg_response=$(awk '{sum+=$1} END {printf "%.0f", sum/NR}' "$temp_dir/response_times.txt")
    
    # Percentiles
    local total_lines=$(wc -l < "$temp_dir/response_times.txt")
    local p50_line=$(echo "$total_lines * 0.50" | bc | cut -d. -f1)
    local p90_line=$(echo "$total_lines * 0.90" | bc | cut -d. -f1)
    local p95_line=$(echo "$total_lines * 0.95" | bc | cut -d. -f1)
    local p99_line=$(echo "$total_lines * 0.99" | bc | cut -d. -f1)
    
    local p50=$(sed -n "${p50_line}p" "$temp_dir/response_times.txt")
    local p90=$(sed -n "${p90_line}p" "$temp_dir/response_times.txt")
    local p95=$(sed -n "${p95_line}p" "$temp_dir/response_times.txt")
    local p99=$(sed -n "${p99_line}p" "$temp_dir/response_times.txt")
    
    # Time analysis
    local first_timestamp=$(awk -F',' 'NR==1 {print $1}' "$temp_dir/data.csv")
    local last_timestamp=$(awk -F',' 'END {print $1}' "$temp_dir/data.csv")
    local duration_ms=$((last_timestamp - first_timestamp))
    local duration_sec=$(echo "scale=2; $duration_ms / 1000" | bc)
    local throughput=$(echo "scale=2; $total_requests / $duration_sec" | bc)
    
    # Error analysis
    local error_rate=$(echo "scale=2; $failed_requests * 100 / $total_requests" | bc)
    local timeout_requests=$(awk -F',' -v threshold="$THRESHOLD" '$2 > threshold' "$temp_dir/data.csv" | wc -l)
    local timeout_rate=$(echo "scale=2; $timeout_requests * 100 / $total_requests" | bc)
    
    # Response code analysis
    local response_codes=$(awk -F',' '{print $4}' "$temp_dir/data.csv" | sort | uniq -c | sort -nr)
    
    # Thread analysis
    local max_threads=$(awk -F',' '{print $12}' "$temp_dir/data.csv" | sort -n | tail -1)
    
    # Performance assessment
    local status="UNKNOWN"
    if (( $(echo "$error_rate < 1" | bc -l) )) && (( $(echo "$avg_response < 1000" | bc -l) )); then
        status="EXCELLENT"
    elif (( $(echo "$error_rate < 5" | bc -l) )) && (( $(echo "$avg_response < 2000" | bc -l) )); then
        status="GOOD"
    elif (( $(echo "$error_rate < 10" | bc -l) )) && (( $(echo "$avg_response < 5000" | bc -l) )); then
        status="ACCEPTABLE"
    else
        status="POOR"
    fi
    
    # Output results
    if [[ "$JSON" == "true" ]]; then
        output_json "$file" "$total_requests" "$successful_requests" "$failed_requests" "$error_rate" "$status" "$duration_sec" "$throughput" "$min_response" "$max_response" "$avg_response" "$p50" "$p90" "$p95" "$p99" "$timeout_requests" "$timeout_rate" "$max_threads"
    else
        output_text "$file" "$total_requests" "$successful_requests" "$failed_requests" "$error_rate" "$status" "$duration_sec" "$throughput" "$min_response" "$max_response" "$avg_response" "$p50" "$p90" "$p95" "$p99" "$timeout_requests" "$timeout_rate" "$max_threads" "$response_codes"
    fi
    
    # Cleanup
    rm -rf "$temp_dir"
}

# Output functions
output_json() {
    local file="$1" total_requests="$2" successful_requests="$3" failed_requests="$4" error_rate="$5" status="$6"
    local duration_sec="$7" throughput="$8" min_response="$9" max_response="${10}" avg_response="${11}"
    local p50="${12}" p90="${13}" p95="${14}" p99="${15}" timeout_requests="${16}" timeout_rate="${17}" max_threads="${18}"
    
    cat << EOF
{
    "file": "$file",
    "summary": {
        "total_requests": $total_requests,
        "successful_requests": $successful_requests,
        "failed_requests": $failed_requests,
        "error_rate": $error_rate,
        "status": "$status"
    },
    "timing": {
        "duration_seconds": $duration_sec,
        "throughput_rps": $throughput
    },
    "response_times": {
        "min": $min_response,
        "max": $max_response,
        "average": $avg_response,
        "median": $p50,
        "p90": $p90,
        "p95": $p95,
        "p99": $p99
    },
    "thresholds": {
        "timeout_threshold": $THRESHOLD,
        "timeout_requests": $timeout_requests,
        "timeout_rate": $timeout_rate
    },
    "load": {
        "max_concurrent_threads": $max_threads
    }
}
EOF
}

output_text() {
    local file="$1" total_requests="$2" successful_requests="$3" failed_requests="$4" error_rate="$5" status="$6"
    local duration_sec="$7" throughput="$8" min_response="$9" max_response="${10}" avg_response="${11}"
    local p50="${12}" p90="${13}" p95="${14}" p99="${15}" timeout_requests="${16}" timeout_rate="${17}" max_threads="${18}" response_codes="${19}"
    
    echo "=========================================="
    echo "JMeter Results Analysis"
    echo "=========================================="
    echo "File: $(basename "$file")"
    echo "Analysis Date: $(date)"
    echo ""
    
    echo "📊 SUMMARY"
    echo "----------------------------------------"
    echo "Total Requests:      $total_requests"
    echo "Successful:          $successful_requests ($(echo "scale=1; $successful_requests * 100 / $total_requests" | bc)%)"
    echo "Failed:              $failed_requests (${error_rate}%)"
    echo "Status:              $status"
    echo ""
    
    echo "⏱️ TIMING"
    echo "----------------------------------------"
    echo "Test Duration:       ${duration_sec}s"
    echo "Throughput:          ${throughput} requests/second"
    echo ""
    
    echo "📈 RESPONSE TIMES (ms)"
    echo "----------------------------------------"
    echo "Minimum:             $min_response"
    echo "Maximum:             $max_response"
    echo "Average:             $avg_response"
    echo "Median (50th):       $p50"
    echo "90th Percentile:     $p90"
    echo "95th Percentile:     $p95"
    echo "99th Percentile:     $p99"
    echo ""
    
    echo "🚨 PERFORMANCE THRESHOLDS"
    echo "----------------------------------------"
    echo "Timeout Threshold:   ${THRESHOLD}ms"
    echo "Requests > Threshold: $timeout_requests (${timeout_rate}%)"
    echo ""
    
    echo "🔗 LOAD CHARACTERISTICS"
    echo "----------------------------------------"
    echo "Max Concurrent:      $max_threads threads"
    echo ""
    
    echo "📋 RESPONSE CODES"
    echo "----------------------------------------"
    echo "$response_codes"
    echo ""
    
    # Performance recommendations
    echo "💡 RECOMMENDATIONS"
    echo "----------------------------------------"
    if [[ "$status" == "EXCELLENT" ]]; then
        echo "✅ Performance is excellent. System can handle this load comfortably."
    elif [[ "$status" == "GOOD" ]]; then
        echo "✅ Performance is good. Consider this as sustainable load level."
    elif [[ "$status" == "ACCEPTABLE" ]]; then
        echo "⚠️  Performance is acceptable but approaching limits. Monitor closely."
    else
        echo "❌ Performance is poor. Reduce load or investigate bottlenecks."
    fi
    
    if (( $(echo "$timeout_rate > 1" | bc -l) )); then
        echo "⚠️  High timeout rate detected. Check server resources and capacity."
    fi
    
    if (( $(echo "$p95 > $avg_response * 3" | bc -l) )); then
        echo "⚠️  High response time variability. Check for intermittent issues."
    fi
}

# Main execution
main() {
    check_dependencies
    
    # Determine which file(s) to analyze
    local files_to_analyze=()
    
    if [[ "$LATEST" == "true" ]]; then
        files_to_analyze+=($(get_latest_result))
    elif [[ "$ALL" == "true" ]]; then
        mapfile -t files_to_analyze < <(ls -t "$RESULTS_DIR"/*.csv 2>/dev/null)
        if [[ ${#files_to_analyze[@]} -eq 0 ]]; then
            log_error "No CSV result files found in $RESULTS_DIR"
            exit 1
        fi
    elif [[ -n "$RESULT_FILE" ]]; then
        files_to_analyze+=("$RESULT_FILE")
    else
        files_to_analyze+=($(get_latest_result))
        log_info "No specific file provided, analyzing latest result"
    fi
    
    # Analyze files
    local output=""
    for file in "${files_to_analyze[@]}"; do
        log_info "Analyzing: $(basename "$file")"
        validate_result_file "$file"
        
        local analysis=$(analyze_result "$file")
        if [[ "$JSON" == "true" ]]; then
            if [[ -n "$output" ]]; then
                output="$output,"
            fi
            output="$output$analysis"
        else
            output="$output$analysis\n\n"
        fi
    done
    
    # Format final output
    if [[ "$JSON" == "true" ]]; then
        if [[ ${#files_to_analyze[@]} -gt 1 ]]; then
            output="[$output]"
        fi
    fi
    
    # Output results
    if [[ -n "$OUTPUT_FILE" ]]; then
        echo -e "$output" > "$OUTPUT_FILE"
        log_success "Analysis saved to: $OUTPUT_FILE"
    else
        echo -e "$output"
    fi
}

# Run main function
main "$@" 