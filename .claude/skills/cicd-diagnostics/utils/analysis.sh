#!/bin/bash
# Failure Analysis and Classification Utilities
# Root cause determination and pattern analysis

set -euo pipefail

# Classify failure type based on patterns
# Usage: classify_failure_type ERROR_SUMMARY_FILE JOBS_FILE
# Returns: Classification (New Failure, Flaky Test, Infrastructure, Test Filtering)
classify_failure_type() {
    local error_summary="$1"
    local jobs_file="$2"

    local content=$(cat "$error_summary" 2>/dev/null || echo "")

    # Infrastructure patterns
    if echo "$content" | grep -qiE "(timeout|connection refused|rate limit|memory|elasticsearch.*exception|database.*error)"; then
        echo "Infrastructure"
        return
    fi

    # Test filtering (PR vs merge queue)
    if echo "$content" | grep -qE "Test Filtering Issue"; then
        echo "Test Filtering"
        return
    fi

    # Flaky test indicators
    local failed_count=$(jq '[.jobs[] | select(.conclusion == "failure")] | length' "$jobs_file" 2>/dev/null || echo "0")
    local cancelled_count=$(jq '[.jobs[] | select(.conclusion == "cancelled")] | length' "$jobs_file" 2>/dev/null || echo "0")

    if [ "$failed_count" -le 2 ] && [ "$cancelled_count" -gt 3 ]; then
        echo "Flaky Test (Possible)"
        return
    fi

    # Default: New failure
    echo "New Failure"
}

# Calculate failure rate from nightly builds
# Usage: calculate_nightly_failure_rate NIGHTLY_HISTORY_FILE
# Returns: "X/Y (Z%)"
calculate_nightly_failure_rate() {
    local history_file="$1"

    local total=$(jq '. | length' "$history_file" 2>/dev/null || echo "0")
    local failures=$(jq '[.[] | select(.conclusion == "failure")] | length' "$history_file" 2>/dev/null || echo "0")

    if [ "$total" -eq 0 ]; then
        echo "0/0 (N/A)"
        return
    fi

    local percentage=$((failures * 100 / total))
    echo "$failures/$total (${percentage}%)"
}

# Compare PR and merge queue results
# Usage: compare_pr_mergequeue PR_RESULT MQ_RESULT
# Returns: Comparison summary
compare_pr_mergequeue() {
    local pr_result="$1"
    local mq_result="$2"

    if [ "$pr_result" = "success" ] && [ "$mq_result" = "failure" ]; then
        echo "⚠️ Test Filtering Issue: PR passed but merge queue failed"
        echo "This usually indicates a test was filtered in PR but ran in merge queue"
        return
    fi

    if [ "$pr_result" = "failure" ] && [ "$mq_result" = "success" ]; then
        echo "✓ Merge queue passed after PR failure (likely fixed in merge)"
        return
    fi

    if [ "$pr_result" = "$mq_result" ]; then
        echo "✓ Consistent results: both $pr_result"
        return
    fi

    echo "⚠️ Inconsistent results: PR=$pr_result, MergeQueue=$mq_result"
}

# Determine if failure is recurring
# Usage: is_recurring_failure CURRENT_RUN_SHA RECENT_RUNS_FILE
# Returns: "true" or "false"
is_recurring_failure() {
    local current_sha="$1"
    local recent_runs="$2"

    local recent_failures=$(jq -r '[.[] | select(.conclusion == "failure" and .headSha != "'"$current_sha"'")] | length' "$recent_runs" 2>/dev/null || echo "0")

    if [ "$recent_failures" -ge 2 ]; then
        echo "true"
    else
        echo "false"
    fi
}

# Get failure frequency
# Usage: get_failure_frequency RECENT_RUNS_FILE
# Returns: "Once", "Intermittent (X/Y)", or "Consistent (X/Y)"
get_failure_frequency() {
    local recent_runs="$1"

    local total=$(jq '. | length' "$recent_runs" 2>/dev/null || echo "0")
    local failures=$(jq '[.[] | select(.conclusion == "failure")] | length' "$recent_runs" 2>/dev/null || echo "0")

    if [ "$failures" -eq 1 ]; then
        echo "Once"
        return
    fi

    local percentage=$((failures * 100 / total))

    if [ "$percentage" -ge 80 ]; then
        echo "Consistent ($failures/$total)"
    elif [ "$percentage" -ge 20 ]; then
        echo "Intermittent ($failures/$total)"
    else
        echo "Rare ($failures/$total)"
    fi
}

