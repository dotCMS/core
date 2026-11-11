#!/usr/bin/env bash
# Entry point for the workflow-lint CI job, which discovers *.test.sh suites.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$SCRIPT_DIR/update-license-date.test.py"
