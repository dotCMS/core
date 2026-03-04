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

    print("")
    print("=== Failed Jobs ===")
    jobs_data = json.loads(jobs_file.read_text(encoding='utf-8'))
    jobs = jobs_data.get('jobs', [])

    for job in jobs:
        if job.get('conclusion') == 'failure':
            print(f"Name: {job.get('name')}")
            print(f"ID: {job.get('id')}")
            print(f"Conclusion: {job.get('conclusion')}")
            print("")


if __name__ == "__main__":
    main()
