#!/usr/bin/env bash
# Usage: verify-anchors.sh --findings <file.jsonl> --base <sha> (--rev <sha> | --worktree) [--root <dir>] [--out <file>]
#
# Checks every finding's anchor against the reviewed revision: in Stage 5, before the report, and again
# in Stage 7b, before anything is posted.
#   --findings <file>  finding rows, one JSON object per line, each with "file", "line" and "evidence"
#                      (the findings.jsonl written by merge-sidecars.sh); a row that already carries
#                      an "anchor" (from an earlier run of this script) is checked at its anchor.line
#   --base <sha>       the base of the reviewed diff (the PR's base sha, or local mode's BASE_SHA)
#   --rev <sha>        PR mode: read each file at this commit (`git show <sha>:<path>`) and take the
#                      changed ranges from `git diff <base> <sha>`
#   --worktree         local mode: read each file from the working tree and take the changed ranges from
#                      `git diff <base>`; every line of an untracked file is changed
#   --root <dir>       the checkout to run git in and read files from (default: the current directory)
#   --out <file>       where to write the annotated rows (default: stdout)
#
# Each row is written back with an "anchor" object:
#   {"status": S, "line": <line to use>, "cited": <line the finding gave>, "file_lines": <n>,
#    "past_eof": <bool>, "in_diff": <bool>}
# S is one of:
#   OK            the evidence is on the cited line
#   REANCHORED    the evidence is on exactly one other line, or on exactly one changed line among
#                 several; "line" is that line
#   AMBIGUOUS     the evidence is on several lines and not on exactly one changed line
#   STALE         the evidence is nowhere in the file
#   UNVERIFIABLE  the finding carries no evidence
#   NO_FILE       the file does not exist in the reviewed revision
# Evidence is matched on its first non-blank line, whitespace-normalized, as a substring of the file
# line. A leading + or - copied from the diff is retried without it when the literal matches nothing.
# "in_diff" is true when the final line is inside the changed (+) ranges of the diff: false marks a
# finding on a line this change did not touch (PRE-EXISTING).
#
# Prints `PAST_EOF <file>:<line> (<n> lines)` for every cited line past the end of its file (a diff
# offset cited as a file line), then one summary line, to stderr:
#   ANCHORS total=<n> ok=<n> reanchored=<n> ambiguous=<n> stale=<n> unverifiable=<n> no_file=<n> past_eof=<n> outside_diff=<n>
# Exit codes: 0 checked (the statuses carry the verdicts), 1 jq or git missing or unreadable input,
# 2 usage error.

set -euo pipefail

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0" >&2
  exit "${1:-2}"
}

FINDINGS="" BASE="" REV="" WORKTREE=0 ROOT="." OUTFILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --findings) FINDINGS="${2:-}"; shift 2 ;;
    --base) BASE="${2:-}"; shift 2 ;;
    --rev) REV="${2:-}"; shift 2 ;;
    --worktree) WORKTREE=1; shift ;;
    --root) ROOT="${2:-}"; shift 2 ;;
    --out) OUTFILE="${2:-}"; shift 2 ;;
    -h|--help) usage 0 ;;
    *) echo "ERROR: unknown argument $1" >&2; usage ;;
  esac
done
[[ -n "$FINDINGS" && -n "$BASE" ]] || { echo "ERROR: --findings and --base are required" >&2; usage; }
if [[ -n "$REV" && "$WORKTREE" -eq 1 ]] || [[ -z "$REV" && "$WORKTREE" -eq 0 ]]; then
  echo "ERROR: give exactly one of --rev and --worktree" >&2; usage
fi
[[ -r "$FINDINGS" ]] || { echo "ERROR: cannot read $FINDINGS" >&2; exit 1; }
command -v jq > /dev/null || { echo "ERROR: jq is required" >&2; exit 1; }
git -C "$ROOT" rev-parse --verify --quiet "$BASE^{commit}" > /dev/null || { echo "ERROR: base $BASE is not a commit in $ROOT" >&2; exit 1; }
[[ -z "$REV" ]] || git -C "$ROOT" rev-parse --verify --quiet "$REV^{commit}" > /dev/null || { echo "ERROR: rev $REV is not a commit in $ROOT" >&2; exit 1; }

TMP="$(mktemp "${TMPDIR:-/tmp}/verify-anchors.XXXXXX")"
RANGES="$(mktemp "${TMPDIR:-/tmp}/verify-anchors-ranges.XXXXXX")"
trap 'rm -f "$TMP" "$RANGES"' EXIT

if [[ -n "$OUTFILE" ]]; then : > "$OUTFILE"; fi
emit() { if [[ -n "$OUTFILE" ]]; then printf '%s\n' "$1" >> "$OUTFILE"; else printf '%s\n' "$1"; fi; }

total=0 ok=0 reanchored=0 ambiguous=0 stale=0 unverifiable=0 no_file=0 past_eof=0 outside=0

