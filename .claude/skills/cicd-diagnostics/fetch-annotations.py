#!/usr/bin/env python3
"""Fetch workflow run annotations (syntax errors, validation failures).

Usage: python fetch-annotations.py <RUN_ID> <WORKSPACE>

Annotations show GitHub Actions workflow syntax validation errors that are
visible in the UI but not in job logs. These explain why jobs were skipped
or never evaluated due to workflow file syntax errors.

Example: python fetch-annotations.py 19131365567 /path/to/.claude/diagnostics/run-19131365567
"""

import sys
import json
from pathlib import Path

# Add utils to path
script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir / "utils"))

from github_api import get_workflow_run_annotations
from workspace import validate_workspace


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
    if not validate_workspace(workspace):
        print(f"ERROR: Invalid workspace path: {workspace}", file=sys.stderr)
        print("Workspace must be a valid directory", file=sys.stderr)
        sys.exit(1)

    output_file = workspace / "annotations.json"

    # Check cache
    if output_file.exists():
        print(f"✅ Using cached annotations from {output_file}")
        annotations = json.loads(output_file.read_text(encoding='utf-8'))
    else:
        print(f"📡 Fetching annotations for run {run_id}...")
        annotations = get_workflow_run_annotations(run_id, output_file)
        print(f"✅ Saved annotations to {output_file}")

    # Display summary
    if not annotations:
        print("\n✅ No workflow annotations found (workflow syntax is valid)")
    else:
        print(f"\n⚠️  Found {len(annotations)} workflow annotation(s):")
        print("")

        # Group by annotation level
        by_level = {}
        for annotation in annotations:
            level = annotation.get('annotation_level', 'unknown')
            if level not in by_level:
                by_level[level] = []
            by_level[level].append(annotation)

        # Display by severity
        for level in ['failure', 'warning', 'notice', 'unknown']:
            if level in by_level:
                print(f"  {level.upper()}:")
                for annotation in by_level[level]:
                    path = annotation.get('path', 'unknown')
                    line = annotation.get('start_line', '?')
                    col = annotation.get('start_column', '?')
                    message = annotation.get('message', 'No message')
                    title = annotation.get('title', '')

                    if title:
                        print(f"    {path} (Line: {line}, Col: {col}): {title}")
                        print(f"      → {message}")
                    else:
                        print(f"    {path} (Line: {line}, Col: {col}): {message}")
                print("")

        print("💡 Workflow annotations explain why jobs may have been skipped or never evaluated.")
        print("   These errors are visible in the GitHub UI but not in job logs.")


if __name__ == "__main__":
    main()