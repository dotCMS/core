#!/bin/bash
# Log Analysis Utility Functions
# Pattern matching and error extraction from CI/CD logs

set -euo pipefail

# Extract Maven/build failures from logs
# Usage: extract_build_failures LOG_FILE OUTPUT_FILE
extract_build_failures() {
    local log_file="$1"
    local output_file="$2"

    {
        echo "=== BUILD FAILURES ==="
        grep -E "\[ERROR\]|BUILD FAILURE" "$log_file" 2>/dev/null | head -100 || echo "No build failures found"
    } > "$output_file"
}

# Extract test failures from logs
# Usage: extract_test_failures LOG_FILE OUTPUT_FILE [APPEND]
extract_test_failures() {
    local log_file="$1"
    local output_file="$2"
    local append="${3:-false}"

    local output_op=">"
    [ "$append" = "true" ] && output_op=">>"

    {
        echo ""
        echo "=== TEST FAILURES ==="
        grep -E "(<<< FAILURE!|Tests run:.*Failures: [1-9]|::error|FAILED)" "$log_file" 2>/dev/null | head -100 || echo "No test failures found"
    } $output_op "$output_file"
}

# Extract Newman/Postman errors from logs
# Usage: extract_newman_errors LOG_FILE OUTPUT_FILE [APPEND]
extract_newman_errors() {
    local log_file="$1"
    local output_file="$2"
    local append="${3:-false}"

    local output_op=">"
    [ "$append" = "true" ] && output_op=">>"

    {
        echo ""
        echo "=== NEWMAN/POSTMAN ERRORS ==="
        grep -E "(newman.*error|Assertion.*failed|Expected.*to.*but|AssertionError)" "$log_file" 2>/dev/null | head -100 || echo "No newman errors found"
    } $output_op "$output_file"
}

# Extract infrastructure/timeout errors from logs
# Usage: extract_infrastructure_errors LOG_FILE OUTPUT_FILE [APPEND]
extract_infrastructure_errors() {
    local log_file="$1"
    local output_file="$2"
    local append="${3:-false}"

    local output_op=">"
    [ "$append" = "true" ] && output_op=">>"

    {
        echo ""
        echo "=== TIMEOUT/INFRASTRUCTURE ==="
        grep -iE "(timeout|connection refused|rate limit|Process exited with an error)" "$log_file" 2>/dev/null | head -50 || echo "No infrastructure issues found"
    } $output_op "$output_file"
}

# Extract E2E/Playwright test failures
# Usage: extract_e2e_failures LOG_FILE OUTPUT_FILE
extract_e2e_failures() {
    local log_file="$1"
    local output_file="$2"

    {
        echo "=== E2E TEST FAILURES ==="
        grep "::error file=" "$log_file" 2>/dev/null | sed 's/%0A/\n/g' || echo "No E2E failures found"
    } > "$output_file"
}

# Extract test summary from logs
# Usage: extract_test_summary LOG_FILE OUTPUT_FILE [APPEND]
extract_test_summary() {
    local log_file="$1"
    local output_file="$2"
    local append="${3:-false}"

    local output_op=">"
    [ "$append" = "true" ] && output_op=">>"

    {
        echo ""
        echo "=== TEST SUMMARY ==="
        grep -E "(passed|failed|flaky|Assertion failed|assertions|iterations|requests|test-scripts)" "$log_file" 2>/dev/null | tail -50 || echo "No test summary found"
    } $output_op "$output_file"
}

# Comprehensive error extraction (all patterns)
# Usage: extract_all_errors LOG_FILE OUTPUT_FILE
extract_all_errors() {
    local log_file="$1"
    local output_file="$2"

    # Build failures (first, creates file)
    extract_build_failures "$log_file" "$output_file"

    # Test failures (append)
    extract_test_failures "$log_file" "$output_file" true

    # Newman errors (append)
    extract_newman_errors "$log_file" "$output_file" true

    # Infrastructure errors (append)
    extract_infrastructure_errors "$log_file" "$output_file" true

    # Test summary (append)
    extract_test_summary "$log_file" "$output_file" true
}