while IFS= read -r row || [[ -n "$row" ]]; do
  [[ -n "${row//[[:space:]]/}" ]] || continue
  total=$((total + 1))
  file=$(jq -r '.file // ""' <<< "$row")
  # A row anchored by an earlier run (Stage 5) is checked at its latest anchor.
  cited=$(jq -r '(.anchor.line // .line // 0) | tonumber? // 0' <<< "$row")
  ev=$(jq -r '(.evidence // "") | tostring | split("\n") | map(select(test("\\S"))) | (.[0] // "")' <<< "$row")

  # The file at the reviewed revision, and whether it is untracked (local mode).
  have_file=1 untracked=0
  if [[ -n "$REV" ]]; then
    git -C "$ROOT" show "$REV:$file" > "$TMP" 2> /dev/null || have_file=0
  elif [[ -f "$ROOT/$file" ]]; then
    cat "$ROOT/$file" > "$TMP"
    git -C "$ROOT" ls-files --error-unmatch -- "$file" > /dev/null 2>&1 || untracked=1
  else
    have_file=0
  fi

  if [[ "$have_file" -eq 0 ]]; then
    status="NO_FILE"; line="$cited"; nlines=0; in_diff=false
    no_file=$((no_file + 1))
  else
    # Changed (+) ranges as "start count" pairs; an untracked file is changed throughout.
    if [[ "$untracked" -eq 1 ]]; then
      echo "1 999999999" > "$RANGES"
    elif [[ -n "$REV" ]]; then
      git -C "$ROOT" diff --no-color --unified=0 "$BASE" "$REV" -- "$file" \
        | awk '/^@@ / { split($3, a, ","); s = substr(a[1], 2); c = (2 in a) ? a[2] : 1; print s, c }' > "$RANGES"
    else
      git -C "$ROOT" diff --no-color --unified=0 "$BASE" -- "$file" \
        | awk '/^@@ / { split($3, a, ","); s = substr(a[1], 2); c = (2 in a) ? a[2] : 1; print s, c }' > "$RANGES"
    fi

    ev2=""
    case "$ev" in [+-]*) ev2="${ev:1}" ;; esac

    # One pass over the file: its length, whether the cited line holds the evidence, every line that does.
    read -r nlines cited_hit hits <<< "$(EV="$ev" EV2="$ev2" CITED="$cited" awk '
      function norm(s) { gsub(/[ \t\r]+/, " ", s); sub(/^ /, "", s); sub(/ $/, "", s); return s }
      BEGIN { e1 = norm(ENVIRON["EV"]); e2 = norm(ENVIRON["EV2"]); cited = ENVIRON["CITED"] + 0 }
      {
        l = norm($0)
        if (e1 != "" && index(l, e1) > 0) { m1 = m1 (m1 == "" ? "" : ",") NR; if (NR == cited) c1 = 1 }
        if (e2 != "" && index(l, e2) > 0) { m2 = m2 (m2 == "" ? "" : ",") NR; if (NR == cited) c2 = 1 }
      }
      END {
        if (m1 == "" && m2 != "") { m1 = m2; c1 = c2 }
        printf "%d %d %s\n", NR, c1 + 0, (m1 == "" ? "-" : m1)
      }' "$TMP")"

    if [[ -z "$ev" ]]; then
      status="UNVERIFIABLE"; line="$cited"; unverifiable=$((unverifiable + 1))
    elif [[ "$cited_hit" -eq 1 ]]; then
      status="OK"; line="$cited"; ok=$((ok + 1))
    elif [[ "$hits" == "-" ]]; then
      status="STALE"; line="$cited"; stale=$((stale + 1))
    else
      # Prefer the single hit inside the changed ranges when there are several.
      pick=$(awk -v hits="$hits" '
        { s[NR] = $1; c[NR] = $2 }
        END {
          n = split(hits, h, ",")
          if (n == 1) { print h[1]; exit }
          k = 0
          for (i = 1; i <= n; i++) for (j = 1; j <= NR; j++)
            if (c[j] > 0 && h[i] >= s[j] && h[i] < s[j] + c[j]) { k++; pick = h[i]; break }
          print (k == 1 ? pick : "-")
        }' "$RANGES")
      if [[ "$pick" == "-" ]]; then
        status="AMBIGUOUS"; line="$cited"; ambiguous=$((ambiguous + 1))
      else
        status="REANCHORED"; line="$pick"; reanchored=$((reanchored + 1))
      fi
    fi

    in_diff=$(awk -v ln="$line" '$2 > 0 && ln >= $1 && ln < $1 + $2 { hit = 1 } END { print (hit ? "true" : "false") }' "$RANGES")
    [[ "$in_diff" == "true" ]] || outside=$((outside + 1))
  fi

  pe=false
  if [[ "$have_file" -eq 1 && "$cited" -gt "$nlines" ]]; then
    pe=true; past_eof=$((past_eof + 1))
    echo "PAST_EOF $file:$cited ($nlines lines)" >&2
  fi

  emit "$(jq -c --arg st "$status" --argjson ln "$line" --argjson ci "$cited" --argjson nl "$nlines" \
      --argjson pe "$pe" --argjson id "$in_diff" \
      '. + {anchor: {status: $st, line: $ln, cited: $ci, file_lines: $nl, past_eof: $pe, in_diff: $id}}' <<< "$row")"
done < "$FINDINGS"

echo "ANCHORS total=$total ok=$ok reanchored=$reanchored ambiguous=$ambiguous stale=$stale unverifiable=$unverifiable no_file=$no_file past_eof=$past_eof outside_diff=$outside" >&2
