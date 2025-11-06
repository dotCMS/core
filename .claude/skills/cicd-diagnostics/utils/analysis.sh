#!/usr/bin/env bash
# Advanced Analysis Functions
# Test frequency analysis and source code lookup

# Note: Use 'set -e' only to allow sourcing in both bash and zsh
set -e

# Analyze test failure frequency across recent workflow runs
# Usage: analyze_test_failure_frequency WORKFLOW_NAME TEST_NAME [LIMIT]
# Returns: Statistics about test failures
analyze_test_failure_frequency() {
    local workflow="$1"
    local test_name="$2"
    local limit="${3:-30}"

    echo "=== TEST FAILURE FREQUENCY ANALYSIS ==="
    echo ""
    echo "Workflow: $workflow"
    echo "Test: $test_name"
    echo "Analyzing last $limit runs..."
    echo ""

    local tmp_runs=$(mktemp)
    gh run list --workflow="$workflow" --limit "$limit" \
        --json databaseId,conclusion,createdAt > "$tmp_runs" 2>/dev/null || echo "[]" > "$tmp_runs"

    local total_runs=$(jq '. | length' "$tmp_runs")

    if [ "$total_runs" -eq 0 ]; then
        echo "No runs found for workflow: $workflow"
        rm -f "$tmp_runs"
        return 1
    fi

    # Check each run for this specific test failure
    local failures_with_test=0
    local total_failures=0

    echo "Checking runs for test '$test_name'..."

    jq -r '.[] | "\(.databaseId) \(.conclusion)"' "$tmp_runs" | while read -r run_id conclusion; do
        if [ "$conclusion" = "failure" ]; then
            total_failures=$((total_failures + 1))

            # Download job logs for failed runs (only check if failure present)
            local jobs_file=$(mktemp)
            gh api "/repos/dotCMS/core/actions/runs/$run_id/jobs" --jq '.jobs[] | select(.conclusion == "failure") | .id' > "$jobs_file" 2>/dev/null || true

            if [ -s "$jobs_file" ]; then
                while read -r job_id; do
                    # Quick check for test name in job logs
                    if gh api "/repos/dotCMS/core/actions/jobs/$job_id/logs" 2>/dev/null | grep -q "$test_name"; then
                        echo "  Run $run_id: CONTAINS test failure"
                        failures_with_test=$((failures_with_test + 1))
                        break
                    fi
                done < "$jobs_file"
            fi
            rm -f "$jobs_file"
        fi
    done

    rm -f "$tmp_runs"

    # Calculate rates
    local failure_rate=$((total_failures * 100 / total_runs))
    local test_failure_rate=0
    if [ "$total_failures" -gt 0 ]; then
        test_failure_rate=$((failures_with_test * 100 / total_failures))
    fi

    cat <<EOF

=== FREQUENCY STATISTICS ===
Total runs analyzed: $total_runs
Total failures: $total_failures ($failure_rate%)
Failures with this test: $failures_with_test
Test failure rate: $test_failure_rate% of all failures

EOF

    # Classification
    if [ "$failures_with_test" -eq 0 ]; then
        echo "ASSESSMENT: This test has NOT failed recently (new failure)"
    elif [ "$failures_with_test" -eq 1 ]; then
        echo "ASSESSMENT: This test has failed once recently (rare failure)"
    elif [ "$test_failure_rate" -lt 30 ]; then
        echo "ASSESSMENT: This test fails occasionally (intermittent/flaky)"
    elif [ "$test_failure_rate" -lt 70 ]; then
        echo "ASSESSMENT: This test fails frequently (very flaky)"
    else
        echo "ASSESSMENT: This test fails consistently (likely a real bug or broken test)"
    fi
}

# Lookup test source code
# Usage: lookup_test_source TEST_CLASS TEST_METHOD
# Returns: Test source code with context
lookup_test_source() {
    local test_class="$1"
    local test_method="${2:-}"

    echo "=== TEST SOURCE CODE LOOKUP ==="
    echo ""
    echo "Searching for: $test_class${test_method:+.$test_method}"
    echo ""

    # Find the test file
    local test_files=$(find . -type f -name "${test_class}.java" -o -name "${test_class}.ts" 2>/dev/null | grep -v "/node_modules/\|/target/\|/build/" || true)

    if [ -z "$test_files" ]; then
        echo "❌ Test file not found: $test_class"
        return 1
    fi

    # If multiple files, list them
    local file_count=$(echo "$test_files" | wc -l | tr -d ' ')
    if [ "$file_count" -gt 1 ]; then
        echo "Found multiple matches:"
        echo "$test_files" | nl
        echo ""
        # Use the first one
        local test_file=$(echo "$test_files" | head -1)
        echo "Using: $test_file"
    else
        local test_file="$test_files"
        echo "Found: $test_file"
    fi

    echo ""

    if [ -n "$test_method" ]; then
        # Show specific test method
        echo "=== TEST METHOD: $test_method ==="
        echo ""

        # Find the method and show 50 lines
        grep -n -A 50 "function $test_method\|void $test_method\|@Test.*$test_method" "$test_file" 2>/dev/null || \
            echo "Method not found (file exists but method pattern didn't match)"
    else
        # Show class structure (first 100 lines)
        echo "=== CLASS STRUCTURE (first 100 lines) ==="
        echo ""
        head -100 "$test_file" | cat -n
    fi

    echo ""
    echo "Full file path: $(realpath "$test_file" 2>/dev/null || echo "$test_file")"
}

