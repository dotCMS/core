#!/usr/bin/env bash
# Evidence Presentation for AI Analysis
# Simple data extraction without classification logic

# Note: Don't use 'set -e' to prevent SIGPIPE from killing parent shell
# SIGPIPE (exit 141) occurs when piping to head and is normal behavior

# Present all failure evidence for AI analysis
# Usage: present_failure_evidence LOG_FILE
present_failure_evidence() {
    local log_file="$1"

    cat <<EOF
================================================================================
FAILURE EVIDENCE FOR ANALYSIS
================================================================================

EOF

    # Test Failures
    echo "=== FAILED TESTS ==="
    echo ""
    local failed_tests=$(grep "<<< FAILURE!\|::error file=" "$log_file" 2>/dev/null | head -10)
    if [ -n "$failed_tests" ]; then
        echo "$failed_tests"
    else
        echo "No JUnit/E2E test failures found"
    fi

    echo ""
    echo "=== ERROR MESSAGES ==="
    echo ""
    local errors=$(grep -B 5 -A 5 "\[ERROR\]\|AssertionError\|Exception" "$log_file" 2>/dev/null | head -100)
    if [ -n "$errors" ]; then
        echo "$errors"
    else
        echo "No explicit errors found"
    fi

    echo ""
    echo "=== ASSERTION DETAILS ==="
    echo ""
    local assertions=$(grep "expected:.*but was:\|AssertionFailedError" "$log_file" 2>/dev/null | head -10)
    if [ -n "$assertions" ]; then
        echo "$assertions"
    else
        echo "No assertion failures found"
    fi

    echo ""
    echo "=== STACK TRACES ==="
    echo ""
    local stacks=$(grep "at [a-zA-Z0-9.]*([A-Za-z0-9]*\.java:[0-9]*)" "$log_file" 2>/dev/null | head -30)
    if [ -n "$stacks" ]; then
        echo "$stacks"
    else
        echo "No Java stack traces found"
    fi

    echo ""
    echo "=== TIMING INDICATORS ==="
    echo ""
    local timing=$(grep -i "timeout\|timed out\|Thread\.sleep\|Awaitility\|race condition\|concurrent" "$log_file" 2>/dev/null | head -10)
    if [ -n "$timing" ]; then
        echo "$timing"
    else
        echo "No obvious timing indicators"
    fi

    echo ""
    echo "=== INFRASTRUCTURE INDICATORS ==="
    echo ""
    local infra=$(grep -i "connection refused\|docker\|container.*failed\|elasticsearch.*exception\|database.*error" "$log_file" 2>/dev/null | head -10)
    if [ -n "$infra" ]; then
        echo "$infra"
    else
        echo "No obvious infrastructure issues"
    fi

    echo ""
    echo "================================================================================"
}

# Get context around first error (for cascade detection)
# Usage: get_first_error_context LOG_FILE [LINES_BEFORE] [LINES_AFTER]
get_first_error_context() {
    local log_file="$1"
    local before="${2:-30}"
    local after="${3:-20}"

    local first_error_line=$(grep -n "\[ERROR\]\|FAILURE!\|::error" "$log_file" 2>/dev/null | head -1 | cut -d: -f1)

    if [ -z "$first_error_line" ]; then
        echo "No errors found in log"
        return 1
    fi

    echo "=== FIRST ERROR AT LINE $first_error_line ==="
    echo ""

    local start=$((first_error_line - before))
    [ "$start" -lt 1 ] && start=1
    local end=$((first_error_line + after))

    sed -n "${start},${end}p" "$log_file" | cat -n
}

# Get timeline of all failures (for cascade analysis)
# Usage: get_failure_timeline LOG_FILE
get_failure_timeline() {
    local log_file="$1"

    echo "=== FAILURE TIMELINE ==="
    echo ""

    # Extract all failure events with line numbers
    grep -n "\[ERROR\]\|FAILURE!\|::error" "$log_file" 2>/dev/null | while read -r line; do
        local line_num=$(echo "$line" | cut -d: -f1)
        local content=$(echo "$line" | cut -d: -f2- | cut -c1-100)
        echo "Line $line_num: $content"
    done | head -20
}

