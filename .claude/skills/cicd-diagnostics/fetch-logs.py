#!/usr/bin/env python3
"""Fetch failed job logs with caching."""

import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "utils"))
from github_api import download_job_logs, get_failed_jobs


def format_size(size_bytes: int) -> str:
    """Format size in human-readable format."""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f}{unit}"
        size_bytes /= 1024.0
    return f"{size_bytes:.1f}TB"


def main():
    if len(sys.argv) < 3:
        print("Usage: python fetch-logs.py <RUN_ID> <WORKSPACE> [JOB_ID]", file=sys.stderr)
        print("", file=sys.stderr)
        print("Example:", file=sys.stderr)
        print("  python fetch-logs.py 19219835536 /path/to/workspace", file=sys.stderr)
        print("  python fetch-logs.py 19219835536 /path/to/workspace 54939324205", file=sys.stderr)
        sys.exit(1)

    run_id = sys.argv[1]
    workspace_path = sys.argv[2]

    # Optional job ID parameter
    specific_job_id = sys.argv[3] if len(sys.argv) > 3 else None

    # Validate parameters are not swapped (workspace should be a path, not just digits)
    # A workspace path will contain slashes or be a relative path like "workspace"
    # A job ID will be only digits
    if workspace_path.isdigit() and len(workspace_path) > 10:
        print(f"ERROR: WORKSPACE parameter appears to be a job ID: {workspace_path}", file=sys.stderr)
        print("", file=sys.stderr)
        print("Correct usage: python fetch-logs.py <RUN_ID> <WORKSPACE_PATH> [JOB_ID]", file=sys.stderr)
        print(f"  RUN_ID: {run_id}", file=sys.stderr)
        print(f"  WORKSPACE_PATH: should be a directory path (e.g., /path/to/workspace)", file=sys.stderr)
        print(f"  JOB_ID (optional): {workspace_path} <- you may have meant this as job ID", file=sys.stderr)
        sys.exit(1)

    workspace = Path(workspace_path)

    if not workspace.exists():
        print(f"ERROR: Workspace directory does not exist: {workspace}", file=sys.stderr)
        print(f"", file=sys.stderr)
        print(f"Make sure the workspace path is correct. You passed:", file=sys.stderr)
        print(f"  RUN_ID: {run_id}", file=sys.stderr)
        print(f"  WORKSPACE: {workspace_path}", file=sys.stderr)
        if specific_job_id:
            print(f"  JOB_ID: {specific_job_id}", file=sys.stderr)
        sys.exit(1)

    jobs_file = workspace / "jobs-detailed.json"
    if not jobs_file.exists():
        print(f"ERROR: Jobs file not found: {jobs_file}", file=sys.stderr)
        print("Run fetch-jobs.py first to get job details.", file=sys.stderr)
        sys.exit(1)

    # Get failed jobs
    failed_jobs = get_failed_jobs(jobs_file)

    if not failed_jobs:
        print("No failed jobs found.")
        return

    # If specific job ID provided, filter to that job
    if specific_job_id:
        failed_jobs = [job for job in failed_jobs if str(job['id']) == specific_job_id]
        if not failed_jobs:
            print(f"ERROR: Job {specific_job_id} not found or not failed", file=sys.stderr)
            sys.exit(1)

    # Download logs for each failed job
    for job in failed_jobs:
        job_id = str(job['id'])
        job_name = job.get('name', 'Unknown')
        log_file = workspace / f"failed-job-{job_id}.txt"

        # Download logs if not cached or empty
        if not log_file.exists() or log_file.stat().st_size == 0:
            print(f"Downloading logs for job {job_id} ({job_name})...")
            try:
                download_job_logs(job_id, log_file)
                size = log_file.stat().st_size
                print(f"✓ Downloaded: {format_size(size)} -> {log_file}")
            except Exception as e:
                print(f"✗ Failed to download logs for job {job_id}: {e}", file=sys.stderr)
        else:
            size = log_file.stat().st_size
            print(f"✓ Using cached logs: {format_size(size)} -> {log_file}")


if __name__ == "__main__":
    main()


