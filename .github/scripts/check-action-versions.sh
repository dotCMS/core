#!/usr/bin/env bash
#
# Action-version drift guard (#36850).
#
# GitHub retired the Node 20 action runtime. This script is the standing check that
# every action under .github/ stays on a major whose runs.using is node24, so the
# 124-reference sweep done for #36850 cannot silently rot back.
#
# It exists because .github/filters.yaml routes only cicd_comp_*.yml, cicd_1-pr.yml
# and core-cicd/**/action.yml to any build — roughly 38 of the 51 files that sweep
# touched get no build, no test and no lint on a PR.
#
#   Usage:  .github/scripts/check-action-versions.sh [--include-docs] [--format text|github] [PATH ...]
#   Exit:   0 = clean · 1 = violations · 2 = usage error or malformed manifest
#
# Contract and rationale: specs/36850-upgrade-github-actions-node-24/contracts/check-action-versions.md
# Manifest data:          specs/36850-upgrade-github-actions-node-24/data-model.md
#
# Three assertions, each catching a different class of mistake:
#   A1 floor      — no reference below its target major; non-numeric tags always fail.
#                   It is a FLOOR, so a newer upstream major passes and never breaks CI.
#   A2 SHA pin    — a SHA pin must be the manifest SHA AND carry a matching "# vX.Y.Z"
#                   comment. This is the likeliest error across 124 edits and the one
#                   review misses; main already carried two such mismatches.
#   A3 explicit   — listed inputs must be written out, not inherited from a default.
#                   Keeps download-artifact's digest-mismatch from moving under us again.
#
# Actions absent from the manifest are deliberately NOT policed — the node16 first-party
# actions and the cold-path third-party ones are tracked in #37194. Adding them here is
# the natural first commit of that follow-up: the guard goes red, then the sweep greens it.

set -uo pipefail

usage() {
  cat >&2 <<'EOU'
usage: check-action-versions.sh [--include-docs] [--format text|github] [PATH ...]

  --include-docs        also check README.md usage examples and prose references
  --format text|github  text report (default) or ::error annotations for Actions
  PATH ...              roots to scan (default: .github)

exit codes: 0 clean · 1 violations · 2 usage error or malformed manifest
EOU
}

# ---------------------------------------------------------------------------
# Manifest: action <TAB> min_major <TAB> pinned_version <TAB> pinned_sha <TAB> required_inputs
#
# required_inputs is comma-separated and may be empty. Sub-path actions
# (actions/cache/restore) are separate records from their parent (actions/cache)
# because their input contracts differ; the longest match wins at scan time.
#
# Override with CHECK_ACTION_VERSIONS_MANIFEST=<file> (used by the test suite).
# ---------------------------------------------------------------------------
default_manifest() {
  cat <<'EOM'
actions/checkout	7	v7.0.1	3d3c42e5aac5ba805825da76410c181273ba90b1	
actions/cache/restore	6	v6.1.0	55cc8345863c7cc4c66a329aec7e433d2d1c52a9	
actions/cache/save	6	v6.1.0	55cc8345863c7cc4c66a329aec7e433d2d1c52a9	
actions/cache	6	v6.1.0	55cc8345863c7cc4c66a329aec7e433d2d1c52a9	
actions/upload-artifact	7	v7.0.1	043fb46d1a93c77aae656e7c1c64a875d1fc6a0a	
actions/download-artifact	8	v8.0.1	3e5f45b2cfb9172054b4087a40e8e0b5a5461e7c	digest-mismatch
actions/setup-node	7	v7.0.0	820762786026740c76f36085b0efc47a31fe5020	
pnpm/action-setup	6	v6.0.10	0977fd99725f1db4007ccb2928dbb4e90d06cc86	
actions/github-script	8	v8	ed597411d8f924073f98dfc5c65a23a2325f34cd	
dorny/paths-filter	4	v4.0.3	ceb8a2b8f2d89434be7ff52d3de7ec3738c5cc9d	
dawidd6/action-download-artifact	24	v24	d63b86af1b34672e53c440b1b83979861906bad7	
docker/login-action	4	v4.6.0	dbcb813823bdd20940b903addbd779551569679f	
EOM
}

