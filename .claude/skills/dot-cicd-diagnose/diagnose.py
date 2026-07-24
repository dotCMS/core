#!/usr/bin/env python3
"""CI/CD Diagnostic Entry Point.

Single entry point for all diagnostic operations. Supports progressive
investigation via subcommands, or full evidence gathering by default.

Usage:
    diagnose.py <RUN_ID_OR_URL>                      # Full gather (default)
    diagnose.py <RUN_ID_OR_URL> --metadata            # Metadata + jobs + step detail
    diagnose.py <RUN_ID_OR_URL> --jobs                # Jobs + step detail only (cached)
    diagnose.py <RUN_ID_OR_URL> --annotations         # Workflow annotations only
    diagnose.py <RUN_ID_OR_URL> --logs                # Download logs + error summary
    diagnose.py <RUN_ID_OR_URL> --logs <JOB_ID>       # Single job log + errors
    diagnose.py <RUN_ID_OR_URL> --evidence            # Full evidence.py analysis on logs
    diagnose.py <RUN_ID_OR_URL> --evidence <JOB_ID>   # evidence.py on single job log
"""

import sys
import json
import re
from pathlib import Path

# Add utils to path
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir / "utils"))

from github_api import (
    DiagnosticError, preflight_check, get_run_metadata,
    get_jobs_detailed, get_failed_jobs, download_job_logs
)
from workspace import get_diagnostic_workspace


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def extract_run_id(input_str: str) -> str:
    """Extract run ID from URL or plain number."""
    match = re.search(r'/runs/(\d+)', input_str)
    if match:
        return match.group(1)
    if input_str.strip().isdigit():
        return input_str.strip()
    print(f"ERROR: Cannot extract run ID from: {input_str}", file=sys.stderr)
    sys.exit(1)


def format_size(size_bytes: int) -> str:
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f}{unit}"
        size_bytes /= 1024.0
    return f"{size_bytes:.1f}TB"


def is_cleanup_step(step: dict) -> bool:
    """GitHub Actions cleanup steps that always run regardless of failure."""
    name = step.get('name', '')
    return name.startswith('Post ') or name == 'Complete job' or name == 'Set up job'


def print_section(title: str):
    print()
    print("=" * 80)
    print(title)
    print("=" * 80)


# ---------------------------------------------------------------------------
# Shared setup: preflight + workspace + ensure metadata & jobs are fetched
# ---------------------------------------------------------------------------

def setup(run_id: str, quiet: bool = False) -> tuple:
    """Run preflight, create workspace, fetch metadata + jobs if not cached.

    Returns (workspace, metadata, jobs_data, failed_jobs).
    """
    # Preflight
    try:
        preflight_check()
    except DiagnosticError as e:
        print(f"PREFLIGHT FAILED: {e}", file=sys.stderr)
        sys.exit(1)

    workspace = get_diagnostic_workspace(run_id)
    if not quiet:
        print(f"  Workspace: {workspace}")

    # Metadata (always needed for context)
    metadata_file = workspace / "run-metadata.json"
    if not metadata_file.exists():
        try:
            get_run_metadata(run_id, metadata_file)
        except DiagnosticError as e:
            print(f"ERROR fetching metadata: {e}", file=sys.stderr)
            sys.exit(1)
    metadata = json.loads(metadata_file.read_text(encoding='utf-8'))

    # Jobs (always needed for step detail)
    jobs_file = workspace / "jobs-detailed.json"
    if not jobs_file.exists():
        try:
            get_jobs_detailed(run_id, jobs_file)
        except DiagnosticError as e:
            print(f"ERROR fetching jobs: {e}", file=sys.stderr)
            sys.exit(1)
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    failed_jobs = [j for j in jobs_data.get('jobs', []) if j.get('conclusion') == 'failure']

    return workspace, metadata, jobs_data, failed_jobs


# ---------------------------------------------------------------------------
# Output functions
# ---------------------------------------------------------------------------

def show_metadata(metadata: dict):
    print_section("RUN METADATA")
    print(f"  Workflow:   {metadata.get('workflowName', 'unknown')}")
    print(f"  Conclusion: {metadata.get('conclusion', 'unknown')}")
    print(f"  Branch:     {metadata.get('headBranch', 'unknown')}")
    print(f"  Commit:     {metadata.get('headSha', 'unknown')[:12]}")
    print(f"  Created:    {metadata.get('createdAt', 'unknown')}")
    print(f"  URL:        {metadata.get('url', 'unknown')}")


