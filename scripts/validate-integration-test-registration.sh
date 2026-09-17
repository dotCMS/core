#!/bin/bash

# Integration Test Registration Validation Script
#
# An integration test class that is not listed in a MainSuite*/Junit5Suite*
# @SuiteClasses array compiles fine and is silently never run in CI: green
# build, zero coverage. See CLAUDE.md's Critical Rules and
# docs/testing/INTEGRATION_TESTS.md.
#
# This reports two different things:
#   - the standing backlog: every unregistered class in dotcms-integration
#   - the flow: classes ADDED in a recent window and whether they were
#     registered, which is what tells you if the failure mode is still live
#
# Usage:
#   scripts/validate-integration-test-registration.sh            # 30-day window
#   scripts/validate-integration-test-registration.sh 60         # 60-day window
#   scripts/validate-integration-test-registration.sh 14 --strict  # exit 1 on a leak in the window

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
IT_DIR="$PROJECT_ROOT/dotcms-integration"

WINDOW_DAYS="${1:-30}"
STRICT=0
[ "${2:-}" = "--strict" ] && STRICT=1

if [ ! -d "$IT_DIR" ]; then
    echo "❌ Error: Could not find $IT_DIR"
    exit 1
fi

cd "$PROJECT_ROOT"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Classes named in any @SuiteClasses array across the six suite files.
find "$IT_DIR" \( -name 'MainSuite*.java' -o -name 'Junit5Suite*.java' \) -print0 \
  | xargs -0 cat \
  | grep -oE '[A-Za-z0-9_]+\.class' | sed 's/\.class//' | sort -u > "$TMP/registered.txt"

# Concrete test classes. Abstract classes are excluded: they are base classes,
# never registered, and would otherwise dominate the count as false positives.
: > "$TMP/concrete.txt"
while IFS= read -r f; do
    grep -qE '^\s*(public\s+)?abstract\s+class' "$f" && continue
    basename "$f" .java >> "$TMP/concrete.txt"
done < <(find "$IT_DIR" -name '*Test.java')
sort -u -o "$TMP/concrete.txt" "$TMP/concrete.txt"

comm -13 "$TMP/registered.txt" "$TMP/concrete.txt" > "$TMP/unregistered.txt"

echo "🔍 Integration Test Registration"
echo "==============================="
echo ""
echo "Suites:              $(find "$IT_DIR" \( -name 'MainSuite*.java' -o -name 'Junit5Suite*.java' \) | wc -l | tr -d ' ')"
echo "Registered classes:  $(wc -l < "$TMP/registered.txt" | tr -d ' ')"
echo "Concrete *Test.java: $(wc -l < "$TMP/concrete.txt" | tr -d ' ')"
echo "Never run in CI:     $(wc -l < "$TMP/unregistered.txt" | tr -d ' ')"
echo ""

# The metric that matters for the monitoring gate: what landed recently.
echo "📈 Added in the last $WINDOW_DAYS days"
echo "-----------------------------------"
LEAKED=0
ADDED=0
while IFS= read -r f; do
    [ -z "$f" ] && continue
    c="$(basename "$f" .java)"
    grep -qx "$c" "$TMP/concrete.txt" || continue   # deleted or renamed since
    ADDED=$((ADDED + 1))
    if grep -qx "$c" "$TMP/registered.txt"; then
        echo "  ✅ $c"
    else
        echo "  ❌ $c  — not in any suite, never runs in CI"
        LEAKED=$((LEAKED + 1))
    fi
done < <(git log --since="$WINDOW_DAYS days ago" --diff-filter=A --name-only --format='' \
           -- 'dotcms-integration/**/*Test.java' | sort -u)

echo ""
if [ "$ADDED" -eq 0 ]; then
    echo "No integration tests added in this window."
else
    echo "Leak rate: $LEAKED of $ADDED new tests unregistered."
fi

# Known caveat: matching is by class name, not fully-qualified name. Two
# classes with the same simple name in different packages — one registered,
# one not — read as registered here. Good enough to trend; verify by hand
# before quoting a number.

if [ "$STRICT" -eq 1 ] && [ "$LEAKED" -gt 0 ]; then
    exit 1
fi