include_docs=0
format=text
roots=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --include-docs) include_docs=1; shift ;;
    --format)
      shift
      [[ $# -gt 0 ]] || { echo "error: --format needs a value" >&2; usage; exit 2; }
      case "$1" in
        text|github) format="$1" ;;
        *) echo "error: unknown format '$1'" >&2; usage; exit 2 ;;
      esac
      shift ;;
    -h|--help) usage; exit 2 ;;
    -*) echo "error: unknown flag '$1'" >&2; usage; exit 2 ;;
    *) roots+=("$1"); shift ;;
  esac
done

[[ ${#roots[@]} -gt 0 ]] || roots=(.github)

for root in "${roots[@]}"; do
  if [[ ! -e "$root" ]]; then
    echo "error: path not found: $root" >&2
    exit 2
  fi
done

manifest_file="$(mktemp)"
trap 'rm -f "$manifest_file"' EXIT
if [[ -n "${CHECK_ACTION_VERSIONS_MANIFEST:-}" ]]; then
  if [[ ! -r "${CHECK_ACTION_VERSIONS_MANIFEST}" ]]; then
    echo "error: manifest not readable: ${CHECK_ACTION_VERSIONS_MANIFEST}" >&2
    exit 2
  fi
  cat "${CHECK_ACTION_VERSIONS_MANIFEST}" > "$manifest_file"
else
  default_manifest > "$manifest_file"
fi

# Validate the manifest before trusting it. A guard that exits 0 on a malformed
# manifest would silently stop guarding, which is the exact failure mode #36850 is about.
if ! awk -F'\t' '
  /^[[:space:]]*$/ { next }
  {
    if (NF < 4)                        { printf "manifest line %d: expected >=4 tab-separated fields, got %d\n", NR, NF > "/dev/stderr"; bad = 1; next }
    if ($1 !~ /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.\/-]+$/) { printf "manifest line %d: bad action name %s\n", NR, $1 > "/dev/stderr"; bad = 1 }
    if ($2 !~ /^[0-9]+$/)              { printf "manifest line %d: min_major %s is not a number\n", NR, $2 > "/dev/stderr"; bad = 1 }
    if ($3 !~ /^v[0-9]/)               { printf "manifest line %d: pinned_version %s does not look like a tag\n", NR, $3 > "/dev/stderr"; bad = 1 }
    if ($4 !~ /^[0-9a-f]{40}$/)        { printf "manifest line %d: pinned_sha %s is not 40 hex chars\n", NR, $4 > "/dev/stderr"; bad = 1 }
    seen++
  }
  END { if (seen == 0) { print "manifest is empty" > "/dev/stderr"; bad = 1 } exit bad ? 1 : 0 }
' "$manifest_file"; then
  echo "error: malformed manifest" >&2
  exit 2
fi

# Collect files deterministically so the report is diffable run to run.
files_list="$(mktemp)"
trap 'rm -f "$manifest_file" "$files_list"' EXIT
{
  for root in "${roots[@]}"; do
    if [[ -f "$root" ]]; then
      echo "$root"
    else
      find "$root" -type f \( -name '*.yml' -o -name '*.yaml' \) 2>/dev/null
      if [[ $include_docs -eq 1 ]]; then
        find "$root" -type f -iname '*.md' 2>/dev/null
      fi
    fi
  done
} | LC_ALL=C sort -u > "$files_list"

report="$(mktemp)"
trap 'rm -f "$manifest_file" "$files_list" "$report"' EXIT

# One awk pass per file: the whole file is read into L[] so A3 can look ahead
# into the step body without a second read.
while IFS= read -r f; do
  [[ -n "$f" ]] || continue
  awk -v manifest="$manifest_file" -v FNAME="$f" '
    function longest_match(ref,   i, a, best) {
      best = ""
      for (i = 1; i <= nact; i++) {
        a = act[i]
        if (ref == a || index(ref, a "@") == 1) {
          if (length(a) > length(best)) best = a
        }
      }
      return best
    }

    BEGIN {
      while ((getline line < manifest) > 0) {
        if (line ~ /^[ \t]*$/) continue
        n = split(line, f, "\t")
        nact++
        act[nact]    = f[1]
        minmaj[f[1]] = f[2]
        pinver[f[1]] = f[3]
        pinsha[f[1]] = f[4]
        reqin[f[1]]  = (n >= 5 ? f[5] : "")
      }
      close(manifest)
    }

    { nl++; L[nl] = $0 }

    END {
      for (i = 1; i <= nl; i++) {
        line = L[i]
        if (line !~ /uses:[ \t]*[A-Za-z0-9_.-]+\/[A-Za-z0-9_.\/-]+@/) continue

        # Split off an inline comment before parsing the ref.
        body = line; comment = ""
        if (match(body, /#/)) {
          comment = substr(body, RSTART + 1)
          body = substr(body, 1, RSTART - 1)
        }
        gsub(/^[ \t]+|[ \t]+$/, "", comment)

        if (match(body, /uses:[ \t]*[A-Za-z0-9_.-]+\/[A-Za-z0-9_.\/-]+@[^ \t]+/) == 0) continue
        spec = substr(body, RSTART, RLENGTH)
        sub(/^uses:[ \t]*/, "", spec)

        at = index(spec, "@")
        name = substr(spec, 1, at - 1)
        ref  = substr(spec, at + 1)

        action = longest_match(name)
        if (action == "") continue          # out of manifest: deferred to #37194

        # ---- A2: SHA pin integrity ----
        if (ref ~ /^[0-9a-f]{40}$/) {
          if (ref != pinsha[action]) {
            printf "%s:%d: %s is SHA-pinned to %s, expected %s (%s)\n", FNAME, i, action, substr(ref,1,12), substr(pinsha[action],1,12), pinver[action]
            bad++
          } else if (comment != pinver[action]) {
            printf "%s:%d: %s SHA pin comment says %s, expected %s\n", FNAME, i, action, (comment == "" ? "(none)" : comment), pinver[action]
            bad++
          }
        }
        # ---- A1: version floor ----
        else if (ref ~ /^v[0-9]+$/ || ref ~ /^v[0-9]+\./) {
          maj = ref; sub(/^v/, "", maj); sub(/\..*$/, "", maj)
          if (maj + 0 < minmaj[action] + 0) {
            printf "%s:%d: %s@%s is below the required major (%s)\n", FNAME, i, action, ref, pinver[action]
            bad++
          }
        }
        else {
          printf "%s:%d: %s@%s is not a numeric version tag; required major is %s (%s)\n", FNAME, i, action, ref, minmaj[action], pinver[action]
          bad++
        }

        # ---- A3: required inputs must be written explicitly ----
        if (reqin[action] != "") {
          match(L[i], /[^ \t]/); base = RSTART
          nreq = split(reqin[action], want, ",")
          for (k = 1; k <= nreq; k++) {
            found = 0
            for (j = i + 1; j <= nl; j++) {
              nxt = L[j]
              if (nxt ~ /^[ \t]*$/) continue
              match(nxt, /[^ \t]/); ind = RSTART
              # Where the step ends. `uses:` and `with:` are SIBLING keys of the step
              # mapping, so in the composite / leading-`- name:` form they share an
              # indent -- stopping at ind <= base would never look inside `with:`.
              # Stop only on a genuine dedent, or on the next list item at or left of
              # this column (the `- uses:` form, where the dash sets base).
              if (ind < base) break
              if (nxt ~ /^[ \t]*-[ \t]/ && ind <= base) break
              if (nxt ~ ("^[ \t]*" want[k] "[ \t]*:")) { found = 1; break }
            }
            if (!found) {
              printf "%s:%d: %s is missing required input \047%s\047 (write it explicitly; do not inherit the default)\n", FNAME, i, action, want[k]
              bad++
            }
          }
        }
      }
      exit 0
    }
  ' "$f" >> "$report"
done < "$files_list"

violations=$(grep -c . "$report" 2>/dev/null || true)
violations=${violations:-0}

if [[ "$violations" -eq 0 ]]; then
  [[ "$format" == text ]] && echo "0 violations"
  exit 0
fi

if [[ "$format" == github ]]; then
  # file:line: message  ->  ::error file=<f>,line=<n>::<message>
  sed -E 's/^([^:]+):([0-9]+): (.*)$/::error file=\1,line=\2::\3/' "$report"
else
  cat "$report"
  file_count=$(cut -d: -f1 "$report" | LC_ALL=C sort -u | grep -c . || true)
  echo
  echo "${violations} violations in ${file_count:-0} files"
fi

exit 1