def show_jobs(jobs_data: dict) -> list:
    """Print all jobs with status, then failed job step-level detail.
    Returns list of failed jobs."""
    jobs = jobs_data.get('jobs', [])

    print_section("ALL JOBS")
    for job in jobs:
        conclusion = job.get('conclusion', 'unknown')
        status = job.get('status', 'unknown')
        label = conclusion if conclusion else status
        print(f"  [{label:<12}] {job.get('name')}  (ID: {job.get('id')})")

    failed_jobs = [j for j in jobs if j.get('conclusion') == 'failure']
    if not failed_jobs:
        print("\nNo failed jobs found.")
        return failed_jobs

    print_section("FAILED JOB DETAILS")
    for job in failed_jobs:
        print(f"\n--- {job.get('name')} (ID: {job.get('id')}) ---")
        steps = job.get('steps', [])
        if not steps:
            print("  (no step data available)")
            continue

        # Track non-cleanup steps that ran (have any conclusion), not just succeeded.
        # A failed step followed by any later non-cleanup step that ran means the job
        # didn't stop at that step — i.e., it had continue-on-error.
        ran_non_cleanup = set()
        for step in steps:
            if step.get('conclusion') and not is_cleanup_step(step):
                ran_non_cleanup.add(step.get('number', 0))

        for step in steps:
            conclusion = step.get('conclusion', '')
            if not conclusion:
                continue
            number = step.get('number', 0)
            name = step.get('name', 'unknown')

            if conclusion == 'failure':
                later_ran = any(n > number for n in ran_non_cleanup)
                if later_ran:
                    marker = "FAIL (continue-on-error — did NOT cause job failure)"
                    print(f"  [{marker}] Step {number}: {name}")
                    print(f"         NOTE: Real error masked by continue-on-error. Worth investigating separately.")
                else:
                    marker = "FAIL <- likely caused job failure"
                    print(f"  [{marker}] Step {number}: {name}")
            elif conclusion == 'skipped':
                print(f"  [skipped     ] Step {number}: {name}")
            else:
                print(f"  [{conclusion:<12}] Step {number}: {name}")

    return failed_jobs


def show_annotations(run_id: str, workspace: Path):
    print_section("WORKFLOW ANNOTATIONS")
    try:
        from html_scraper import scrape_workflow_annotations, save_scraped_annotations
    except ImportError:
        print("  (annotation scraper not available)")
        return

    scraped_file = workspace / "workflow-annotations-scraped.json"
    if scraped_file.exists():
        scraped_data = json.loads(scraped_file.read_text(encoding='utf-8'))
    else:
        print("  Scraping workflow annotations...")
        scraped_data = scrape_workflow_annotations(run_id)
        save_scraped_annotations(run_id, workspace, scraped_data)

    annotations = scraped_data.get('workflow_annotations', [])
    if annotations:
        print(f"  Found {len(annotations)} annotation(s):")
        for ann in annotations:
            level = ann.get('level', 'unknown')
            msg = ann.get('message', 'no message')
            # Strip HTML tags for readability
            msg = re.sub(r'<[^>]+>', '', msg)
            loc = ann.get('location', '')
            print(f"    [{level}] {loc}: {msg}" if loc else f"    [{level}] {msg}")
    else:
        if scraped_data.get('error'):
            print(f"  Error scraping: {scraped_data['error']}")
        else:
            print("  No workflow syntax errors found.")


def download_logs(workspace: Path, failed_jobs: list, specific_job_id: str = None):
    """Download logs for failed jobs. If specific_job_id given, download only that one."""
    targets = failed_jobs
    if specific_job_id:
        targets = [j for j in failed_jobs if str(j['id']) == specific_job_id]
        if not targets:
            # Maybe it's not a failed job — try downloading anyway
            targets = [{'id': int(specific_job_id), 'name': f'Job {specific_job_id}'}]

    print_section("DOWNLOADING JOB LOGS")
    for job in targets:
        job_id = str(job['id'])
        job_name = job.get('name', 'Unknown')
        log_file = workspace / f"failed-job-{job_id}.txt"

        if not log_file.exists() or log_file.stat().st_size == 0:
            try:
                download_job_logs(job_id, log_file)
                size = log_file.stat().st_size
                print(f"  Downloaded: {job_name} ({format_size(size)})")
            except Exception as e:
                print(f"  FAILED: {job_name}: {e}")
        else:
            size = log_file.stat().st_size
            print(f"  Cached: {job_name} ({format_size(size)})")

    return targets


def show_log_errors(workspace: Path, jobs: list):
    """Extract and print ##[error] lines from job logs."""
    print_section("ERROR SUMMARY FROM LOGS")
    for job in jobs:
        job_id = str(job['id'])
        job_name = job.get('name', 'Unknown')
        log_file = workspace / f"failed-job-{job_id}.txt"

        if not log_file.exists():
            print(f"\n--- {job_name}: log not downloaded ---")
            continue

        print(f"\n--- Errors in: {job_name} (ID: {job_id}) ---")
        size = log_file.stat().st_size
        print(f"  Log size: {format_size(size)}")

        lines = log_file.read_text(encoding='utf-8', errors='ignore').split('\n')
        error_lines = []
        # Patterns that indicate errors (beyond just ##[error])
        error_patterns = ['##[error]', 'npm error', 'npm ERR!', 'FATAL:', 'BUILD FAILURE']
        for i, line in enumerate(lines):
            if any(pat in line for pat in error_patterns):
                clean = re.sub(r'^\d{4}-\d{2}-\d{2}T[\d:.]+Z\s*', '', line)
                error_lines.append((i + 1, clean.strip()))

        if error_lines:
            print(f"  Found {len(error_lines)} error(s):")
            for line_num, msg in error_lines:
                print(f"    Line {line_num}: {msg}")
        else:
            print("  No error lines found in log.")