# Present known issues for comparison (ENHANCED)
# Usage: present_known_issues TEST_NAME [ERROR_KEYWORDS]
present_known_issues() {
    local test_name="$1"
    local error_keywords="${2:-}"

    echo "=== KNOWN ISSUES SEARCH ==="
    echo ""
    echo "Searching for: $test_name"
    if [ -n "$error_keywords" ]; then
        echo "Error keywords: $error_keywords"
    fi
    echo ""

    # Strategy 1: Exact test name match
    echo "Strategy 1: Exact test name match"
    local exact_match=$(gh issue list --search "\"$test_name\" in:body" --state all \
        --label "Flakey Test" --json number,title,state --limit 5 2>/dev/null || echo "[]")

    if [ "$(echo "$exact_match" | jq '. | length')" -gt 0 ]; then
        echo "  EXACT MATCHES:"
        echo "$exact_match" | jq -r '.[] | "  - Issue #\(.number): \(.title) [\(.state)]"'
    else
        echo "  No exact matches"
    fi
    echo ""

    # Strategy 2: Test class name match (without method)
    echo "Strategy 2: Test class name match"
    local test_class=$(echo "$test_name" | sed 's/\..*//')
    local class_match=$(gh issue list --search "\"$test_class\" in:body" --state all \
        --label "Flakey Test" --json number,title,state --limit 10 2>/dev/null || echo "[]")

    # Deduplicate with exact matches
    local new_class_matches=$(echo "$class_match" | jq --argjson exact "$exact_match" \
        '[.[] | select([.number] | inside($exact | map(.number)) | not)]')

    if [ "$(echo "$new_class_matches" | jq '. | length')" -gt 0 ]; then
        echo "  CLASS NAME MATCHES:"
        echo "$new_class_matches" | jq -r '.[] | "  - Issue #\(.number): \(.title) [\(.state)]"'
    else
        echo "  No additional class matches"
    fi
    echo ""

    # Strategy 3: Error pattern/keyword match (if provided)
    if [ -n "$error_keywords" ]; then
        echo "Strategy 3: Error pattern match ($error_keywords)"
        local pattern_match=$(gh issue list --search "$error_keywords in:body" --state all \
            --label "Flakey Test" --json number,title,state,body --limit 15 2>/dev/null || echo "[]")

        # Deduplicate and show only new matches
        local new_pattern_matches=$(echo "$pattern_match" | jq --argjson exact "$exact_match" --argjson class "$new_class_matches" \
            '[.[] | select([.number] | inside(($exact + $class) | map(.number)) | not)]')

        if [ "$(echo "$new_pattern_matches" | jq '. | length')" -gt 0 ]; then
            echo "  PATTERN MATCHES:"
            echo "$new_pattern_matches" | jq -r '.[] | "  - Issue #\(.number): \(.title) [\(.state)]"'
            echo ""
            echo "  Pattern match details (showing first 200 chars from body):"
            echo "$new_pattern_matches" | jq -r '.[] | "    #\(.number): \(.body[0:200] | gsub("\n"; " "))..."' 2>/dev/null || true
        else
            echo "  No additional pattern matches"
        fi
        echo ""
    fi

    # Strategy 4: CLI test issues (if this is a CLI test)
    if echo "$test_name" | grep -qi "cli\|command"; then
        echo "Strategy 4: CLI-related flaky tests"
        local cli_match=$(gh issue list --search "cli in:body" --state all \
            --label "Flakey Test" --json number,title,state --limit 10 2>/dev/null || echo "[]")

        if [ "$(echo "$cli_match" | jq '. | length')" -gt 0 ]; then
            echo "  CLI-RELATED:"
            echo "$cli_match" | jq -r '.[] | "  - Issue #\(.number): \(.title) [\(.state)]"'
        else
            echo "  No CLI-related matches"
        fi
        echo ""
    fi

    # Summary
    local total_exact=$(echo "$exact_match" | jq '. | length')
    local total_class=$(echo "$new_class_matches" | jq '. | length')
    local total_pattern=$(if [ -n "$error_keywords" ]; then echo "$new_pattern_matches" | jq '. | length'; else echo "0"; fi)
    local total=$((total_exact + total_class + total_pattern))

    echo "=== SEARCH SUMMARY ==="
    echo "Total potential matches: $total"
    echo "  - Exact matches: $total_exact"
    echo "  - Class matches: $total_class"
    if [ -n "$error_keywords" ]; then
        echo "  - Pattern matches: $total_pattern"
    fi
    echo ""
}

# Get recent workflow run history
# Usage: present_recent_runs WORKFLOW_NAME [LIMIT]
present_recent_runs() {
    local workflow="$1"
    local limit="${2:-10}"

    echo "=== RECENT RUNS: $workflow ==="
    echo ""

    local runs=$(gh run list --workflow="$workflow" --limit "$limit" \
        --json databaseId,conclusion,displayTitle,createdAt 2>/dev/null || echo "[]")

    if [ "$(echo "$runs" | jq '. | length')" -eq 0 ]; then
        echo "No recent runs found"
    else
        echo "$runs" | jq -r '.[] | "\(.databaseId) | \(.conclusion) | \(.displayTitle) | \(.createdAt)"' | \
            column -t -s '|'
    fi

    echo ""

    # Calculate failure rate
    local total=$(echo "$runs" | jq '. | length')
    local failures=$(echo "$runs" | jq '[.[] | select(.conclusion == "failure")] | length')

    if [ "$total" -gt 0 ]; then
        local rate=$(( (failures * 100) / total ))
        echo "Failure rate: $failures/$total ($rate%)"
    fi
}

