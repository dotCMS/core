#!/usr/bin/env bash
# Utility Helper Functions
# Simple extraction and comparison functions (no classification logic)

# Note: Use 'set -e' only to allow sourcing in both bash and zsh
set -e

# Extract test names from logs (macOS compatible)
# Usage: extract_failed_test_names LOG_FILE
# Returns: Newline-separated list of test names
extract_failed_test_names() {
    local log_file="$1"

    {
        # E2E test names from ::error annotations
        grep "::error file=" "$log_file" 2>/dev/null | sed -E 's/.*file=([^,]+).*/\1/' | while read -r line; do basename "$line" 2>/dev/null; done | sed 's/\.spec\.ts$//'

        # JUnit/Maven test names
        grep "<<< FAILURE!" "$log_file" 2>/dev/null | sed -E 's/.*\[ERROR\] //' | awk '{print $1}'

        # Postman collection failures
        grep "Collection.*had failures" "$log_file" 2>/dev/null | sed -E 's/.*Collection ([^ ]+) had failures.*/\1/'
    } | sort -u
}

# Compare PR and merge queue results for same commit
# Usage: compare_pr_and_merge_queue COMMIT_SHA
# Returns: JSON with comparison
compare_pr_and_merge_queue() {
    local commit_sha="$1"

    local pr_result=$(gh run list --workflow="cicd_1-pr.yml" --commit="$commit_sha" \
        --limit 1 --json conclusion,databaseId --jq '.[0]' 2>/dev/null || echo "{}")

    local mq_result=$(gh run list --workflow="cicd_2-merge-queue.yml" --commit="$commit_sha" \
        --limit 1 --json conclusion,databaseId --jq '.[0]' 2>/dev/null || echo "{}")

    local pr_conclusion=$(echo "$pr_result" | jq -r '.conclusion // "not_found"')
    local mq_conclusion=$(echo "$mq_result" | jq -r '.conclusion // "not_found"')

    local discrepancy=false
    if [ "$pr_conclusion" = "success" ] && [ "$mq_conclusion" = "failure" ]; then
        discrepancy=true
    fi

    echo "{"
    echo "  \"pr_conclusion\": \"$pr_conclusion\","
    echo "  \"mq_conclusion\": \"$mq_conclusion\","
    echo "  \"discrepancy\": $discrepancy,"
    echo "  \"pr_run_id\": $(echo "$pr_result" | jq -r '.databaseId // null'),"
    echo "  \"mq_run_id\": $(echo "$mq_result" | jq -r '.databaseId // null')"
    echo "}"
}

# Calculate failure frequency for recent runs
# Usage: calculate_failure_frequency WORKFLOW_NAME [LIMIT]
# Returns: Failure rate percentage
calculate_failure_frequency() {
    local workflow="$1"
    local limit="${2:-20}"

    local tmp_file=$(mktemp)
    gh run list --workflow="$workflow" --limit "$limit" \
        --json conclusion > "$tmp_file" 2>/dev/null || echo "[]" > "$tmp_file"

    local total=$(jq '. | length' "$tmp_file")
    local failures=$(jq '[.[] | select(.conclusion == "failure")] | length' "$tmp_file")

    rm -f "$tmp_file"

    if [ "$total" -eq 0 ]; then
        echo "0"
    else
        echo "$(( (failures * 100) / total ))"
    fi
}

# Get test failure history for specific test
# Usage: get_test_failure_history TEST_NAME [DAYS]
# Returns: Count of failures in recent issues
get_test_failure_history() {
    local test_name="$1"
    local days="${2:-30}"

    local test_class=$(basename "$test_name" | sed 's/\.java$//' | sed 's/\.spec\.ts$//')

    # Search closed and open issues mentioning this test
    local count=$(gh issue list --search "\"$test_class\" created:>$(date -v-${days}d +%Y-%m-%d 2>/dev/null || date -d "${days} days ago" +%Y-%m-%d)" \
        --state all --limit 100 --json number 2>/dev/null | jq '. | length' || echo "0")

    echo "$count"
}

# Extract specific test failure details with context
# Usage: extract_test_failure_context LOG_FILE TEST_NAME
extract_test_failure_context() {
    local log_file="$1"
    local test_name="$2"

    echo "=== TEST FAILURE CONTEXT: $test_name ==="
    echo ""

    # Find the test failure and get surrounding context
    grep -B 30 -A 10 "$test_name" "$log_file" 2>/dev/null || echo "Test context not found"
}

# Check if we should analyze full log or just context
# Usage: should_analyze_full_log LOG_FILE
# Returns: 0 if full analysis needed, 1 if context only
should_analyze_full_log() {
    local log_file="$1"
    local file_size=$(wc -c < "$log_file" | tr -d ' ')
    local size_mb=$((file_size / 1048576))

    # For files > 10MB, do context-only analysis first
    [ "$size_mb" -gt 10 ]
}

