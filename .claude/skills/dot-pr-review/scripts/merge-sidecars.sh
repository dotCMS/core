#!/usr/bin/env bash
# Usage: merge-sidecars.sh --dir <sidecar-dir> --out <dir> [--roster <file>]
#
# Merges the reviewers' JSONL sidecars (Stage 5) and reports the ones that came back empty (Stage 4b).
#   --dir <dir>      the run's sidecar directory; every *.jsonl in it is read
#   --out <dir>      where the merged files go:
#                      findings.jsonl     finding rows (file, line, severity, claim, evidence, fix,
#                                         confidence, agent). Exact duplicates are merged: rows with the
#                                         same file, line and claim keep the highest confidence and list
#                                         every agent in "agents". Sorted Critical, Important, Quality,
#                                         then by confidence, highest first.
#                      ac-coverage.jsonl  ac-validator rows, one per AC (the last row written wins)
#                      test-design.jsonl  test-case-designer rows, one per TC id (the last row wins)
#   --roster <file>  the sidecar file names launched in Stage 4, one per line. Each one that is absent
#                    or holds no valid row is reported, so a lost reviewer is never read as a clean one.
#
# Every row gets "sidecar": "<file name>". Lines that are not valid JSON, or JSON of no known shape,
# are counted as invalid and dropped.
#
# Prints one `SIDECAR <name> rows=<n> invalid=<n>` line per sidecar, `MISSING <name>` and
# `EMPTY <name>` for the roster, `OVERLAP <file>:<line> agents=<a,b>` where different claims share a
# line (Stage 5 judges whether they are one defect), and a final
# `TOTAL findings=<n> raw=<n> ac=<n> tc=<n> invalid=<n>` line.
# Exit codes: 0 merged (empty or missing sidecars are reported, not errors), 1 jq missing or
# unreadable input, 2 usage error.

set -euo pipefail

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0" >&2
  exit "${1:-2}"
}

DIR="" OUT="" ROSTER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dir) DIR="${2:-}"; shift 2 ;;
    --out) OUT="${2:-}"; shift 2 ;;
    --roster) ROSTER="${2:-}"; shift 2 ;;
    -h|--help) usage 0 ;;
    *) echo "ERROR: unknown argument $1" >&2; usage ;;
  esac
done
[[ -n "$DIR" && -n "$OUT" ]] || { echo "ERROR: --dir and --out are required" >&2; usage; }
[[ -d "$DIR" ]] || { echo "ERROR: $DIR is not a directory" >&2; exit 1; }
[[ -z "$ROSTER" || -r "$ROSTER" ]] || { echo "ERROR: cannot read $ROSTER" >&2; exit 1; }
command -v jq > /dev/null || { echo "ERROR: jq is required" >&2; exit 1; }

mkdir -p "$OUT"
ALL="$OUT/.all-rows.jsonl"
COUNTS="$OUT/.counts.tsv"
: > "$ALL"
: > "$COUNTS"
invalid_total=0

for f in "$DIR"/*.jsonl; do
  [[ -e "$f" ]] || continue
  name="$(basename "$f")"
  nonblank=$(grep -c '[^[:space:]]' "$f" || true)
  rows=$(jq -c -R --arg sc "$name" '
      fromjson?
      | select(type == "object")
      | select(has("ac") or has("tc") or (has("file") and has("line")))
      | . + {sidecar: $sc}' "$f" | tee -a "$ALL" | wc -l | tr -d ' ')
  invalid=$((nonblank - rows))
  invalid_total=$((invalid_total + invalid))
  printf '%s\t%s\n' "$name" "$rows" >> "$COUNTS"
  echo "SIDECAR $name rows=$rows invalid=$invalid"
done

if [[ -n "$ROSTER" ]]; then
  while IFS= read -r want || [[ -n "$want" ]]; do
    want="$(printf '%s' "$want" | sed -e 's/\r$//' -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    [[ -z "$want" || "$want" == \#* ]] && continue
    want="$(basename "$want")"
    got=$(awk -F'\t' -v n="$want" '$1 == n { print $2; found = 1 } END { if (!found) print "-" }' "$COUNTS")
    if [[ "$got" == "-" ]]; then echo "MISSING $want"
    elif [[ "$got" -eq 0 ]]; then echo "EMPTY $want"
    fi
  done < "$ROSTER"
fi

raw=$(jq -s '[.[] | select(has("file") and has("line"))] | length' "$ALL")

jq -s -c '
  [ .[] | select(has("file") and has("line")) ]
  | to_entries | map(.value + {_i: .key})
  | group_by([.file, (.line | tostring), (.claim // "")])
  | map(
      (max_by([((.confidence // 0) | tonumber? // 0), -._i]))
      + {agents: (map(.agent // .sidecar) | unique)}
    )
  | sort_by([
      ({"Critical": 0, "Important": 1, "Quality": 2}[.severity // ""] // 3),
      -((.confidence // 0) | tonumber? // 0),
      ._i
    ])
  | .[] | del(._i)
' "$ALL" > "$OUT/findings.jsonl"

# A test-design row also names its AC, so an AC-coverage row is one with "ac" and no "tc".
for kind in ac tc; do
  case "$kind" in ac) dest="ac-coverage.jsonl" ;; tc) dest="test-design.jsonl" ;; esac
  jq -s -c --arg k "$kind" '
    [ .[] | select(has($k) and ($k == "tc" or (has("tc") | not))) ]
    | to_entries | map(.value + {_i: .key})
    | group_by(.[$k] | tostring)
    | map(max_by(._i) | del(._i))
    | .[]
  ' "$ALL" > "$OUT/$dest"
done

jq -s -r '
  group_by([.file, (.line | tostring)])
  | map(select(length > 1))
  | .[]
  | "OVERLAP \(.[0].file):\(.[0].line) agents=\([.[].agents[]] | unique | join(","))"
' "$OUT/findings.jsonl"

findings=$(wc -l < "$OUT/findings.jsonl" | tr -d ' ')
ac=$(wc -l < "$OUT/ac-coverage.jsonl" | tr -d ' ')
tc=$(wc -l < "$OUT/test-design.jsonl" | tr -d ' ')
echo "TOTAL findings=$findings raw=$raw ac=$ac tc=$tc invalid=$invalid_total"
