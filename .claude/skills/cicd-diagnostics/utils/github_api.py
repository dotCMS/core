#!/usr/bin/env python3
"""GitHub API Utility Functions for CI/CD Diagnostics.

Provides reusable functions for interacting with GitHub API and CLI.
"""

import re
import subprocess
import json
import shutil
import sys
from typing import Optional, Dict, Any, List
from pathlib import Path


class DiagnosticError(Exception):
    """Raised when a diagnostic operation fails with a clear, actionable message."""
    pass


def preflight_check() -> None:
    """Verify all required tools are available and authenticated.

    Raises DiagnosticError with actionable resolution if any check fails.
    Call this once at the start of any diagnostic session.
    """
    if sys.version_info < (3, 8):
        raise DiagnosticError(
            f"Python 3.8+ required, found {sys.version_info.major}.{sys.version_info.minor}. "
            "Install a newer Python: brew install python3"
        )

    if not shutil.which("gh"):
        raise DiagnosticError(
            "GitHub CLI (gh) not found on PATH. "
            "Install it: brew install gh (macOS) or see https://cli.github.com/"
        )

    result = subprocess.run(
        ["gh", "auth", "status"],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        raise DiagnosticError(
            "GitHub CLI is not authenticated. Run: gh auth login\n"
            f"Details: {result.stderr.strip()}"
        )

    result = subprocess.run(
        ["gh", "api", "/repos/dotCMS/core", "--jq", ".full_name"],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        stderr = result.stderr.strip()
        if "403" in stderr or "rate limit" in stderr.lower():
            raise DiagnosticError(
                "GitHub API rate limit exceeded. Wait a few minutes or check your token scopes.\n"
                f"Details: {stderr}"
            )
        raise DiagnosticError(
            f"Cannot access dotCMS/core repository. Check permissions.\nDetails: {stderr}"
        )


def _run_gh(args: list, error_context: str = "") -> subprocess.CompletedProcess:
    """Run a gh CLI command with clear error handling.

    Args:
        args: Command arguments (e.g., ["gh", "run", "view", ...])
        error_context: Human-readable description for error messages.

    Returns:
        CompletedProcess result

    Raises:
        DiagnosticError: With actionable message on failure
    """
    try:
        result = subprocess.run(args, capture_output=True, text=True, check=True)
        return result
    except FileNotFoundError:
        raise DiagnosticError(
            f"Command not found: {args[0]}. Is GitHub CLI installed? "
            "Install: brew install gh"
        )
    except subprocess.CalledProcessError as e:
        stderr = e.stderr.strip()
        if "401" in stderr or "authentication" in stderr.lower():
            raise DiagnosticError(
                f"Authentication failed while {error_context or 'calling GitHub API'}. "
                f"Run: gh auth login\nDetails: {stderr}"
            )
        if "403" in stderr or "rate limit" in stderr.lower():
            raise DiagnosticError(
                f"Rate limit hit while {error_context or 'calling GitHub API'}. "
                f"Wait a few minutes and retry.\nDetails: {stderr}"
            )
        if "404" in stderr or "Not Found" in stderr:
            raise DiagnosticError(
                f"Resource not found while {error_context or 'calling GitHub API'}. "
                f"Check the run ID / job ID is correct.\nDetails: {stderr}"
            )
        raise DiagnosticError(
            f"GitHub CLI failed while {error_context or 'running command'}.\n"
            f"Command: {' '.join(args)}\n"
            f"Exit code: {e.returncode}\n"
            f"Error: {stderr}"
        )


def extract_run_id(url: str) -> Optional[str]:
    """Extract run ID from GitHub Actions URL."""
    match = re.search(r'/runs/(\d+)', url)
    return match.group(1) if match else None


def extract_pr_number(input_str: str) -> Optional[str]:
    """Extract PR number from URL or branch name."""
    match = re.search(r'/pull/(\d+)', input_str)
    if match:
        return match.group(1)
    match = re.search(r'issue-(\d+)', input_str)
    if match:
        return match.group(1)
    return None


def get_run_metadata(run_id: str, output_file: Path) -> None:
    """Get workflow run metadata."""
    result = _run_gh(
        [
            "gh", "run", "view", run_id,
            "--json", "conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle"
        ],
        error_context=f"fetching metadata for run {run_id}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_jobs_detailed(run_id: str, output_file: Path) -> None:
    """Get all jobs for a workflow run with detailed step information."""
    result = _run_gh(
        [
            "gh", "api",
            f"/repos/dotCMS/core/actions/runs/{run_id}/jobs",
            "--paginate"
        ],
        error_context=f"fetching jobs for run {run_id}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_failed_jobs(jobs_file: Path) -> List[Dict[str, Any]]:
    """Get failed jobs from detailed jobs file."""
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    return [job for job in jobs_data.get('jobs', []) if job.get('conclusion') == 'failure']


def get_canceled_jobs(jobs_file: Path) -> List[Dict[str, Any]]:
    """Get canceled jobs from detailed jobs file."""
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    return [job for job in jobs_data.get('jobs', []) if job.get('conclusion') == 'cancelled']


def download_job_logs(job_id: str, output_file: Path) -> None:
    """Download logs for a specific job."""
    result = _run_gh(
        [
            "gh", "api",
            f"/repos/dotCMS/core/actions/jobs/{job_id}/logs"
        ],
        error_context=f"downloading logs for job {job_id}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_pr_info(pr_num: str, output_file: Path) -> None:
    """Get PR information including status check rollup."""
    result = _run_gh(
        [
            "gh", "pr", "view", pr_num,
            "--json", "number,headRefOid,headRefName,title,author,statusCheckRollup"
        ],
        error_context=f"fetching PR #{pr_num} info"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def find_failed_run_from_pr(pr_info_file: Path) -> Optional[str]:
    """Find failed run from PR info."""
    pr_data = json.loads(pr_info_file.read_text(encoding='utf-8'))
    status_checks = pr_data.get('statusCheckRollup', [])
    for check in status_checks:
        if (check.get('conclusion') == 'FAILURE' and
            check.get('workflowName') == '-1 PR Check'):
            details_url = check.get('detailsUrl', '')
            return extract_run_id(details_url)
    return None


def get_recent_runs(workflow_name: str, limit: int = 20, output_file: Optional[Path] = None) -> List[Dict[str, Any]]:
    """Get recent workflow runs."""
    result = _run_gh(
        [
            "gh", "run", "list",
            "--workflow", workflow_name,
            "--limit", str(limit),
            "--json", "databaseId,conclusion,headSha,displayTitle,createdAt"
        ],
        error_context=f"listing recent runs for {workflow_name}"
    )
    runs = json.loads(result.stdout)
    if output_file:
        output_file.write_text(result.stdout, encoding='utf-8')
    return runs


def get_artifacts(run_id: str, output_file: Path) -> None:
    """Get artifacts for a workflow run."""
    result = _run_gh(
        [
            "gh", "api",
            f"/repos/dotCMS/core/actions/runs/{run_id}/artifacts",
            "--jq", ".artifacts[] | {name, id, size_in_bytes, expired}"
        ],
        error_context=f"fetching artifacts for run {run_id}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def search_issues(query: str, output_file: Optional[Path] = None) -> List[Dict[str, Any]]:
    """Search for related GitHub issues."""
    result = _run_gh(
        [
            "gh", "issue", "list",
            "--search", query,
            "--json", "number,title,state,labels,createdAt",
            "--limit", "10"
        ],
        error_context=f"searching issues for '{query}'"
    )
    issues = json.loads(result.stdout)
    if output_file:
        output_file.write_text(result.stdout, encoding='utf-8')
    return issues


def get_issue(issue_num: str, output_file: Path) -> None:
    """Get issue details."""
    result = _run_gh(
        [
            "gh", "issue", "view", issue_num,
            "--json", "title,body,labels,author"
        ],
        error_context=f"fetching issue #{issue_num}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def compare_commits(base_sha: str, head_sha: str, output_file: Path) -> None:
    """Compare two commits."""
    result = _run_gh(
        [
            "gh", "api",
            f"/repos/dotCMS/core/compare/{base_sha}...{head_sha}",
            "--jq", ".commits[] | {sha: .sha[:7], message: .commit.message, author: .commit.author.name}"
        ],
        error_context=f"comparing commits {base_sha[:7]}...{head_sha[:7]}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_prs_for_branch(branch: str, output_file: Path) -> None:
    """Get PR list for current branch."""
    result = _run_gh(
        [
            "gh", "pr", "list",
            "--head", branch,
            "--json", "number,url,headRefOid,title,author"
        ],
        error_context=f"listing PRs for branch {branch}"
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_runs_for_commit(workflow_name: str, commit_sha: str, limit: int = 5) -> List[Dict[str, Any]]:
    """Get workflow runs for specific commit."""
    result = _run_gh(
        [
            "gh", "run", "list",
            "--workflow", workflow_name,
            "--commit", commit_sha,
            "--limit", str(limit),
            "--json", "databaseId,conclusion,status,displayTitle"
        ],
        error_context=f"listing runs for commit {commit_sha[:7]} in {workflow_name}"
    )
    return json.loads(result.stdout)


def is_macos() -> bool:
    """Check if running on macOS."""
    import platform
    return platform.system() == "Darwin"


def get_workflow_run_annotations(run_id: str, output_file: Optional[Path] = None) -> List[Dict[str, Any]]:
    """Get workflow run annotations (syntax errors, validation failures, etc.).

    Annotations include GitHub Actions workflow syntax validation errors that are
    shown in the UI but not in job logs. These can indicate why jobs were skipped
    or never evaluated.
    """
    try:
        run_result = _run_gh(
            ["gh", "api", f"/repos/dotCMS/core/actions/runs/{run_id}", "--jq", ".check_suite_id"],
            error_context=f"fetching check suite ID for run {run_id}"
        )
        check_suite_id = run_result.stdout.strip()
        if not check_suite_id:
            return []

        check_runs_result = _run_gh(
            ["gh", "api", f"/repos/dotCMS/core/check-suites/{check_suite_id}/check-runs", "--paginate"],
            error_context=f"fetching check runs for suite {check_suite_id}"
        )
        check_runs_data = json.loads(check_runs_result.stdout)

        all_annotations = []
        for check_run in check_runs_data.get('check_runs', []):
            check_run_id = check_run.get('id')
            if not check_run_id:
                continue
            try:
                ann_result = _run_gh(
                    ["gh", "api", f"/repos/dotCMS/core/check-runs/{check_run_id}/annotations", "--paginate"],
                    error_context=f"fetching annotations for check run {check_run_id}"
                )
                annotations = json.loads(ann_result.stdout)
                if isinstance(annotations, list):
                    all_annotations.extend(annotations)
            except (DiagnosticError, json.JSONDecodeError):
                continue

        if output_file:
            output_file.write_text(json.dumps(all_annotations, indent=2), encoding='utf-8')
        return all_annotations

    except (DiagnosticError, json.JSONDecodeError, KeyError):
        return []


def get_skipped_jobs(jobs_file: Path) -> List[Dict[str, Any]]:
    """Get skipped jobs from detailed jobs file."""
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    return [job for job in jobs_data.get('jobs', []) if job.get('conclusion') == 'skipped']


def categorize_job_states(jobs_file: Path) -> Dict[str, List[Dict[str, Any]]]:
    """Categorize jobs by their state."""
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    jobs = jobs_data.get('jobs', [])

    categorized = {
        'failed': [], 'skipped': [], 'cancelled': [],
        'success': [], 'in_progress': [], 'queued': [], 'never_evaluated': []
    }

    for job in jobs:
        conclusion = job.get('conclusion')
        status = job.get('status')
        if conclusion == 'failure':
            categorized['failed'].append(job)
        elif conclusion == 'skipped':
            categorized['skipped'].append(job)
        elif conclusion == 'cancelled':
            categorized['cancelled'].append(job)
        elif conclusion == 'success':
            categorized['success'].append(job)
        elif status == 'in_progress':
            categorized['in_progress'].append(job)
        elif status == 'queued':
            categorized['queued'].append(job)
        else:
            if not conclusion and status == 'completed':
                categorized['never_evaluated'].append(job)

    return categorized
