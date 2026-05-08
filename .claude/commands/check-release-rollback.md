Check whether a dotCMS release can be safely rolled back to a previous version by inspecting all PRs between the two versions for rollback safety labels.

Usage: /check-release-rollback <current-version> <target-version>

Example: /check-release-rollback 26.04.28-02_7149dce 26.04.11-02_9650131

The version format is: `YY.MM.DD-patch_commitsha` (the commit SHA is the short hash suffix).

## Step 0 — Resolve inputs

Parse the two version strings from the arguments:
- `current-version` — the version currently running (FROM)
- `target-version`  — the version to roll back TO

Extract the short commit SHA from each (the part after `_`):
- current SHA = everything after the last `_` in current-version
- target SHA  = everything after the last `_` in target-version

The dotCMS core repo is at `DOTCMS_CORE_PATH` (from ~/.claude/CLAUDE.md context) or
`/Users/infoserveisaudiovisuals/dev/core` as fallback.

If either argument is missing, ask the user to provide both version strings.

## Step 1 — Get commits between versions

Run:
```bash
cd $DOTCMS_CORE_PATH && git log --oneline <target-sha>..<current-sha> 2>&1
```

If git cannot resolve the SHAs (e.g. they are not fetched locally), run:
```bash
cd $DOTCMS_CORE_PATH && git fetch --all 2>&1 | tail -3
```
Then retry.

## Step 2 — Extract merged PR numbers

From the git log output, extract the **last** `#NNNNN` from each commit line — this is the
actually-merged PR number (backport commits have two numbers; only the last matters).

Use python3 to extract unique PR numbers:
```bash
git log --oneline <target-sha>..<current-sha> | python3 -c "
import sys, re
prs = []
seen = set()
for line in sys.stdin:
    nums = re.findall(r'#(\d+)', line)
    if nums:
        pr = int(nums[-1])
        if pr not in seen:
            seen.add(pr)
            prs.append(pr)
for pr in sorted(prs):
    print(pr)
"
```

Store this as the full PR list. If the list is empty, report "No PRs found between these versions" and stop.

## Step 3 — Check rollback labels via gh CLI

Fetch the title and labels for every PR in the list:
```bash
for pr in <space-separated list>; do
  gh pr view $pr --repo dotCMS/core --json number,title,labels \
    --jq '"#\(.number)|\(.title)|\([.labels[].name] | join(","))"' 2>/dev/null \
    || echo "#$pr|ERROR|"
done
```

Parse each line. For each PR classify it as:
- **SAFE**        — has label `AI: Safe To Rollback` and NOT `AI: Not Safe To Rollback`
- **NOT_SAFE**    — has label `AI: Not Safe To Rollback` (regardless of Safe label)
- **CONFLICTING** — has BOTH `AI: Safe To Rollback` AND `AI: Not Safe To Rollback`
- **UNLABELED**   — has neither label

Note: label matching must be case-insensitive (both `AI: Safe To Rollback` and `AI: Safe to Rollback` are valid).

## Step 4 — Generate the report

Output the following report:

---

## Rollback Safety Report
**From:** `<current-version>`
**To:**   `<target-version>`
**Total PRs:** N  |  **Safe:** N  |  **Not Safe:** N  |  **Conflicting:** N  |  **Unlabeled:** N

---

### NOT SAFE TO ROLLBACK
(omit this section if empty)

| PR | Title | Labels |
|----|-------|--------|
| [#NNNNN](https://github.com/dotCMS/core/pull/NNNNN) | title | AI: Not Safe To Rollback |

For each Not Safe PR, add a one-line note explaining WHY it is risky if discernible from
the title (e.g. "DB schema migration", "ES index change", "API contract change"). If it is
a revert of another Not Safe PR also in this list, note the pairing.

---

### CONFLICTING LABELS
(omit this section if empty)

| PR | Title |
|----|-------|
| [#NNNNN](https://github.com/dotCMS/core/pull/NNNNN) | title |

---

### UNLABELED — Never Assessed
(omit this section if empty)

| PR | Title |
|----|-------|
| [#NNNNN](https://github.com/dotCMS/core/pull/NNNNN) | title |

For CI-only changes (label `Area : CI/CD` or title starts with `ci:`) note they are
likely low-risk even without a rollback label.

---

### VERDICT

**ROLLBACK SAFE: YES / NO / CONDITIONAL**

- **YES** — all PRs are labeled Safe, no Not Safe or Conflicting, no Unlabeled
- **NO** — one or more PRs labeled Not Safe To Rollback
- **CONDITIONAL** — no Not Safe PRs, but there are Unlabeled or Conflicting PRs that
  require manual engineering review before proceeding

Followed by a 2-3 sentence plain-English summary of the main risks, e.g.:
> "Rollback is blocked by #NNNNN (DB migration) and #NNNNN (ES index change). These
> involve schema changes that cannot be automatically reversed. Engineering must confirm
> that the migration tracking tables allow clean reversal before proceeding."

---

## Step 5 — Optionally save the report

After displaying the report, ask the user:
"Would you like me to save this report as a .txt file?"

If yes, save to the current working directory as:
`rollback-safety-<current-version>-to-<target-version>.txt`

## Error handling

- If a PR returns ERROR from gh CLI, list it as UNLABELED with note "(fetch failed)"
- If git SHAs cannot be resolved even after fetch, ask the user to verify the version strings
- If fewer than 3 PRs are found, warn that the range may be too narrow and confirm with the user
- If the two versions are the same, report "Nothing to check — versions are identical"
