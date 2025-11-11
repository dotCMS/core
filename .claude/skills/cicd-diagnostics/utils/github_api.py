#!/usr/bin/env python3
"""GitHub API Utility Functions for CI/CD Diagnostics.

Provides reusable functions for interacting with GitHub API and CLI.
"""

import re
import subprocess
import json
from typing import Optional, Dict, Any, List
from pathlib import Path


def extract_run_id(url: str) -> Optional[str]:
    """Extract run ID from GitHub Actions URL.
    
    Args:
        url: GitHub Actions run URL
        
    Returns:
        Run ID or None if not found
    """
    match = re.search(r'/runs/(\d+)', url)
    return match.group(1) if match else None


def extract_pr_number(input_str: str) -> Optional[str]:
    """Extract PR number from URL or branch name.
    
    Args:
        input_str: PR URL or branch name
        
    Returns:
        PR number or None if not found
    """
    # Try pull URL pattern
    match = re.search(r'/pull/(\d+)', input_str)
    if match:
        return match.group(1)
    
    # Try branch name pattern (issue-123-feature-name)
    match = re.search(r'issue-(\d+)', input_str)
    if match:
        return match.group(1)
    
    return None


def get_run_metadata(run_id: str, output_file: Path) -> None:
    """Get workflow run metadata.
    
    Args:
        run_id: GitHub Actions run ID
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "run", "view", run_id,
            "--json", "conclusion,status,event,headBranch,headSha,workflowName,url,createdAt,updatedAt,displayTitle"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_jobs_detailed(run_id: str, output_file: Path) -> None:
    """Get all jobs for a workflow run with detailed step information.
    
    Args:
        run_id: GitHub Actions run ID
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "api",
            f"/repos/dotCMS/core/actions/runs/{run_id}/jobs",
            "--paginate"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_failed_jobs(jobs_file: Path) -> List[Dict[str, Any]]:
    """Get failed jobs from detailed jobs file.
    
    Args:
        jobs_file: Path to jobs JSON file
        
    Returns:
        List of failed job dictionaries
    """
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    return [job for job in jobs_data.get('jobs', []) if job.get('conclusion') == 'failure']


def get_canceled_jobs(jobs_file: Path) -> List[Dict[str, Any]]:
    """Get canceled jobs from detailed jobs file.
    
    Args:
        jobs_file: Path to jobs JSON file
        
    Returns:
        List of canceled job dictionaries
    """
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    return [job for job in jobs_data.get('jobs', []) if job.get('conclusion') == 'cancelled']


def download_job_logs(job_id: str, output_file: Path) -> None:
    """Download logs for a specific job.
    
    Args:
        job_id: GitHub Actions job ID
        output_file: Path to save logs
    """
    result = subprocess.run(
        [
            "gh", "api",
            f"/repos/dotCMS/core/actions/jobs/{job_id}/logs"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_pr_info(pr_num: str, output_file: Path) -> None:
    """Get PR information including status check rollup.
    
    Args:
        pr_num: PR number
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "pr", "view", pr_num,
            "--json", "number,headRefOid,headRefName,title,author,statusCheckRollup"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def find_failed_run_from_pr(pr_info_file: Path) -> Optional[str]:
    """Find failed run from PR info.
    
    Args:
        pr_info_file: Path to PR info JSON file
        
    Returns:
        Run ID or None if not found
    """
    pr_data = json.loads(pr_info_file.read_text(encoding='utf-8'))
    
    status_checks = pr_data.get('statusCheckRollup', [])
    for check in status_checks:
        if (check.get('conclusion') == 'FAILURE' and 
            check.get('workflowName') == '-1 PR Check'):
            details_url = check.get('detailsUrl', '')
            return extract_run_id(details_url)
    
    return None


def get_recent_runs(workflow_name: str, limit: int = 20, output_file: Optional[Path] = None) -> List[Dict[str, Any]]:
    """Get recent workflow runs.
    
    Args:
        workflow_name: Name of the workflow
        limit: Maximum number of runs to fetch
        output_file: Optional path to save JSON output
        
    Returns:
        List of run dictionaries
    """
    result = subprocess.run(
        [
            "gh", "run", "list",
            "--workflow", workflow_name,
            "--limit", str(limit),
            "--json", "databaseId,conclusion,headSha,displayTitle,createdAt"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    
    runs = json.loads(result.stdout)
    
    if output_file:
        output_file.write_text(result.stdout, encoding='utf-8')
    
    return runs


def get_artifacts(run_id: str, output_file: Path) -> None:
    """Get artifacts for a workflow run.
    
    Args:
        run_id: GitHub Actions run ID
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "api",
            f"/repos/dotCMS/core/actions/runs/{run_id}/artifacts",
            "--jq", ".artifacts[] | {name, id, size_in_bytes, expired}"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def search_issues(query: str, output_file: Optional[Path] = None) -> List[Dict[str, Any]]:
    """Search for related GitHub issues.
    
    Args:
        query: Search query
        output_file: Optional path to save JSON output
        
    Returns:
        List of issue dictionaries
    """
    result = subprocess.run(
        [
            "gh", "issue", "list",
            "--search", query,
            "--json", "number,title,state,labels,createdAt",
            "--limit", "10"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    
    issues = json.loads(result.stdout)
    
    if output_file:
        output_file.write_text(result.stdout, encoding='utf-8')
    
    return issues


def get_issue(issue_num: str, output_file: Path) -> None:
    """Get issue details.
    
    Args:
        issue_num: Issue number
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "issue", "view", issue_num,
            "--json", "title,body,labels,author"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def compare_commits(base_sha: str, head_sha: str, output_file: Path) -> None:
    """Compare two commits.
    
    Args:
        base_sha: Base commit SHA
        head_sha: Head commit SHA
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "api",
            f"/repos/dotCMS/core/compare/{base_sha}...{head_sha}",
            "--jq", ".commits[] | {sha: .sha[:7], message: .commit.message, author: .commit.author.name}"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_prs_for_branch(branch: str, output_file: Path) -> None:
    """Get PR list for current branch.
    
    Args:
        branch: Branch name
        output_file: Path to save JSON output
    """
    result = subprocess.run(
        [
            "gh", "pr", "list",
            "--head", branch,
            "--json", "number,url,headRefOid,title,author"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    output_file.write_text(result.stdout, encoding='utf-8')


def get_runs_for_commit(workflow_name: str, commit_sha: str, limit: int = 5) -> List[Dict[str, Any]]:
    """Get workflow runs for specific commit.
    
    Args:
        workflow_name: Name of the workflow
        commit_sha: Commit SHA
        limit: Maximum number of runs to fetch
        
    Returns:
        List of run dictionaries
    """
    result = subprocess.run(
        [
            "gh", "run", "list",
            "--workflow", workflow_name,
            "--commit", commit_sha,
            "--limit", str(limit),
            "--json", "databaseId,conclusion,status,displayTitle"
        ],
        capture_output=True,
        text=True,
        check=True
    )
    
    return json.loads(result.stdout)


def is_macos() -> bool:
    """Check if running on macOS."""
    import platform
    return platform.system() == "Darwin"


