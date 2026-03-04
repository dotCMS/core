#!/usr/bin/env python3
"""Initialize diagnostic environment.

Usage: python init-diagnostic.py <RUN_ID>
Returns: Sets WORKSPACE environment variable and loads all utilities
"""

import sys
import os
from pathlib import Path

script_dir = Path(__file__).parent
sys.path.insert(0, str(script_dir / "utils"))

from workspace import get_diagnostic_workspace
from github_api import preflight_check, DiagnosticError


def main():
    if len(sys.argv) < 2:
        print("ERROR: Run ID required", file=sys.stderr)
        print("Usage: python init-diagnostic.py <RUN_ID>", file=sys.stderr)
        sys.exit(1)

    run_id = sys.argv[1]

    # Verify prerequisites before doing anything
    try:
        preflight_check()
    except DiagnosticError as e:
        print(f"PREFLIGHT FAILED: {e}", file=sys.stderr)
        sys.exit(1)

    workspace = get_diagnostic_workspace(run_id)

    print("Diagnostic environment initialized")
    print(f"   RUN_ID: {run_id}")
    print(f"   WORKSPACE: {workspace}")
    print(f"\nexport RUN_ID={run_id}")
    print(f"export WORKSPACE={workspace}")


if __name__ == "__main__":
    main()