# Extract test name from log file
# Usage: extract_test_name LOG_FILE
extract_test_name() {
    local log_file="$1"

    # Try different patterns
    local test_name=""

    # JUnit test
    test_name=$(grep "<<< FAILURE!" "$log_file" 2>/dev/null | head -1 | sed -E 's/.*\[ERROR\] ([^ ]+) .*/\1/' | cut -d'.' -f1)

    if [ -z "$test_name" ]; then
        # E2E test
        test_name=$(grep "::error file=" "$log_file" 2>/dev/null | head -1 | sed -E 's/.*file=([^,]+).*/\1/' | xargs basename 2>/dev/null | sed 's/\.spec\.ts$//')
    fi

    if [ -z "$test_name" ]; then
        # Postman
        test_name=$(grep "Collection.*had failures" "$log_file" 2>/dev/null | head -1 | sed -E 's/.*Collection ([^ ]+) had failures.*/\1/')
    fi

    echo "$test_name"
}

# Extract error keywords for pattern matching
# Usage: extract_error_keywords LOG_FILE
extract_error_keywords() {
    local log_file="$1"

    local keywords=""

    # Check for common flaky test patterns
    if grep -qi "modDate\|moddate\|modification date" "$log_file" 2>/dev/null; then
        keywords="$keywords modDate"
    fi

    if grep -qi "createdDate\|created date\|creationDate" "$log_file" 2>/dev/null; then
        keywords="$keywords createdDate"
    fi

    if grep -qi "race condition\|concurrent\|synchronization" "$log_file" 2>/dev/null; then
        keywords="$keywords timing"
    fi

    if grep -qi "timeout\|timed out" "$log_file" 2>/dev/null; then
        keywords="$keywords timeout"
    fi

    if grep -qi "ordering\|order by\|sorted" "$log_file" 2>/dev/null; then
        keywords="$keywords ordering"
    fi

    if grep -qi "boolean.*flip\|expected:.*true.*but was:.*false\|expected:.*false.*but was:.*true" "$log_file" 2>/dev/null; then
        keywords="$keywords assertion"
    fi

    # Return trimmed keywords
    echo "$keywords" | xargs
}

# Present complete diagnostic package for AI
# Usage: present_complete_diagnostic LOG_FILE
present_complete_diagnostic() {
    local log_file="$1"

    echo "================================================================================"
    echo "COMPLETE DIAGNOSTIC EVIDENCE"
    echo "================================================================================"
    echo ""

    # 1. Failure evidence
    present_failure_evidence "$log_file"

    echo ""
    echo ""

    # 2. First error context
    get_first_error_context "$log_file"

    echo ""
    echo ""

    # 3. Timeline
    get_failure_timeline "$log_file"

    echo ""
    echo ""

    # 4. Known issues (with keyword extraction)
    local test_name=$(extract_test_name "$log_file")
    if [ -n "$test_name" ]; then
        local error_keywords=$(extract_error_keywords "$log_file")
        present_known_issues "$test_name" "$error_keywords"
    fi

    echo ""
    echo "================================================================================"
    echo "END DIAGNOSTIC EVIDENCE - READY FOR AI ANALYSIS"
    echo "================================================================================"
}

# Extract only error sections for large files (performance optimization)
# Usage: extract_error_sections_only LOG_FILE OUTPUT_FILE
extract_error_sections_only() {
    local log_file="$1"
    local output_file="$2"

    {
        echo "=== ERRORS AND FAILURES ==="
        grep -B 20 -A 20 "\[ERROR\]\|FAILURE!\|::error" "$log_file" 2>/dev/null | head -2000

        echo ""
        echo "=== FIRST 200 LINES ==="
        head -200 "$log_file"

        echo ""
        echo "=== LAST 200 LINES ==="
        tail -200 "$log_file"
    } > "$output_file"
}

# Get log file stats
# Usage: get_log_stats LOG_FILE
get_log_stats() {
    local log_file="$1"

    local size=$(wc -c < "$log_file" | tr -d ' ')
    local lines=$(wc -l < "$log_file" | tr -d ' ')
    local size_mb=$((size / 1048576))

    local error_count=$(grep -c "\[ERROR\]" "$log_file" 2>/dev/null || echo "0")
    local failure_count=$(grep -c "FAILURE!" "$log_file" 2>/dev/null || echo "0")

    cat <<EOF
=== LOG FILE STATISTICS ===
File: $log_file
Size: $size bytes ($size_mb MB)
Lines: $lines
Errors: $error_count
Failures: $failure_count

$(if [ "$size_mb" -gt 10 ]; then
    echo "⚠️  Large file detected. Consider using extract_error_sections_only() for faster analysis."
fi)
EOF
}