# Compare PR vs Merge Queue for same commit
# Usage: compare_pr_vs_merge_queue COMMIT_SHA
# Returns: Comparison analysis
compare_pr_vs_merge_queue() {
    local commit_sha="$1"

    echo "=== PR VS MERGE QUEUE COMPARISON ==="
    echo ""
    echo "Commit: $commit_sha"
    echo ""

    local pr_run=$(gh run list --workflow="cicd_1-pr.yml" --commit="$commit_sha" \
        --limit 1 --json databaseId,conclusion,displayTitle,createdAt 2>/dev/null | jq -r '.[0]')

    local mq_run=$(gh run list --workflow="cicd_2-merge-queue.yml" --commit="$commit_sha" \
        --limit 1 --json databaseId,conclusion,displayTitle,createdAt 2>/dev/null | jq -r '.[0]')

    if [ "$pr_run" = "null" ] || [ -z "$pr_run" ]; then
        echo "❌ No PR run found for commit $commit_sha"
    else
        echo "PR Workflow:"
        echo "$pr_run" | jq -r '"  Run ID: \(.databaseId)\n  Conclusion: \(.conclusion)\n  Title: \(.displayTitle)\n  Created: \(.createdAt)"'
        echo ""
    fi

    if [ "$mq_run" = "null" ] || [ -z "$mq_run" ]; then
        echo "❌ No Merge Queue run found for commit $commit_sha"
    else
        echo "Merge Queue Workflow:"
        echo "$mq_run" | jq -r '"  Run ID: \(.databaseId)\n  Conclusion: \(.conclusion)\n  Title: \(.displayTitle)\n  Created: \(.createdAt)"'
        echo ""
    fi

    # Analyze discrepancy
    local pr_conclusion=$(echo "$pr_run" | jq -r '.conclusion // "not_found"')
    local mq_conclusion=$(echo "$mq_run" | jq -r '.conclusion // "not_found"')

    echo "=== ANALYSIS ==="
    if [ "$pr_conclusion" = "success" ] && [ "$mq_conclusion" = "failure" ]; then
        cat <<EOF

⚠️  TEST FILTERING DISCREPANCY DETECTED

The PR passed but the Merge Queue failed for the same commit.

Common causes:
1. Test Filtering: PR only runs affected tests, Merge Queue runs full suite
2. Flaky Test: Test that wasn't run in PR failed in Merge Queue
3. Race Condition: Different execution timing caused different results
4. Infrastructure: Different runner environment or resource contention

Recommendation: Check which tests ran in PR vs Merge Queue
EOF
    elif [ "$pr_conclusion" = "failure" ] && [ "$mq_conclusion" = "failure" ]; then
        echo "✓ Both PR and Merge Queue failed (consistent failure)"
    elif [ "$pr_conclusion" = "success" ] && [ "$mq_conclusion" = "success" ]; then
        echo "✓ Both PR and Merge Queue passed (no issue)"
    elif [ "$pr_conclusion" = "not_found" ] || [ "$mq_conclusion" = "not_found" ]; then
        echo "⚠️  One or both workflows not found for this commit"
    else
        echo "Status: PR=$pr_conclusion, MergeQueue=$mq_conclusion"
    fi
}

# Generate quick test failure summary
# Usage: generate_failure_summary LOG_FILE
# Returns: One-line summary for AI analysis
generate_failure_summary() {
    local log_file="$1"

    local test_name=$(grep "<<< FAILURE!" "$log_file" 2>/dev/null | head -1 | sed -E 's/.*\[ERROR\] ([^ ]+) .*/\1/' || echo "Unknown")
    local error_type=$(grep "AssertionFailedError\|Exception\|Error" "$log_file" 2>/dev/null | head -1 | sed -E 's/.*([A-Z][a-zA-Z]+Error|[A-Z][a-zA-Z]+Exception).*/\1/' || echo "Unknown")
    local assertion=$(grep "expected:.*but was:" "$log_file" 2>/dev/null | head -1 | cut -c1-100 || echo "")

    echo "Test: $test_name | Error: $error_type | Assertion: $assertion"
}

# Main analysis coordinator (calls all analysis functions)
# Usage: run_complete_analysis RUN_ID LOG_FILE
run_complete_analysis() {
    local run_id="$1"
    local log_file="$2"

    echo "================================================================================"
    echo "COMPLETE FAILURE ANALYSIS"
    echo "================================================================================"
    echo ""

    # 1. Extract basic info
    local test_name=$(grep "<<< FAILURE!" "$log_file" 2>/dev/null | head -1 | sed -E 's/.*\[ERROR\] ([^ ]+) .*/\1/' | cut -d'.' -f1 || echo "")
    local workflow=$(gh run view "$run_id" --json workflowName --jq '.workflowName' 2>/dev/null || echo "Unknown")
    local commit=$(gh run view "$run_id" --json headSha --jq '.headSha' 2>/dev/null || echo "")

    echo "Run ID: $run_id"
    echo "Workflow: $workflow"
    echo "Commit: $commit"
    echo "Test: $test_name"
    echo ""

    # 2. Frequency analysis
    if [ -n "$test_name" ] && [ -n "$workflow" ]; then
        analyze_test_failure_frequency "$workflow" "$test_name" 20
        echo ""
    fi

    # 3. PR vs MQ comparison
    if [ -n "$commit" ]; then
        compare_pr_vs_merge_queue "$commit"
        echo ""
    fi

    # 4. Test source lookup
    if [ -n "$test_name" ]; then
        lookup_test_source "$test_name"
        echo ""
    fi

    echo "================================================================================"
}