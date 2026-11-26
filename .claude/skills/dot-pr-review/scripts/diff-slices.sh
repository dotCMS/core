#!/usr/bin/env bash
# Usage:
#   diff-slices.sh --out <dir> [--expect <paths-file>] [--strict] --diff <diff-file> <slice-list>...
#   diff-slices.sh --out <dir> [--expect <paths-file>] [--strict] --git <base> [--head <sha>] [--root <dir>] <slice-list>...
#
# Writes one diff slice per reviewer instance (Stage 4) and proves no changed file was dropped.
#   --diff <file>    slice an existing unified diff (the saved `gh pr diff` output). It is normalized
#                    first: a `diff --git` header glued onto the previous file's last line (a file with
#                    no trailing newline) is moved back to its own line.
#   --git <base>     build the diff from local git instead, one `git diff` per path, so no header can be
#                    glued: `git diff <base> <head> -- <path>` with --head (a PR, or a diff too large for
#                    `gh pr diff`), `git diff <base> -- <path>` without it (the working tree, local mode;
#                    an untracked path is diffed against /dev/null).
#   --head <sha>     the reviewed commit for --git (omit it to diff the working tree).
#   --root <dir>     the checkout to run git in (default: the current directory).
#   --expect <file>  every changed path, one per line (the reviewed file list). With --git, the full diff
#                    is built from it; without --expect, from the union of the slice lists.
#   --strict         an --expect path that no slice holds is an error, not a warning.
#   <slice-list>     one file per reviewer instance, named <name>.txt, one repo-relative path per line
#                    (blank lines and lines starting with # are ignored). Writes <out>/<name>.patch
#                    holding the blocks of exactly those paths.
#
# <out>/full.patch is the normalized full diff the slices are cut from.
#
# Assertions (each failure prints one line and makes the exit code 1):
#   COUNT    the full diff's `diff --git` count differs from the --expect path count
#   MISSING  a listed path (in --expect or a slice) has no block in the full diff
#   EXTRA    the full diff holds a block for a path not in --expect
#   DUP      the full diff holds two blocks for one path
#   SLICE    a slice's `diff --git` count differs from its list length
#   SUM      the `diff --git` count summed over all slices differs from the paths summed over all lists
# Warning (an error only with --strict): UNSLICED <path>, an --expect path in no slice.
#
# Prints `NORMALIZED <n>` when glued headers were fixed, one `SLICE <name>.patch files=<n> lines=<n>`
# line per slice, then `OK` or `FAILED <n>`.
# Exit codes: 0 every assertion holds, 1 an assertion failed or git failed, 2 usage error.

set -euo pipefail

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0" >&2
  exit "${1:-2}"
}

OUT="" EXPECT="" STRICT=0 DIFF="" BASE="" HEAD="" ROOT="."
LISTS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out) OUT="${2:-}"; shift 2 ;;
    --expect) EXPECT="${2:-}"; shift 2 ;;
    --strict) STRICT=1; shift ;;
    --diff) DIFF="${2:-}"; shift 2 ;;
    --git) BASE="${2:-}"; shift 2 ;;
    --head) HEAD="${2:-}"; shift 2 ;;
    --root) ROOT="${2:-}"; shift 2 ;;
    -h|--help) usage 0 ;;
    --*) echo "ERROR: unknown option $1" >&2; usage ;;
    *) LISTS+=("$1"); shift ;;
  esac
done

[[ -n "$OUT" ]] || { echo "ERROR: --out is required" >&2; usage; }
if [[ -n "$DIFF" && -n "$BASE" ]] || [[ -z "$DIFF" && -z "$BASE" ]]; then
  echo "ERROR: give exactly one of --diff and --git" >&2; usage