# Extract only relevant log sections (efficient for large files)
# Usage: extract_relevant_sections LOG_FILE OUTPUT_FILE
extract_relevant_sections() {
    local log_file="$1"
    local output_file="$2"

    {
        echo "=== ERROR SECTIONS ==="
        # Get 20 lines before and after each ERROR
        grep -B 20 -A 20 "\[ERROR\]" "$log_file" 2>/dev/null | head -1000

        echo ""
        echo "=== FAILURE SECTIONS ==="
        # Get 30 lines before and after each FAILURE
        grep -B 30 -A 10 "<<< FAILURE!" "$log_file" 2>/dev/null | head -500

        echo ""
        echo "=== FIRST 100 LINES ==="
        head -100 "$log_file"

        echo ""
        echo "=== LAST 100 LINES ==="
        tail -100 "$log_file"
    } > "$output_file"
}

# Search for known issues matching test failure (ENHANCED - searches issue bodies)
# Usage: search_known_issues_for_test TEST_CLASS TEST_METHOD [ERROR_PATTERN]
# Returns: JSON array of matching issues
search_known_issues_for_test() {
    local test_class="$1"
    local test_method="${2:-}"
    local error_pattern="${3:-}"

    echo "=== Searching for Known Issues ===" >&2
    echo "Test: ${test_class}${test_method:+.$test_method}" >&2

    local results_file=$(mktemp)

    # Strategy 1: Exact full test name in body
    if [ -n "$test_method" ]; then
        echo "  Searching for exact test name..." >&2
        gh api "search/issues?q=repo:dotCMS/core+\"${test_class}.${test_method}\"+in:body+label:\"Flakey Test\"" \
            --jq '.items[] | {number, title, state, html_url, relevance: "exact_match"}' 2>/dev/null >> "$results_file" || true
    fi

    # Strategy 2: Test class name in body
    echo "  Searching for test class name..." >&2
    gh api "search/issues?q=repo:dotCMS/core+\"${test_class}\"+in:body+label:\"Flakey Test\"" \
        --jq '.items[] | {number, title, state, html_url, relevance: "class_match"}' 2>/dev/null >> "$results_file" || true

    # Strategy 3: Error pattern keywords in body (if provided)
    if [ -n "$error_pattern" ]; then
        echo "  Searching for error pattern: $error_pattern..." >&2
        gh api "search/issues?q=repo:dotCMS/core+${error_pattern}+in:body+label:\"Flakey Test\"" \
            --jq '.items[] | {number, title, state, html_url, relevance: "pattern_match"}' 2>/dev/null >> "$results_file" || true
    fi

    # Combine, deduplicate by issue number, and rank by relevance
    if [ -s "$results_file" ]; then
        cat "$results_file" | jq -s '
            group_by(.number) |
            map({
                number: .[0].number,
                title: .[0].title,
                state: .[0].state,
                html_url: .[0].html_url,
                relevance: (map(.relevance) | if any(. == "exact_match") then "exact_match"
                            elif any(. == "class_match") then "class_match"
                            else "pattern_match" end),
                match_count: length
            }) |
            sort_by(
                if .relevance == "exact_match" then 0
                elif .relevance == "class_match" then 1
                else 2 end,
                -.match_count
            )'

        local count=$(cat "$results_file" | wc -l | tr -d ' ')
        echo "  Found $count potential matches" >&2
    else
        echo "[]"
        echo "  No matches found" >&2
    fi

    rm -f "$results_file"
}

# Get issue body for AI analysis
# Usage: get_issue_body_for_analysis ISSUE_NUMBER
# Returns: Issue body (first 1000 chars)
get_issue_body_for_analysis() {
    local issue_number="$1"

    gh issue view "$issue_number" --json body --jq '.body' 2>/dev/null | head -c 1000
}

# Present candidate issues for AI matching
# Usage: present_candidate_issues_for_ai TEST_NAME ERROR_SUMMARY
# Returns: Formatted text for AI analysis
present_candidate_issues_for_ai() {
    local test_name="$1"
    local error_summary="$2"

    cat <<EOF
=== KNOWN ISSUE MATCHING (AI Review Required) ===

Test Failure:
- Test: $test_name
- Error: $error_summary

Candidate Flaky Test Issues (last 50):

EOF

    # Get all flaky test issues and show relevant excerpts
    gh issue list --label "Flakey Test" --state all --json number,title,state,body --limit 50 | \
        jq -r '.[] | "Issue #\(.number): \(.title) (\(.state))\n\(.body[0:400])...\n---\n"'

    cat <<EOF

AI: Please review the above issues and identify which one(s) match this test failure pattern.
Look for:
- Same test class or method name
- Similar error patterns (e.g., modDate, timing, assertion failures)
- Related flaky test descriptions
- File paths matching the failed test

Provide issue number(s) and explain the match.
EOF
}