# Assess impact level
# Usage: assess_impact FAILURE_TYPE FREQUENCY ERROR_PATTERNS
# Returns: "High", "Medium", or "Low"
assess_impact() {
    local failure_type="$1"
    local frequency="$2"
    local error_patterns="$3"

    # High impact: Build failures, consistent failures, or infrastructure issues
    if [[ "$failure_type" =~ "Infrastructure" ]] || [[ "$frequency" =~ "Consistent" ]]; then
        echo "High"
        return
    fi

    # High impact: Build or compilation failures
    if echo "$error_patterns" | grep -qE "BUILD FAILURE|compilation error"; then
        echo "High"
        return
    fi

    # Medium impact: New failures or intermittent issues
    if [[ "$failure_type" =~ "New" ]] || [[ "$frequency" =~ "Intermittent" ]]; then
        echo "Medium"
        return
    fi

    # Low impact: One-time or rare failures
    echo "Low"
}

# Determine root cause confidence
# Usage: determine_confidence FAILURE_TYPE ERROR_COUNT EVIDENCE_QUALITY
# Returns: "High", "Medium", or "Low"
determine_confidence() {
    local failure_type="$1"
    local error_count="$2"
    local evidence_quality="$3"

    # High confidence: Clear error patterns, deterministic failures
    if [ "$error_count" -gt 0 ] && [[ "$evidence_quality" =~ "high" ]]; then
        echo "High"
        return
    fi

    # High confidence: Infrastructure issues (clear patterns)
    if [[ "$failure_type" =~ "Infrastructure" ]] && [ "$error_count" -gt 0 ]; then
        echo "High"
        return
    fi

    # Medium confidence: Some evidence but not definitive
    if [ "$error_count" -gt 0 ]; then
        echo "Medium"
        return
    fi

    # Low confidence: Unclear or missing evidence
    echo "Low"
}

# Extract evidence from error summary
# Usage: extract_evidence ERROR_SUMMARY_FILE
# Returns: Bullet-pointed evidence list
extract_evidence() {
    local error_summary="$1"

    echo "Evidence:"

    # Look for specific error patterns
    if grep -q "BUILD FAILURE" "$error_summary" 2>/dev/null; then
        echo "- Build failure detected in Maven output"
    fi

    if grep -q "AssertionError" "$error_summary" 2>/dev/null; then
        local assertion=$(grep "AssertionError" "$error_summary" | head -1)
        echo "- Test assertion failure: $assertion"
    fi

    if grep -qiE "timeout|connection refused" "$error_summary" 2>/dev/null; then
        echo "- Infrastructure/connectivity issues detected"
    fi

    if grep -q "expected.*to be at most" "$error_summary" 2>/dev/null; then
        echo "- Limit/boundary violation in API response"
    fi
}

# Generate recommendations based on failure type
# Usage: generate_recommendations FAILURE_TYPE ERROR_SUMMARY_FILE
# Returns: List of recommended actions
generate_recommendations() {
    local failure_type="$1"
    local error_summary="$2"

    echo "Recommended Actions:"
    echo ""

    case "$failure_type" in
        "New Failure")
            echo "1. Review recent code changes in the PR"
            echo "2. Compare with last successful build"
            echo "3. Check if issue reproduces locally"
            echo "4. Fix the identified issue and push update"
            ;;

        "Infrastructure")
            echo "1. Check GitHub Actions status page"
            echo "2. Review infrastructure logs (DB, ES)"
            echo "3. Retry the workflow"
            echo "4. If persistent, escalate to platform team"
            ;;

        "Flaky Test"*)
            echo "1. Search for existing flaky test issues"
            echo "2. Analyze test stability over recent runs"
            echo "3. Create or update flaky test issue"
            echo "4. Consider skipping test temporarily"
            ;;

        "Test Filtering")
            echo "1. Review test filtering configuration"
            echo "2. Ensure PR runs same tests as merge queue"
            echo "3. Update workflow test filters"
            echo "4. Re-run merge queue after fix"
            ;;

        *)
            echo "1. Review error logs for root cause"
            echo "2. Search for similar issues"
            echo "3. Reproduce locally if possible"
            echo "4. Create issue if needed"
            ;;
    esac
}

# Calculate time to resolution estimate
# Usage: estimate_resolution_time FAILURE_TYPE COMPLEXITY
# Returns: Estimated time range
estimate_resolution_time() {
    local failure_type="$1"
    local complexity="${2:-medium}"

    case "$failure_type" in
        "Infrastructure")
            echo "10-30 minutes (retry or wait for service recovery)"
            ;;
        "Flaky Test"*)
            echo "2-4 hours (investigation + temporary fix)"
            ;;
        "Test Filtering")
            echo "30-60 minutes (configuration update)"
            ;;
        "New Failure")
            case "$complexity" in
                "low") echo "30 minutes - 2 hours" ;;
                "medium") echo "2-4 hours" ;;
                "high") echo "4-8 hours or more" ;;
            esac
            ;;
        *)
            echo "2-4 hours (investigation required)"
            ;;
    esac
}