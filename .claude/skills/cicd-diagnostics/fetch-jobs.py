#!/usr/bin/env python3
"""Fetch job details with caching."""

import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "utils"))
from github_api import get_jobs_detailed


def main():
    if len(sys.argv) < 3:
        print("Usage: python fetch-jobs.py <RUN_ID> <WORKSPACE>", file=sys.stderr)
        sys.exit(1)
    
    run_id = sys.argv[1]
    workspace = Path(sys.argv[2])
    
    if not workspace:
        print("ERROR: WORKSPACE parameter is required", file=sys.stderr)
        sys.exit(1)
    
    jobs_file = workspace / "jobs-detailed.json"
    
    # Fetch jobs if not cached
    if not jobs_file.exists():
        print("Fetching job details...")
        get_jobs_detailed(run_id, jobs_file)
        print(f"✓ Job details saved to {jobs_file}")
    else:
        print(f"✓ Using cached jobs: {jobs_file}")
    
    # Display failed jobs
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


