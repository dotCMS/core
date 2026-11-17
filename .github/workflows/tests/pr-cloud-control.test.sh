#!/usr/bin/env bash
set -euo pipefail
node --test "$(dirname "$0")/pr-cloud-control.test.cjs"
