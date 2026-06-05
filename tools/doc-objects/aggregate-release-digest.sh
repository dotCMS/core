#!/usr/bin/env bash
# aggregate-release-digest.sh — assemble a release-notes input bundle
# by walking commits in a range and pulling their doc-object notes.
#
# Usage: aggregate-release-digest.sh <FROM>..<TO> [--audience customer|internal|all]
#
# The read side of the doc-object pattern:
#   commits → git notes (refs/notes/doc-objects) → filtered bundle
# An LLM then synthesises this bundle into a changelog or doc-update input.
# The expensive diff analysis already happened at merge time — no re-analysis needed.
#
# Prerequisites:
#   git fetch origin refs/notes/doc-objects:refs/notes/doc-objects
#
# Examples:
#   ./aggregate-release-digest.sh HEAD~20..HEAD --audience customer
#   ./aggregate-release-digest.sh v25.04..v25.05 --audience all
#   ./aggregate-release-digest.sh v25.04..v25.05 --audience internal

set -euo pipefail

RANGE="${1:-HEAD~10..HEAD}"
AUDIENCE_FILTER="customer"

if [[ "${2:-}" == "--audience" ]]; then
  AUDIENCE_FILTER="${3:-customer}"
fi

NOTES_REF="refs/notes/doc-objects"

printf '# Release digest input\n\n'
printf 'Range: %s\n' "$RANGE"
printf 'Audience filter: %s\n\n' "$AUDIENCE_FILTER"
printf '> Bundle below is the LLM input for release-notes synthesis.\n'
printf '> Each entry is a pre-computed doc-object; no diff re-analysis needed.\n\n'
printf -- '---\n\n'

INCLUDED=0
SKIPPED=0
MISSING=0

for sha in $(git rev-list --reverse "$RANGE"); do
  short=$(git rev-parse --short "$sha")
  subject=$(git log -1 --format='%s' "$sha")

  if ! git notes --ref="$NOTES_REF" show "$sha" >/dev/null 2>&1; then
    MISSING=$((MISSING + 1))
    printf '<!-- %s %s — no doc-object attached, SKIPPED -->\n\n' "$short" "$subject"
    continue
  fi

  # Pull the doc object and its audience field from the frontmatter
  note=$(git notes --ref="$NOTES_REF" show "$sha")
  audience=$(printf '%s\n' "$note" | awk '
    /^---$/ { in_fm = !in_fm; next }
    in_fm && /audience:/ { gsub(/[ "]/, "", $2); print $2; exit }
  ')

  if [[ "$AUDIENCE_FILTER" != "all" && "$audience" != "$AUDIENCE_FILTER" && "$audience" != "both" ]]; then
    SKIPPED=$((SKIPPED + 1))
    printf '<!-- %s %s — audience=%s, filtered out -->\n\n' "$short" "$subject" "$audience"
    continue
  fi

  INCLUDED=$((INCLUDED + 1))
  printf '## %s — %s\n\n' "$short" "$subject"
  printf '%s\n\n' "$note"
  printf -- '---\n\n'
done

printf '<!-- Bundle summary: included=%d  filtered=%d  missing=%d -->\n' \
  "$INCLUDED" "$SKIPPED" "$MISSING"