# Find specific assertion failures with context
# Usage: find_assertion_with_context LOG_FILE PATTERN [CONTEXT_LINES]
find_assertion_with_context() {
    local log_file="$1"
    local pattern="$2"
    local context="${3:-10}"

    grep -B "$context" -A "$context" "$pattern" "$log_file" 2>/dev/null || echo "Pattern not found: $pattern"
}

# Extract failed test names
# Usage: extract_failed_test_names LOG_FILE
# Returns: List of failed test names (one per line)
extract_failed_test_names() {
    local log_file="$1"

    {
        # E2E test names from ::error annotations
        grep "::error file=" "$log_file" 2>/dev/null | sed -E 's/.*file=([^,]+).*/\1/' | xargs -n1 basename 2>/dev/null | sed 's/\.spec\.ts$//'

        # JUnit/Maven test names
        grep "<<< FAILURE!" "$log_file" 2>/dev/null | sed -E 's/.*\[ERROR\] (.*) .*/\1/' | awk '{print $1}'

        # Postman collection failures
        grep "Collection.*had failures" "$log_file" 2>/dev/null | sed -E 's/.*Collection ([^ ]+) had failures.*/\1/'
    } | sort -u
}

# Get log file size and line count
# Usage: get_log_stats LOG_FILE
get_log_stats() {
    local log_file="$1"

    if [ ! -f "$log_file" ]; then
        echo "Log file not found: $log_file" >&2
        return 1
    fi

    local size=$(wc -c < "$log_file" | tr -d ' ')
    local lines=$(wc -l < "$log_file" | tr -d ' ')
    local size_mb=$((size / 1048576))

    echo "File: $log_file"
    echo "Size: ${size} bytes (${size_mb} MB)"
    echo "Lines: ${lines}"
}

# Search for specific error pattern across log
# Usage: search_error_pattern LOG_FILE PATTERN
search_error_pattern() {
    local log_file="$1"
    local pattern="$2"

    grep -n "$pattern" "$log_file" 2>/dev/null | head -20 || echo "No matches for pattern: $pattern"
}

# Extract stack traces from logs
# Usage: extract_stack_traces LOG_FILE OUTPUT_FILE [MAX_TRACES]
extract_stack_traces() {
    local log_file="$1"
    local output_file="$2"
    local max_traces="${3:-5}"

    {
        echo "=== STACK TRACES ==="
        # Find lines with exception/error keywords and extract context
        grep -n -E "(Exception|Error|Stacktrace|at .*\(.*\.java:[0-9]+\))" "$log_file" 2>/dev/null | head -$((max_traces * 20)) || echo "No stack traces found"
    } > "$output_file"
}

# Check for common error patterns
# Usage: check_common_patterns LOG_FILE
# Returns: List of detected patterns
check_common_patterns() {
    local log_file="$1"

    echo "=== DETECTED ERROR PATTERNS ==="

    # Check each common pattern
    local patterns=(
        "OutOfMemoryError:Memory exhaustion"
        "TimeoutException:Timeout issues"
        "ConnectionRefusedException:Connection problems"
        "NullPointerException:Null pointer errors"
        "AssertionError:Test assertion failures"
        "DatabaseException:Database issues"
        "ElasticsearchException:Elasticsearch problems"
    )

    for pattern_def in "${patterns[@]}"; do
        IFS=':' read -r pattern description <<< "$pattern_def"
        if grep -q "$pattern" "$log_file" 2>/dev/null; then
            local count=$(grep -c "$pattern" "$log_file" 2>/dev/null || echo "0")
            echo "  ✗ $description ($count occurrences)"
        fi
    done
}

# Extract specific Postman collection failure details
# Usage: extract_postman_failure_details LOG_FILE COLLECTION_NAME OUTPUT_FILE
extract_postman_failure_details() {
    local log_file="$1"
    local collection_name="$2"
    local output_file="$3"

    {
        echo "=== POSTMAN COLLECTION: $collection_name ==="
        echo ""

        # Find collection run section
        grep -B 50 "Collection ${collection_name}.*had failures" "$log_file" 2>/dev/null | tail -60 || echo "Collection section not found"

        echo ""
        echo "=== FAILED REQUESTS ==="
        grep -B 30 "First page results within limit" "$log_file" 2>/dev/null | grep -E "(POST|GET|Response|Status|→|✓)" || echo "No request details found"

    } > "$output_file"
}