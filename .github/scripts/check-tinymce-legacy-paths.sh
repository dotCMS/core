#!/usr/bin/env bash
#
# Fails if a legacy TinyMCE path reference remains anywhere in the repo, outside the
# intentional backward-compat alias serving point. See:
#   specs/upgrade-tinymce-legacy-version/ (local planning material, not committed) — AC-008,
#   contracts/backward-compat-alias.md
#
# The deleted 4.9.x vendored copy: /html/js/tinymce
# The deprecated alias (expected to still appear, in the alias-serving code/config only):
#   /ext/tinymcev7
#
# Usage: .github/scripts/check-tinymce-legacy-paths.sh [--allow FILE ...]
#   --allow FILE   A repo-relative path allowed to reference /ext/tinymcev7 (the alias
#                  implementation itself). Repeatable. /html/js/tinymce is never allowed.

set -euo pipefail

REPO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

ALLOWED_ALIAS_FILES=()
while [ $# -gt 0 ]; do
    case "$1" in
        --allow)
            shift
            [ $# -gt 0 ] || { echo "Error: --allow requires a value" >&2; exit 2; }
            ALLOWED_ALIAS_FILES+=("$1")
            ;;
        *)
            echo "Error: unknown argument '$1'" >&2
            exit 2
            ;;
    esac
    shift
done

# Use the real `grep` binary explicitly rather than whatever `grep` resolves to on PATH — some
# shells/environments alias it to a different tool (e.g. ugrep-based wrappers) whose exit-code
# behavior under `set -e` + command substitution has been observed to be unreliable. Output is
# also captured to a temp file and checked with `-s` rather than branching on a command
# substitution's exit status, so this is robust regardless of which grep actually runs.
GREP=$(command -v grep)
EXCLUDE_DIRS=(--exclude-dir=node_modules --exclude-dir=target --exclude-dir=dist --exclude-dir=.git --exclude-dir=coverage --exclude-dir=.codegraph --exclude-dir=.angular --exclude-dir=.next)

TMP_HTML_HITS="$(mktemp)"
TMP_ALIAS_HITS="$(mktemp)"
trap 'rm -f "$TMP_HTML_HITS" "$TMP_ALIAS_HITS"' EXIT

fail=0

echo "Checking for references to the deleted /html/js/tinymce path..."
"$GREP" -rn "${EXCLUDE_DIRS[@]}" -- "/html/js/tinymce" . > "$TMP_HTML_HITS" 2>/dev/null || true
if [ -s "$TMP_HTML_HITS" ]; then
    echo "FAIL: found references to the deleted /html/js/tinymce path:"
    cat "$TMP_HTML_HITS"
    fail=1
else
    echo "OK: no references found."
fi

echo
echo "Checking for unexpected references to the /ext/tinymcev7 alias path..."
"$GREP" -rln "${EXCLUDE_DIRS[@]}" -- "/ext/tinymcev7" . > "$TMP_ALIAS_HITS" 2>/dev/null || true
if [ -s "$TMP_ALIAS_HITS" ]; then
    unexpected=""
    while IFS= read -r file; do
        [ -n "$file" ] || continue
        rel="${file#./}"
        allowed=0
        for a in "${ALLOWED_ALIAS_FILES[@]}"; do
            [ "$rel" = "$a" ] && allowed=1 && break
        done
        [ "$allowed" -eq 0 ] && unexpected="${unexpected}${rel}\n"
    done < "$TMP_ALIAS_HITS"
    if [ -n "$unexpected" ]; then
        echo "FAIL: found /ext/tinymcev7 references outside the allowed alias files:"
        printf '%b' "$unexpected"
        echo "If one of these is the alias implementation itself, pass it via --allow."
        fail=1
    else
        echo "OK: all /ext/tinymcev7 references are in allowed alias files."
    fi
else
    echo "OK: no references found (the alias implementation should normally reference this path — confirm that is expected)."
fi

exit "$fail"
