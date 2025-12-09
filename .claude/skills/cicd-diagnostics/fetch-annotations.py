#!/usr/bin/env python3
"""Fetch workflow run annotations (syntax errors, validation failures).

Usage: python fetch-annotations.py <RUN_ID> <WORKSPACE>

Annotations show GitHub Actions workflow syntax validation errors that are
visible in the UI but not in job logs. These explain why jobs were skipped
or never evaluated due to workflow file syntax errors.

IMPORTANT: GitHub's REST API does NOT expose workflow syntax validation errors.
These errors are only visible in the GitHub UI, so this script scrapes the HTML
directly to extract them.

Example: python fetch-annotations.py 19131365567 /path/to/.claude/diagnostics/run-19131365567
"""

import sys
import json
from pathlib import Path

# Add utils to path
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir / "utils"))

from html_scraper import scrape_workflow_annotations, save_scraped_annotations, format_scraped_annotations_report


def main():
    if len(sys.argv) < 3:
        print("ERROR: RUN_ID and WORKSPACE parameters required", file=sys.stderr)
        print("Usage: python fetch-annotations.py <RUN_ID> <WORKSPACE>", file=sys.stderr)
        print("", file=sys.stderr)
        print("Example:", file=sys.stderr)
        print("  python fetch-annotations.py 19131365567 /path/to/.claude/diagnostics/run-19131365567", file=sys.stderr)
        sys.exit(1)

    run_id = sys.argv[1]
    workspace = Path(sys.argv[2])

    # Validate workspace
    if not workspace.exists() or not workspace.is_dir():
        print(f"ERROR: Invalid workspace path: {workspace}", file=sys.stderr)
        print("Workspace must be a valid directory", file=sys.stderr)
        sys.exit(1)

    scraped_file = workspace / "workflow-annotations-scraped.json"

    # Note: We skip the GitHub API because it does NOT return workflow syntax validation errors.
    # The API only returns job-level annotations (things that happened during job execution),
    # but workflow syntax errors prevent jobs from being created in the first place.
    # These errors are only visible in the GitHub UI, so we scrape the HTML directly.

    # Scrape workflow-level annotations from HTML (primary source)
    print("=" * 80)
    print("Fetching workflow annotations from GitHub UI (HTML)")
    print("=" * 80)
    print("ℹ️  Note: GitHub API does NOT expose workflow syntax validation errors")
    print("    We scrape the HTML directly to find these critical errors")
    print()
    # Check cache
    if scraped_file.exists():
        print(f"✅ Using cached annotations from {scraped_file}")
        scraped_data = json.loads(scraped_file.read_text(encoding='utf-8'))
    else:
        print(f"🌐 Scraping workflow annotations for run {run_id}...")
        print("⚠️  WARNING: HTML scraping is fragile and may break if GitHub changes their UI")
        print()
        scraped_data = scrape_workflow_annotations(run_id)
        save_scraped_annotations(run_id, workspace, scraped_data)

    # Display annotations
    workflow_annotations = scraped_data.get('workflow_annotations', [])

    if workflow_annotations:
        print(f"\n📊 Found {len(workflow_annotations)} annotation(s):")
        print(format_scraped_annotations_report(scraped_data))
    else:
        if scraped_data.get('error'):
            print(f"\n❌ Error during HTML scraping: {scraped_data['error']}")
        else:
            print(f"\n✅ No workflow syntax errors found")

    # Summary
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)

    total_annotations = len(workflow_annotations)

    print(f"\nTotal annotations found: {total_annotations}")

    # Group by severity
    if total_annotations > 0:
        by_level = {}
        for ann in workflow_annotations:
            level = ann.get('level', 'unknown')
            by_level[level] = by_level.get(level, 0) + 1

        for level in ['failure', 'warning', 'notice']:
            if level in by_level:
                print(f"  • {level.capitalize()}: {by_level[level]}")

    if total_annotations == 0:
        print("\n✅ No annotations found - workflow syntax is valid!")
    else:
        print("\n💡 Annotations explain why jobs may have been skipped or never evaluated.")
        print("   Workflow syntax errors prevent jobs from being created in the first place.")

    print(f"\nAnnotation data saved to: {scraped_file}")
    print("=" * 80)


if __name__ == "__main__":
    main()