def show_evidence(workspace: Path, jobs: list):
    """Run full evidence.py analysis on downloaded logs."""
    try:
        from evidence import present_complete_diagnostic, get_log_stats
    except ImportError:
        print("  ERROR: evidence.py not available", file=sys.stderr)
        return

    print_section("FULL EVIDENCE ANALYSIS")
    for job in jobs:
        job_id = str(job['id'])
        job_name = job.get('name', 'Unknown')
        log_file = workspace / f"failed-job-{job_id}.txt"

        if not log_file.exists():
            print(f"\n--- {job_name}: log not downloaded (run with --logs first) ---")
            continue

        print(f"\n--- Evidence for: {job_name} ---")
        print(get_log_stats(log_file))
        print(present_complete_diagnostic(log_file, workspace))


def show_summary(workspace: Path, run_id: str, failed_jobs: list):
    print_section("EVIDENCE GATHERED — READY FOR ANALYSIS")
    print(f"  Workspace: {workspace}")
    print(f"  Run ID:    {run_id}")
    print(f"  Failed jobs: {len(failed_jobs)}")
    if failed_jobs:
        for job in failed_jobs:
            print(f"    - {job.get('name')} (ID: {job.get('id')})")
        print()
        print("  Log files available for deeper analysis:")
        for job in failed_jobs:
            log_file = workspace / f"failed-job-{job['id']}.txt"
            if log_file.exists():
                print(f"    {log_file}")
    print()
    print("  Use the step-level detail above to identify the PRIMARY failure.")
    print("  Steps marked 'FAIL <- likely caused job failure' are the root cause.")
    print("  Steps marked 'continue-on-error' are secondary findings (real bugs, but masked).")
    print("  ##[error] lines that don't match any failed step may be from continue-on-error steps")
    print("  (see WORKFLOWS.md for which deployment steps have continue-on-error).")


# ---------------------------------------------------------------------------
# Subcommand dispatch
# ---------------------------------------------------------------------------

def parse_args():
    """Parse arguments into (run_id, subcommand, extra_arg)."""
    args = sys.argv[1:]

    if not args:
        print("Usage: diagnose.py <RUN_ID_OR_URL> [--metadata|--jobs|--annotations|--logs|--evidence] [JOB_ID]", file=sys.stderr)
        print()
        print("Subcommands (progressive investigation):")
        print("  (none)          Full evidence gathering (default)")
        print("  --metadata      Run metadata + jobs + step-level detail")
        print("  --jobs          Jobs + step-level detail only (uses cache)")
        print("  --annotations   Workflow annotations only")
        print("  --logs          Download failed job logs + error summary")
        print("  --logs JOB_ID   Download single job log + errors")
        print("  --evidence      Full evidence.py analysis on all failed job logs")
        print("  --evidence ID   Full evidence.py analysis on single job log")
        print()
        print("Examples:")
        print("  diagnose.py 23469253088")
        print("  diagnose.py https://github.com/dotCMS/core/actions/runs/23469253088")
        print("  diagnose.py 23469253088 --metadata")
        print("  diagnose.py 23469253088 --logs 68289775927")
        sys.exit(1)

    # First arg is always run ID/URL
    run_input = args[0]

    # Find subcommand
    subcommand = None
    extra_arg = None
    for i, arg in enumerate(args[1:], 1):
        if arg.startswith('--'):
            subcommand = arg
            # Check if next arg exists and is not another flag
            if i + 1 < len(args) and not args[i + 1].startswith('--'):
                extra_arg = args[i + 1]
            break

    return run_input, subcommand, extra_arg


def main():
    run_input, subcommand, extra_arg = parse_args()
    run_id = extract_run_id(run_input)

    workspace, metadata, jobs_data, failed_jobs = setup(run_id)

    if subcommand == '--metadata':
        show_metadata(metadata)
        show_jobs(jobs_data)

    elif subcommand == '--jobs':
        show_jobs(jobs_data)

    elif subcommand == '--annotations':
        show_annotations(run_id, workspace)

    elif subcommand == '--logs':
        targets = download_logs(workspace, failed_jobs, extra_arg)
        show_log_errors(workspace, targets)

    elif subcommand == '--evidence':
        # Ensure logs are downloaded first
        if extra_arg:
            targets = download_logs(workspace, failed_jobs, extra_arg)
        else:
            targets = download_logs(workspace, failed_jobs)
        show_evidence(workspace, targets)

    else:
        # Default: full gather
        show_metadata(metadata)
        show_jobs(jobs_data)
        show_annotations(run_id, workspace)
        if failed_jobs:
            download_logs(workspace, failed_jobs)
            show_log_errors(workspace, failed_jobs)
        show_summary(workspace, run_id, failed_jobs)


if __name__ == "__main__":
    main()
