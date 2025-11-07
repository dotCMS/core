# CI/CD Diagnostics Utility Functions

Reusable bash utility functions for CI/CD failure analysis.

## Overview

This directory contains modular utility functions extracted from the cicd-diagnostics skill. These functions can be sourced and used by the skill or other automation scripts.

## Files

### github-api.sh
GitHub API and CLI wrapper functions for fetching workflow, job, and issue data.

**Key Functions:**
- `extract_run_id(url)` - Extract run ID from GitHub Actions URL
- `extract_pr_number(input)` - Extract PR number from URL or branch name
- `get_run_metadata(run_id, output_file)` - Fetch workflow run details
- `get_jobs_detailed(run_id, output_file)` - Get all jobs with step information
- `get_failed_jobs(jobs_file)` - Filter failed jobs from jobs file
- `download_job_logs(job_id, output_file)` - Download job logs
- `get_pr_info(pr_num, output_file)` - Get PR details and status checks
- `find_failed_run_from_pr(pr_info_file)` - Find failed run from PR data
- `get_recent_runs(workflow_name, limit, output_file)` - Fetch workflow history
- `search_issues(query, output_file)` - Search GitHub issues
- `compare_commits(base_sha, head_sha, output_file)` - Compare commit ranges

**Usage Example:**
```bash
source .claude/skills/cicd-diagnostics/utils/github-api.sh

RUN_ID=$(extract_run_id "https://github.com/dotCMS/core/actions/runs/19118302390")
get_run_metadata "$RUN_ID" "run-metadata.json"
```

### workspace.sh
Diagnostic workspace management with caching and artifact organization.

**Key Functions:**
- `create_diagnostic_workspace(run_id)` - Create timestamped directory
- `find_existing_diagnostic(run_id)` - Check for cached diagnostics
- `get_diagnostic_workspace(run_id)` - Get or create workspace (with caching)
- `save_artifact(dir, filename, content)` - Save artifact to workspace
- `artifact_exists(dir, filename)` - Check if artifact is cached
- `get_or_fetch_artifact(dir, filename, fetch_cmd)` - Cache-aware fetching
- `ensure_gitignore_diagnostics()` - Add diagnostic dirs to .gitignore
- `list_diagnostic_workspaces()` - List all diagnostic sessions
- `clean_old_diagnostics(max_age_hours, max_count)` - Cleanup old workspaces
- `get_workspace_summary(dir)` - Display workspace details

**Usage Example:**
```bash
source .claude/skills/cicd-diagnostics/utils/workspace.sh

DIAGNOSTIC_DIR=$(get_diagnostic_workspace "19118302390")
save_artifact "$DIAGNOSTIC_DIR" "notes.txt" "Analysis in progress..."
```

### log-analysis.sh
Pattern matching and error extraction from CI/CD logs.

**Key Functions:**
- `extract_build_failures(log_file, output_file)` - Find Maven/build errors
- `extract_test_failures(log_file, output_file, [append])` - Find test failures
- `extract_newman_errors(log_file, output_file, [append])` - Find Postman errors
- `extract_infrastructure_errors(log_file, output_file, [append])` - Find timeout/connection issues
- `extract_all_errors(log_file, output_file)` - Comprehensive extraction
- `extract_e2e_failures(log_file, output_file)` - E2E/Playwright failures
- `extract_test_summary(log_file, output_file, [append])` - Test run summary
- `find_assertion_with_context(log_file, pattern, [context_lines])` - Contextual search
- `extract_failed_test_names(log_file)` - List of failed test names
- `extract_stack_traces(log_file, output_file, [max_traces])` - Stack trace extraction
- `check_common_patterns(log_file)` - Detect common error patterns
- `extract_postman_failure_details(log_file, collection, output_file)` - Postman-specific details

**Usage Example:**
```bash
source .claude/skills/cicd-diagnostics/utils/log-analysis.sh

extract_all_errors "job-logs.txt" "error-summary.txt"
extract_failed_test_names "job-logs.txt" > "failed-tests.txt"
```

### analysis.sh
Failure classification and root cause determination.

**Key Functions:**
- `classify_failure_type(error_summary, jobs_file)` - Determine failure category
- `calculate_nightly_failure_rate(history_file)` - Compute failure statistics
- `compare_pr_mergequeue(pr_result, mq_result)` - Compare workflow results
- `is_recurring_failure(current_sha, recent_runs)` - Check if failure repeats
- `get_failure_frequency(recent_runs)` - Frequency analysis
- `assess_impact(failure_type, frequency, error_patterns)` - Impact level
- `determine_confidence(failure_type, error_count, evidence_quality)` - Confidence level
- `extract_evidence(error_summary)` - Generate evidence bullets
- `generate_recommendations(failure_type, error_summary)` - Actionable recommendations
- `estimate_resolution_time(failure_type, complexity)` - Time estimate

**Usage Example:**
```bash
source .claude/skills/cicd-diagnostics/utils/analysis.sh

FAILURE_TYPE=$(classify_failure_type "error-summary.txt" "jobs.json")
IMPACT=$(assess_impact "$FAILURE_TYPE" "Consistent (8/10)" "BUILD FAILURE")
generate_recommendations "$FAILURE_TYPE" "error-summary.txt"
```

## Integration with cicd-diagnostics Skill

The main SKILL.md references these utilities throughout the diagnostic workflow:

```bash
# Source utilities at the start of diagnostic
source .claude/skills/cicd-diagnostics/utils/github-api.sh
source .claude/skills/cicd-diagnostics/utils/workspace.sh
source .claude/skills/cicd-diagnostics/utils/log-analysis.sh
source .claude/skills/cicd-diagnostics/utils/analysis.sh

# Then use throughout the workflow
DIAGNOSTIC_DIR=$(get_diagnostic_workspace "$RUN_ID")
get_run_metadata "$RUN_ID" "$DIAGNOSTIC_DIR/run-metadata.json"
# ... etc
```

## Benefits of Modular Design

1. **Reusability** - Functions can be used by other skills or scripts
2. **Testability** - Each utility can be tested independently
3. **Maintainability** - Changes isolated to specific utility files
4. **Clarity** - Main skill logic is cleaner and more readable
5. **Composability** - Functions can be combined in different workflows

## Platform Compatibility

All utilities handle macOS/Linux differences automatically:
- Uses compatible `sed` syntax
- Handles `stat` command variations
- Cross-platform file operations

## Error Handling

All utilities use `set -euo pipefail` for robust error handling:
- `-e`: Exit on error
- `-u`: Error on undefined variables
- `-o pipefail`: Pipeline failures propagate

## Future Extensions

Potential additions to the utilities:
- `reporting.sh` - Report generation functions
- `github-issues.sh` - Issue creation and management
- `slack.sh` - Slack notification integration
- `metrics.sh` - Performance and reliability metrics