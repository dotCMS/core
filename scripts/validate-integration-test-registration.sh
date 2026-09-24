#!/bin/bash

# Integration Test Registration Validation Script
#
# CI runs integration tests only through the @SuiteClasses aggregator suites.
# A test class that is not listed in one of them compiles fine and is silently
# never run in CI: green build, zero coverage. See CLAUDE.md's Critical Rules
# and docs/testing/INTEGRATION_TESTS.md.
#
# The suite list is NOT hardcoded here. It is read from .github/test-matrix.yml,
# which is what CI actually executes — so a new MainSuite3b is picked up the day
# it is added to the matrix, and a suite that exists in the source tree but is
# not wired into CI (QuickSuite) correctly does not count as registered.
#
# Reports two different things:
#   - the standing backlog: every unregistered class in dotcms-integration
#   - the flow: classes ADDED in a recent window and whether they were
#     registered, which is what tells you whether the failure mode is still live
#
# Usage:
#   scripts/validate-integration-test-registration.sh              # 30-day window
#   scripts/validate-integration-test-registration.sh 60           # 60-day window
#   scripts/validate-integration-test-registration.sh 14 --strict  # exit 1 on a leak in the window

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
IT_DIR="$PROJECT_ROOT/dotcms-integration"
MATRIX="$PROJECT_ROOT/.github/test-matrix.yml"

WINDOW_DAYS="${1:-30}"
STRICT=0
[ "${2:-}" = "--strict" ] && STRICT=1

[ -d "$IT_DIR" ] || { echo "❌ Could not find $IT_DIR"; exit 1; }
[ -f "$MATRIX" ] || { echo "❌ Could not find $MATRIX"; exit 1; }

cd "$PROJECT_ROOT"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# Suites CI runs, per the matrix. Entries with no matching source file in
# dotcms-integration are skipped (e.g. KarateCITests, a different module).
: > "$TMP/suitefiles.txt"
while IFS= read -r name; do
    sf="$(find "$IT_DIR" -name "${name}.java" | head -1)"
    [ -n "$sf" ] && echo "$sf" >> "$TMP/suitefiles.txt"
done < <(grep -oE 'test_class:[[:space:]]*"[^"#]+' "$MATRIX" | sed 's/.*"//')

[ -s "$TMP/suitefiles.txt" ] || { echo "❌ No suite sources resolved from $MATRIX"; exit 1; }

# Classes inside the @SuiteClasses({ ... }) block only — not every X.class token
# in the file, which would also pick up @RunWith(MainBaseSuite.class) and friends.
# Three annotation forms occur in this repo: @SuiteClasses (JUnit 4),
# @Suite.SuiteClasses (MainSuite3a) and @SelectClasses (Junit5Suite1, JUnit 5).
# Lines starting with * are skipped so a javadoc mention of an annotation name
# does not open the block early — Junit5Suite1 has exactly that.
: > "$TMP/registered.txt"
while IFS= read -r sf; do
    n_before=$(wc -l < "$TMP/registered.txt")
    awk '/^[[:space:]]*\*/{next} /@((Suite\.)?SuiteClasses|SelectClasses)[[:space:]]*\(/{inblock=1} inblock{print} inblock && /\}\)/{inblock=0}' "$sf" \
      | grep -oE '[A-Za-z0-9_]+\.class' | sed 's/\.class//' >> "$TMP/registered.txt"
    n_after=$(wc -l < "$TMP/registered.txt")
    # A CI suite that contributes nothing means the extraction missed its
    # annotation form. Fail loudly rather than silently reporting its tests as
    # unregistered — that is exactly how this script got its numbers wrong once.
    if [ "$n_before" -eq "$n_after" ]; then
        echo "❌ Extracted 0 classes from $(basename "$sf") — unrecognized @SuiteClasses form?"
        exit 1
    fi
done < "$TMP/suitefiles.txt"
sort -u -o "$TMP/registered.txt" "$TMP/registered.txt"

# Concrete test classes. Abstract classes are base classes, never registered,
# and would otherwise dominate the count as false positives.
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
echo "CI suites (from .github/test-matrix.yml):"
while IFS= read -r sf; do echo "  - $(basename "$sf" .java)"; done < "$TMP/suitefiles.txt"
echo ""
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
    grep -qx "$c" "$TMP/concrete.txt" || continue   # deleted, renamed, or abstract since
    ADDED=$((ADDED + 1))
    if grep -qx "$c" "$TMP/registered.txt"; then
        echo "  ✅ $c"
    else
        echo "  ❌ $c  — not in any CI suite, never runs in CI"
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

# Known caveat: matching is by simple class name, not fully-qualified name. Two
# classes with the same simple name in different packages — one registered, one
# not — read as registered here. Good enough to trend; verify by hand before
# quoting a number.

if [ "$STRICT" -eq 1 ] && [ "$LEAKED" -gt 0 ]; then
    exit 1
fi