fi
[[ ${#LISTS[@]} -gt 0 ]] || { echo "ERROR: at least one <slice-list> is required" >&2; usage; }
[[ -z "$DIFF" || -r "$DIFF" ]] || { echo "ERROR: cannot read $DIFF" >&2; exit 1; }
[[ -z "$EXPECT" || -r "$EXPECT" ]] || { echo "ERROR: cannot read $EXPECT" >&2; exit 1; }

mkdir -p "$OUT/.work/lists"
WORK="$OUT/.work"
rm -f "$WORK"/lists/*.list  # a reused --out must not carry an earlier run's lists
FULL="$OUT/full.patch"
FAILS=0
fail() { echo "$1"; FAILS=$((FAILS + 1)); }

# A clean path list: no CR, no surrounding blanks, no blank or comment lines.
clean_list() { sed -e 's/\r$//' -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' "$1" | grep -v -e '^$' -e '^#' || true; }

for l in "${LISTS[@]}"; do
  [[ -r "$l" ]] || { echo "ERROR: cannot read slice list $l" >&2; exit 1; }
  name="$(basename "$l")"; name="${name%.txt}"
  clean_list "$l" > "$WORK/lists/$name.list"
done
if [[ -n "$EXPECT" ]]; then
  clean_list "$EXPECT" > "$WORK/expect.list"
else
  cat "$WORK"/lists/*.list | LC_ALL=C sort -u > "$WORK/expect.list"
fi

# 1. The full diff.
if [[ -n "$DIFF" ]]; then
  awk '
    function lastpos(s, t,   p, q, off) {
      off = 0; p = 0
      while ((q = index(substr(s, off + 1), t)) > 0) { p = off + q; off = p }
      return p
    }
    { line[NR] = $0 }
    END {
      fixed = 0
      for (i = 1; i <= NR; i++) {
        s = line[i]
        if (substr(s, 1, 11) != "diff --git " && i < NR &&
            line[i + 1] ~ /^(index |new file mode|deleted file mode|similarity index|dissimilarity index|old mode|new mode|--- |Binary files|rename from|copy from)/) {
          p = lastpos(s, "diff --git ")
          if (p > 1 && substr(s, p) ~ /^diff --git "?a\//) {
            print substr(s, 1, p - 1); print substr(s, p); fixed++; continue
          }
        }
        print s
      }
      if (fixed > 0) print "NORMALIZED " fixed > "/dev/stderr"
    }' "$DIFF" > "$FULL" 2> "$WORK/normalize.log"
  if [[ -s "$WORK/normalize.log" ]]; then cat "$WORK/normalize.log"; fi
else
  : > "$FULL"
  while IFS= read -r p; do
    rc=0
    if [[ -n "$HEAD" ]]; then
      git -C "$ROOT" diff --no-color "$BASE" "$HEAD" -- "$p" >> "$FULL" || rc=$?
    elif [[ -e "$ROOT/$p" ]] && ! git -C "$ROOT" ls-files --error-unmatch -- "$p" > /dev/null 2>&1; then
      # Untracked: exit code 1 means "differences found", the normal result here.
      git -C "$ROOT" diff --no-color --no-index -- /dev/null "$p" >> "$FULL" || rc=$?
      [[ $rc -le 1 ]] && rc=0
    else
      git -C "$ROOT" diff --no-color "$BASE" -- "$p" >> "$FULL" || rc=$?
    fi
    [[ $rc -eq 0 ]] || { echo "ERROR: git diff failed for $p (exit $rc)" >&2; exit 1; }
  done < "$WORK/expect.list"
fi

# 2. Index the blocks: start line, end line, path (the new path; the old one for a deletion).
awk '
  function lastpos(s, t,   p, q, off) {
    off = 0; p = 0
    while ((q = index(substr(s, off + 1), t)) > 0) { p = off + q; off = p }
    return p
  }
  function hdrpath(s,   r, n, half, a, b, k) {
    r = substr(s, 12); gsub(/"/, "", r)
    n = length(r)
    if ((n - 1) % 2 == 0) {
      half = (n - 1) / 2; a = substr(r, 1, half); b = substr(r, half + 2)
      if (substr(a, 3) == substr(b, 3)) return substr(b, 3)
    }
    k = lastpos(r, " b/")
    if (k > 0) return substr(r, k + 3)
    return r
  }
  /^diff --git / {
    if (start) printf "%d\t%d\t%s\n", start, NR - 1, path
    start = NR; path = hdrpath($0); inhdr = 1; next
  }
  inhdr && /^rename to / { path = substr($0, 11); next }
  inhdr && /^\+\+\+ / {
    p = substr($0, 5); gsub(/"/, "", p); sub(/\t$/, "", p)
    if (p != "/dev/null") { sub(/^b\//, "", p); path = p }
    inhdr = 0; next
  }
  inhdr && /^@@ / { inhdr = 0 }
  END { if (start) printf "%d\t%d\t%s\n", start, NR, path }
' "$FULL" > "$WORK/index.tsv"

cut -f3 "$WORK/index.tsv" | LC_ALL=C sort > "$WORK/diff-paths.sorted"
LC_ALL=C sort -u "$WORK/expect.list" > "$WORK/expect.sorted"

full_count=$(grep -c '^diff --git ' "$FULL" || true)
expect_count=$(wc -l < "$WORK/expect.sorted" | tr -d ' ')
if [[ -n "$EXPECT" && "$full_count" -ne "$expect_count" ]]; then
  fail "COUNT full diff has $full_count diff --git blocks, --expect lists $expect_count paths"
fi
while IFS= read -r p; do fail "DUP $p"; done < <(uniq -d "$WORK/diff-paths.sorted")
while IFS= read -r p; do fail "MISSING $p"; done < <(LC_ALL=C sort -u "$WORK/diff-paths.sorted" | LC_ALL=C comm -13 - "$WORK/expect.sorted")
if [[ -n "$EXPECT" ]]; then
  while IFS= read -r p; do fail "EXTRA $p"; done < <(LC_ALL=C sort -u "$WORK/diff-paths.sorted" | LC_ALL=C comm -23 - "$WORK/expect.sorted")
fi

# 3. One patch per slice list, with the blocks of exactly its paths.
sum_blocks=0
sum_paths=0
for l in "${LISTS[@]}"; do
  name="$(basename "$l")"; name="${name%.txt}"
  patch="$OUT/$name.patch"
  awk -F'\t' -v slice="$name" '
    FILENAME == ARGV[1] { s[$3] = $1; e[$3] = $2; next }
    FILENAME == ARGV[2] {
      if ($0 in s) { for (j = s[$0]; j <= e[$0]; j++) want[j] = 1 }
      else print "MISSING " slice " " $0 > "/dev/stderr"
      next
    }
    (FNR in want) { print }
  ' "$WORK/index.tsv" "$WORK/lists/$name.list" "$FULL" > "$patch" 2> "$WORK/$name.missing"
  while IFS= read -r m; do fail "$m"; done < "$WORK/$name.missing"
  blocks=$(grep -c '^diff --git ' "$patch" || true)
  paths=$(LC_ALL=C sort -u "$WORK/lists/$name.list" | wc -l | tr -d ' ')
  lines=$(wc -l < "$patch" | tr -d ' ')
  echo "SLICE $name.patch files=$blocks lines=$lines"
  [[ "$blocks" -eq "$paths" ]] || fail "SLICE $name.patch has $blocks diff --git blocks, its list has $paths paths"
  sum_blocks=$((sum_blocks + blocks))
  sum_paths=$((sum_paths + paths))
done
[[ "$sum_blocks" -eq "$sum_paths" ]] || fail "SUM slices hold $sum_blocks diff --git blocks, the lists name $sum_paths paths"

# 4. Paths no slice holds.
cat "$WORK"/lists/*.list | LC_ALL=C sort -u > "$WORK/sliced.sorted"
while IFS= read -r p; do
  if [[ "$STRICT" -eq 1 ]]; then fail "UNSLICED $p"; else echo "UNSLICED $p"; fi
done < <(LC_ALL=C comm -23 "$WORK/expect.sorted" "$WORK/sliced.sorted")

if [[ "$FAILS" -eq 0 ]]; then echo "OK"; exit 0; fi
echo "FAILED $FAILS"
exit 1
