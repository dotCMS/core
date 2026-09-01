#!/usr/bin/env bash
#
# adr-context.sh — Read-only lookup of relevant Architecture Decision Records.
#
# ADRs live in the private repo dotCMS/platform-adrs (never in dotCMS/core).
# This helper fetches that repo's INDEX.md via the authenticated `gh` CLI and
# surfaces ADRs whose title / category / tags match the supplied keywords, so
# the /speckit-plan phase can treat existing decisions as binding input.
#
# It is strictly READ-ONLY (HTTP GET only). It NEVER creates, edits, or commits
# an ADR — ADRs are authored solely in dotCMS/platform-adrs via its new-adr.sh.
#
# Usage:
#   adr-context.sh [--all] [--limit N] <keyword> [keyword ...]
#   adr-context.sh --json  <keyword> ...      # machine-readable matches
#
# Exit code is always 0 on a successful lookup, even with zero matches, so it
# never blocks planning. Non-zero only on a real error (gh missing/unauthenticated).

set -euo pipefail

ADR_REPO="dotCMS/platform-adrs"
ADR_INDEX_PATH="INDEX.md"
ADR_WEB_BASE="https://github.com/${ADR_REPO}/blob/main"

SHOW_ALL=false
JSON=false
LIMIT=0
KEYWORDS=()

while [ $# -gt 0 ]; do
  case "$1" in
    --all) SHOW_ALL=true ;;
    --json) JSON=true ;;
    --limit) shift; LIMIT="${1:-0}" ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) KEYWORDS+=("$1") ;;
  esac
  shift
done

if ! command -v gh >/dev/null 2>&1; then
  echo "ERROR: gh CLI not found. ADR lookup requires an authenticated GitHub CLI." >&2
  echo "       Install gh and run 'gh auth login', then retry." >&2
  exit 2
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: gh is not authenticated. Run 'gh auth login' (needs access to ${ADR_REPO})." >&2
  exit 2
fi

# Fetch the ADR index (base64-decoded contents API response). GET only.
INDEX_MD="$(gh api "repos/${ADR_REPO}/contents/${ADR_INDEX_PATH}" -q .content 2>/dev/null | base64 -d 2>/dev/null || true)"

if [ -z "$INDEX_MD" ]; then
  echo "WARNING: Could not read ${ADR_REPO}/${ADR_INDEX_PATH} (network / permissions?)." >&2
  echo "         Proceed with planning, but manually confirm no relevant ADR is missed:" >&2
  echo "         ${ADR_WEB_BASE}/${ADR_INDEX_PATH}" >&2
  exit 0
fi

# The index lists one ADR per markdown line, e.g.:
#   - 🔄 [ADR-0018: Database-First Search ...](decisions/0018-...md) `content-drive` `search` ...
#   | [0018](decisions/0018-...md) | Database-First Search ... | backend | proposed | 2026-06-30 |
# Keep only lines that reference a decisions/ file — those are ADR entries.
ADR_LINES="$(printf '%s\n' "$INDEX_MD" | grep -E 'decisions/[0-9]+' || true)"

emit_all() {
  echo "# All ADRs in ${ADR_REPO} (source: ${ADR_WEB_BASE}/${ADR_INDEX_PATH})"
  echo
  printf '%s\n' "$ADR_LINES" | sed 's/^[[:space:]]*//'
}

if [ "$SHOW_ALL" = true ] || [ "${#KEYWORDS[@]}" -eq 0 ]; then
  emit_all
  exit 0
fi

# Build a case-insensitive alternation of keywords for matching.
PATTERN="$(printf '%s\n' "${KEYWORDS[@]}" | paste -sd'|' -)"

MATCHES="$(printf '%s\n' "$ADR_LINES" | grep -iE "$PATTERN" || true)"

if [ "$LIMIT" -gt 0 ] && [ -n "$MATCHES" ]; then
  MATCHES="$(printf '%s\n' "$MATCHES" | head -n "$LIMIT")"
fi

if [ "$JSON" = true ]; then
  # Emit minimal JSON: keywords + raw matching index lines.
  printf '{"repo":"%s","keywords":[' "$ADR_REPO"
  first=true
  for k in "${KEYWORDS[@]}"; do
    [ "$first" = true ] && first=false || printf ','
    printf '"%s"' "$(printf '%s' "$k" | sed 's/"/\\"/g')"
  done
  printf '],"matches":['
  first=true
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    [ "$first" = true ] && first=false || printf ','
    printf '"%s"' "$(printf '%s' "$line" | sed 's/\\/\\\\/g; s/"/\\"/g; s/^[[:space:]]*//')"
  done <<< "$MATCHES"
  printf ']}\n'
  exit 0
fi

echo "# ADR lookup in ${ADR_REPO}"
echo "# Keywords: ${KEYWORDS[*]}"
echo "# Full index: ${ADR_WEB_BASE}/${ADR_INDEX_PATH}"
echo
if [ -z "$MATCHES" ]; then
  echo "No ADRs matched those keywords."
  echo "Confirm none apply by skimming the full index above before finalizing the plan."
  echo "If a new architectural decision is warranted, PROPOSE it in the plan's"
  echo "'Proposed ADRs' section — do NOT create the ADR here (use ${ADR_REPO}'s new-adr.sh)."
  exit 0
fi

echo "Potentially relevant ADRs (treat ACCEPTED ones as binding input):"
echo
printf '%s\n' "$MATCHES" | sed 's/^[[:space:]]*//'
echo
echo "Read a specific ADR: gh api repos/${ADR_REPO}/contents/decisions/<file>.md -q .content | base64 -d"
echo "Reminder: Spec-Kit may PROPOSE new ADRs but must NEVER create them here."
