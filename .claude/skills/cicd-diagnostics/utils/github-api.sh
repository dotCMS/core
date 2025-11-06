#!/bin/bash
# GitHub API Utility Functions for CI/CD Diagnostics
# Provides reusable functions for interacting with GitHub API and CLI

set -euo pipefail

# Extract run ID from GitHub Actions URL
# Usage: extract_run_id "https://github.com/dotCMS/core/actions/runs/19118302390"
# Returns: 19118302390
extract_run_id() {
    local url="$1"
    echo "$url" | sed -E 's/.*runs\/([0-9]+).*/\1/'
}

# Extract PR number from URL or branch name
# Usage: extract_pr_number "https://github.com/dotCMS/core/pull/33711"
# Usage: extract_pr_number "issue-33711-feature-name"
# Returns: 33711
extract_pr_number() {
    local input="$1"
    if [[ "$input" =~ pull/([0-9]+) ]]; then
        echo "${BASH_REMATCH[1]}"
    elif [[ "$input" =~ issue-([0-9]+) ]]; then
        echo "${BASH_REMATCH[1]}"
    else
        echo "$input"
    fi
}

# Get workflow run metadata
# Usage: get_run_metadata RUN_ID OUTPUT_FILE
# Fetches: conclusion, status, event, headBranch, headSha, workflowName, url, timestamps, displayTitle
get_run_metadata() {
    local run_id="$1"
    local output_file="$2"

    gh run view "$run_id" \
        --json conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle \
        > "$output_file"
}

# Get all jobs for a workflow run with detailed step information
# Usage: get_jobs_detailed RUN_ID OUTPUT_FILE
get_jobs_detailed() {
    local run_id="$1"
    local output_file="$2"

    gh api "/repos/dotCMS/core/actions/runs/$run_id/jobs?per_page=100" \
        > "$output_file"
}

# Get failed jobs from detailed jobs file
# Usage: get_failed_jobs JOBS_FILE
# Returns: JSON array of failed jobs
get_failed_jobs() {
    local jobs_file="$1"
    jq '.jobs[] | select(.conclusion == "failure")' "$jobs_file"
}

# Get canceled jobs from detailed jobs file
# Usage: get_canceled_jobs JOBS_FILE
# Returns: JSON array of canceled jobs
get_canceled_jobs() {
    local jobs_file="$1"
    jq '.jobs[] | select(.conclusion == "cancelled")' "$jobs_file"
}

# Download logs for a specific job
# Usage: download_job_logs JOB_ID OUTPUT_FILE
download_job_logs() {
    local job_id="$1"
    local output_file="$2"

    gh api "/repos/dotCMS/core/actions/jobs/$job_id/logs" > "$output_file"
}

# Get PR information including status check rollup
# Usage: get_pr_info PR_NUMBER OUTPUT_FILE
get_pr_info() {
    local pr_num="$1"
    local output_file="$2"

    gh pr view "$pr_num" \
        --json number,headRefOid,headRefName,title,author,statusCheckRollup \
        > "$output_file"
}

# Find failed run from PR info
# Usage: find_failed_run_from_pr PR_INFO_FILE
# Returns: Run ID or empty string
find_failed_run_from_pr() {
    local pr_info_file="$1"

    local failed_url=$(jq -r '.statusCheckRollup[] | select(.conclusion == "FAILURE" and .workflowName == "-1 PR Check") | .detailsUrl' "$pr_info_file" | head -1)

    if [ -n "$failed_url" ]; then
        extract_run_id "$failed_url"
    else
        echo ""
    fi
}

# Get recent workflow runs
# Usage: get_recent_runs WORKFLOW_NAME LIMIT OUTPUT_FILE
get_recent_runs() {
    local workflow_name="$1"
    local limit="${2:-20}"
    local output_file="$3"

    gh run list --workflow="$workflow_name" --limit "$limit" \
        --json databaseId,conclusion,headSha,displayTitle,createdAt \
        > "$output_file"
}

# Get artifacts for a workflow run
# Usage: get_artifacts RUN_ID OUTPUT_FILE
get_artifacts() {
    local run_id="$1"
    local output_file="$2"

    gh api "/repos/dotCMS/core/actions/runs/$run_id/artifacts" \
        --jq '.artifacts[] | {name, id, size_in_bytes, expired}' \
        > "$output_file"
}

# Search for related GitHub issues
# Usage: search_issues SEARCH_QUERY OUTPUT_FILE
search_issues() {
    local query="$1"
    local output_file="$2"

    gh issue list --search "$query" \
        --json number,title,state,labels,createdAt \
        --limit 10 \
        > "$output_file"
}

# Get issue details
# Usage: get_issue ISSUE_NUMBER OUTPUT_FILE
get_issue() {
    local issue_num="$1"
    local output_file="$2"

    gh issue view "$issue_num" \
        --json title,body,labels,author \
        > "$output_file"
}

# Compare two commits
# Usage: compare_commits BASE_SHA HEAD_SHA OUTPUT_FILE
compare_commits() {
    local base_sha="$1"
    local head_sha="$2"
    local output_file="$3"

    gh api "/repos/dotCMS/core/compare/$base_sha...$head_sha" \
        --jq '.commits[] | {sha: .sha[:7], message: .commit.message, author: .commit.author.name}' \
        > "$output_file"
}

# Get PR list for current branch
# Usage: get_prs_for_branch BRANCH_NAME OUTPUT_FILE
get_prs_for_branch() {
    local branch="$1"
    local output_file="$2"

    gh pr list --head "$branch" \
        --json number,url,headRefOid,title,author \
        > "$output_file"
}

# Get workflow runs for specific commit
# Usage: get_runs_for_commit WORKFLOW_NAME COMMIT_SHA LIMIT
get_runs_for_commit() {
    local workflow_name="$1"
    local commit_sha="$2"
    local limit="${3:-5}"

    gh run list --workflow="$workflow_name" --commit="$commit_sha" --limit "$limit" \
        --json databaseId,conclusion,status,displayTitle
}

# Check if macOS (for sed compatibility)
is_macos() {
    [[ "$(uname)" == "Darwin" ]]
}

# Platform-agnostic sed for URL parsing
safe_sed() {
    if is_macos; then
        sed -E "$@"
    else
        sed -r "$@"
    fi
}