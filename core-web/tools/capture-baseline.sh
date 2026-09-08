#!/usr/bin/env bash
# Capture per-project test-count baselines — FR-003, FR-003a, FR-003b.
#
# Runs every project's test target ONE AT A TIME and records its count before the
# migration deletes the configuration that produces it.
#
# TWO MEASURED DEFECTS IN THE EXISTING REPORTING THAT THIS WORKS AROUND
# (both pre-existing; the migration did not cause either — see research.md R-12):
#
#   1. The report path depends on how deeply a project is nested. jest-junit's
#      outputDirectory is '../../../target/core-web-reports' relative to each
#      project's rootDir, so `libs/utils` (depth 2) writes to <repo-root>/target/
#      while `libs/portlets/dot-usage` (depth 3) writes to core-web/target/.
#      Verified: utils' 111 tests landed outside core-web entirely.
#
#   2. Every project writes the same filename, so a parallel `nx run-many` leaves
#      only the last writer. Verified: utils (111) + portlets-dot-usage (21) run
#      together produced a report containing 21.
#
# Both are neutralised here by forcing a deterministic per-project path through
# JEST_JUNIT_OUTPUT_DIR/NAME, which override the config.
#
# THE CROSS-CHECK IS NOT OPTIONAL. An earlier version of this script trusted the
# report's presence and silently recorded 0 tests for `utils` — a project with 111.
# A missing report means "we do not know", never "no tests", so the count parsed
# from XML is verified against the runner's own "Tests: N total" line and any
# disagreement is a hard failure.
#
# Usage:  tools/capture-baseline.sh <out.json> [project ...]
# Safe to interrupt and re-run: already-captured projects are skipped.

set -uo pipefail

OUT="${1:?usage: capture-baseline.sh <out.json> [project ...]}"
shift || true

cd "$(dirname "$0")/.." || exit 2
CW="$(pwd)"
LOGDIR="${BASELINE_LOGDIR:-$CW/target/baseline-logs}"
RPTDIR="${BASELINE_RPTDIR:-$CW/target/baseline-reports}"
mkdir -p "$LOGDIR" "$RPTDIR"

if [ "$#" -gt 0 ]; then
    PROJECTS=("$@")
else
    while IFS= read -r line; do PROJECTS+=("$line"); done < <(
        pnpm exec nx show projects --with-target test --json 2>/dev/null \
        | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>JSON.parse(s).sort().forEach(p=>console.log(p)))'
    )
fi

echo "Capturing baselines for ${#PROJECTS[@]} project(s) → $OUT"
ok=0; empty=0; failed=0

for p in "${PROJECTS[@]}"; do
    if [ -f "$OUT" ] && node -e "process.exit(JSON.parse(require('fs').readFileSync('$OUT','utf8')).projects['$p']?0:1)" 2>/dev/null; then
        echo "  = $p (already captured)"
        continue
    fi

    RPT="$RPTDIR/$p.xml"
    LOG="$LOGDIR/$p.log"
    rm -f "$RPT" "$CW/target/core-web-reports/$p.xml"

    if ! JEST_JUNIT_OUTPUT_DIR="$RPTDIR" JEST_JUNIT_OUTPUT_NAME="$p.xml" \
         pnpm exec nx test "$p" --skip-nx-cache > "$LOG" 2>&1; then
        # A red suite before migration is itself a finding: the baseline is meant to
        # be a green starting point, so surface it rather than recording a number.
        echo "  ✗ $p — suite FAILED before migration (see $LOG)"
        failed=$((failed+1)); continue
    fi

    # The runner's own total, as a second source of truth.
    #
    # BOTH runner formats must be recognised, and this is not hypothetical: Jest
    # prints "Tests:       21 passed, 21 total" while Vitest prints
    # "Tests  14 passed (14)" — no colon. A Jest-only pattern silently reported
    # sdk-vue (14 Vitest tests) as "genuinely empty". Harmless there, since it is
    # already migrated and out of scope — but AFTER the migration every project
    # emits the Vitest format, so a Jest-only pattern would report the entire
    # workspace as empty and call it parity. Same false-zero class as the utils bug.
    # ANSI codes stripped FIRST. Vitest colours the count, so the raw line reads
    # "Tests  <ESC>[1m<ESC>[32m55 passed" and a pattern expecting digits right after
    # "Tests " matches nothing. That turned the cross-check into the exact false zero
    # it exists to prevent: sdk-vue ran 55 tests and was recorded as "genuinely empty",
    # silently, because no report was found AND the runner appeared to report nothing.
    REPORTED=$(sed -E $'s/\x1b\[[0-9;]*m//g' "$LOG" | grep -aoE 'Tests[:[:space:]]+[0-9]+ (passed|total)' | grep -aoE '[0-9]+' | tail -1)
    REPORTED="${REPORTED:-}"

    # Migrated projects write their JUnit report where their vite config says, not
    # where JEST_JUNIT_OUTPUT_DIR points — Vitest does not read that env var. Look in
    # both places so the same script works on either side of the migration, which is
    # the whole point of comparing them.
    VITEST_RPT="$CW/target/core-web-reports/$p.xml"
    if [ ! -f "$RPT" ] && [ -f "$VITEST_RPT" ]; then
        RPT="$VITEST_RPT"
    fi

    if [ ! -f "$RPT" ]; then
        if [ -z "$REPORTED" ] || [ "$REPORTED" = "0" ]; then
            echo "  ○ $p — no report and the runner reported no tests: genuinely empty"
            node -e "
              const fs=require('fs');const f='$OUT';
              const b=fs.existsSync(f)?JSON.parse(fs.readFileSync(f,'utf8')):{capturedAt:new Date().toISOString(),nodeVersion:process.version,projects:{}};
              b.projects['$p']={tests:0,skipped:0,failures:0,errors:0,suites:0,note:'no specs'};
              fs.writeFileSync(f,JSON.stringify(b,null,2)+'\n');"
            empty=$((empty+1))
        else
            echo "  ✗ $p — runner reported $REPORTED tests but NO report was written."
            echo "      Recording 0 here would be a false baseline. Investigate before migrating this project."
            failed=$((failed+1))
        fi
        continue
    fi

    if ! node tools/compare-test-counts.mjs --capture --project "$p" --report "$RPT" --out "$OUT" > "$LOG.count" 2>&1; then
        echo "  ✗ $p — could not parse its report ($RPT)"; failed=$((failed+1)); continue
    fi
    cat "$LOG.count"

    PARSED=$(node -e "console.log(JSON.parse(require('fs').readFileSync('$OUT','utf8')).projects['$p'].tests)")
    if [ -n "$REPORTED" ] && [ "$PARSED" != "$REPORTED" ]; then
        echo "  ✗ $p — CROSS-CHECK FAILED: report says $PARSED, runner said $REPORTED."
        echo "      The baseline for this project is not trustworthy."
        failed=$((failed+1)); continue
    fi
    ok=$((ok+1))
done

echo
echo "captured: $ok · genuinely empty: $empty · failed: $failed"
[ "$failed" -eq 0 ] || { echo "WARNING: $failed project(s) have no usable baseline. Do not migrate those until resolved."; exit 1; }
