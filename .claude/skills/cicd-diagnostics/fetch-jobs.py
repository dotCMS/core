#!/usr/bin/env python3
"""Fetch job details with caching."""

import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "utils"))
from github_api import get_jobs_detailed, DiagnosticError


def main():
    if len(sys.argv) < 3:
        print("Usage: python fetch-jobs.py <RUN_ID> <WORKSPACE>", file=sys.stderr)
        sys.exit(1)

    run_id = sys.argv[1]
    workspace = Path(sys.argv[2])

    if not workspace.exists():
        print(f"ERROR: Workspace directory does not exist: {workspace}", file=sys.stderr)
        print("Run init-diagnostic.py first to create the workspace.", file=sys.stderr)
        sys.exit(1)

    jobs_file = workspace / "jobs-detailed.json"

    # Fetch jobs if not cached
    if not jobs_file.exists():
        print("Fetching job details...")
        try:
            get_jobs_detailed(run_id, jobs_file)
        except DiagnosticError as e:
            print(f"ERROR: {e}", file=sys.stderr)
            sys.exit(1)
        print(f"Job details saved to {jobs_file}")
    else:
        print(f"Using cached jobs: {jobs_file}")

    # Display all jobs with status, then detail failed jobs with step-level info
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    jobs = jobs_data.get('jobs', [])

    print("")
    print("=== All Jobs ===")
    for job in jobs:
        conclusion = job.get('conclusion', 'unknown')
        status = job.get('status', 'unknown')
        label = conclusion if conclusion else status
        print(f"  [{label:<12}] {job.get('name')}  (ID: {job.get('id')})")

    failed_jobs = [j for j in jobs if j.get('conclusion') == 'failure']
    if not failed_jobs:
        print("\nNo failed jobs found.")
        return

    print("")
    print("=== Failed Job Details ===")
    for job in failed_jobs:
        print(f"\n--- {job.get('name')} (ID: {job.get('id')}) ---")
        steps = job.get('steps', [])
        if not steps:
            print("  (no step data available)")
            continue

        # Identify which steps failed and whether non-cleanup steps ran after
        # GitHub Actions always runs "Post ..." and "Complete job" cleanup steps
        # even after a failure, so we exclude those from continue-on-error detection.
        def is_cleanup_step(s):
            n = s.get('name', '')
            return n.startswith('Post ') or n == 'Complete job' or n == 'Set up job'

        # Build list of non-cleanup steps that succeeded, keyed by number
        succeeded_non_cleanup = set()
        for step in steps:
            if (step.get('conclusion') == 'success'
                    and not is_cleanup_step(step)):
                succeeded_non_cleanup.add(step.get('number', 0))

        for step in steps:
            conclusion = step.get('conclusion', '')
            if not conclusion:
                continue
            number = step.get('number', 0)
            name = step.get('name', 'unknown')

            if conclusion == 'failure':
                # continue-on-error if a non-cleanup step succeeded AFTER this one
                later_succeeded = any(n > number for n in succeeded_non_cleanup)
                if later_succeeded:
                    marker = "FAIL (continue-on-error — did NOT cause job failure)"
                    print(f"  [{marker}] Step {number}: {name}")
                    print(f"         ↳ NOTE: This is a real error masked by continue-on-error. Worth investigating separately.")
                else:
                    marker = "FAIL ← likely caused job failure"
                    print(f"  [{marker}] Step {number}: {name}")
            elif conclusion == 'skipped':
                print(f"  [skipped     ] Step {number}: {name}")
            else:
                print(f"  [{conclusion:<12}] Step {number}: {name}")


if __name__ == "__main__":
    main()
