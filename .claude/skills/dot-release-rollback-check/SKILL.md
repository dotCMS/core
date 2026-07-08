---
name: dot-release-rollback-check
owner: "@dotcms/platform"
status: active
description: Check whether a dotCMS release can be safely rolled back to a previous version by inspecting all PRs merged between the two versions for rollback-safety labels. Use when the user asks whether a release or version can be safely rolled back, mentions rollback safety, or runs /check-release-rollback.
---

Check whether a dotCMS release can be safely rolled back to a previous version by
inspecting all PRs merged between the two versions for rollback safety labels.

Arguments: <current-version> <target-version>

Example: /check-release-rollback 26.04.28-02_7149dce 26.04.11-02_9650131

Version format: `YY.MM.DD-patch_commitsha` (short commit SHA after the underscore).

---

## Step 0 — Validate inputs

Parse `current-version` (FROM) and `target-version` (TO) from the arguments.

Validate that both strings match the pattern `\d{2}\.\d{2}\.\d{2}-\d+_[0-9a-f]+`.
If either is missing or malformed, stop and tell the user:
  "Version strings must be in the format YY.MM.DD-patch_commitsha (e.g. 26.04.28-02_7149dce)."

Extract the short SHA from each: everything after the last `_`.
- `current-sha` = SHA from current-version
- `target-sha`  = SHA from target-version

If the two SHAs are identical, stop and report "Nothing to check — versions are identical."

**Repo root**: Use the current working directory — this command lives inside dotCMS/core
and must be run from within the repo. Verify with:
```bash
git rev-parse --show-toplevel 2>/dev/null || echo "NOT_A_REPO"
```
If the output is `NOT_A_REPO`, stop and tell the user to run this from inside a dotCMS/core checkout.

---

## Step 1 — Get commits between versions

```bash
git log --oneline <target-sha>..<current-sha>
```

If git cannot resolve the SHAs, fetch first:
```bash
git fetch --all 2>&1 | tail -3
git log --oneline <target-sha>..<current-sha>
```

If the SHAs still cannot be resolved after fetching, stop and tell the user:
  "Could not resolve one or both SHAs. Please verify the version strings are correct
   and that this is the right repository."

If the log is empty, report "No commits found between these versions — nothing to check."

---

## Step 2 — Extract merged PR numbers

Extract the **last** `#NNNNN` from each commit line. This is the actually-merged PR
(backport commits carry both the original and backport number — the last is the backport).
Revert commits (`Revert "... (#X)" (#Y)`) are handled correctly by this rule (Y is kept).
Commits referencing only an issue number (e.g. `fixes #36966`) with no PR number are skipped.

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
print(' '.join(str(p) for p in sorted(prs)))
"
```

Store as `PR_LIST`. If empty after the python extraction, report "No PR numbers found in
the commit range." and stop.

---

## Step 3 — Batch-fetch PR metadata

Get the date range from the two boundary commits:
```bash
TARGET_DATE=$(git log -1 --format="%aI" <target-sha>)
CURRENT_DATE=$(git log -1 --format="%aI" <current-sha>)
```

Fetch all merged PRs in that date range in ONE call (avoids per-PR rate limiting):
```bash
gh pr list \
  --repo dotCMS/core \
  --state merged \
  --json number,title,labels \
  --limit 500 \
  --search "merged:>=${TARGET_DATE} merged:<=${CURRENT_DATE}"
```

Cross-reference the result against `PR_LIST` — keep only entries whose `number` appears
in `PR_LIST`. Any number in `PR_LIST` not present in the API response gets classified as
UNLABELED with note "(not found in API response — may be a cross-repo or draft PR)".

---

## Step 4 — Classify each PR

**Label constants** (canonical source: `.github/scripts/gather-release-data/src/categorize.ts`):
```
ROLLBACK_UNSAFE_LABELS = ["AI: Not Safe To Rollback", "Human: Not Safe To Rollback"]
ROLLBACK_SAFE_LABELS   = ["AI: Safe To Rollback",     "Human: Safe To Rollback"]
INFRA_LABELS           = ["Area : CI/CD"]
INFRA_TITLE_PREFIXES   = ["ci:", "ci(", "chore:", "chore(", "build:"]
```

All label comparisons are **case-insensitive** (lowercase both sides before comparing).

Classification order (first match wins):
1. **CONFLICTING** — has ≥1 ROLLBACK_UNSAFE label **and** ≥1 ROLLBACK_SAFE label
2. **NOT_SAFE**    — has ≥1 ROLLBACK_UNSAFE label
3. **SAFE**        — has ≥1 ROLLBACK_SAFE label
4. **UNLABELED**   — has neither

For UNLABELED PRs, also set a `low_risk` flag = true if the PR has an INFRA_LABELS label
or its title starts with an INFRA_TITLE_PREFIXES prefix. These are noted separately in
the report but do not block the YES verdict.

---

## Step 5 — Generate the report

```
## Rollback Safety Report
From: <current-version>
To:   <target-version>
Total PRs: N | Safe: N | Not Safe: N | Conflicting: N | Unlabeled: N (N low-risk)

---

### NOT SAFE TO ROLLBACK
(omit section if empty)

| PR | Title | Risk note |
|----|-------|-----------|
| [#N](https://github.com/dotCMS/core/pull/N) | title | inferred risk, e.g. "DB migration", "ES index change" |

For each NOT_SAFE PR infer the risk type from the title:
- "migration" / "Task2…" / "db:" / "schema" → "DB schema migration"
- "reindex" / "opensearch" / "elasticsearch" / "ES" → "ES index change"
- "api:" / "REST" / "breaking" → "API contract change"
- Revert of another NOT_SAFE PR → "Revert of NOT_SAFE #X — both must be considered together"
- Otherwise → "Manual review required"

---

### CONFLICTING LABELS
(omit section if empty)

| PR | Title |
|----|-------|
| [#N](https://github.com/dotCMS/core/pull/N) | title |

---

### UNLABELED — Needs Assessment
(omit section if empty)

| PR | Title | Low-risk? |
|----|-------|-----------|
| [#N](https://github.com/dotCMS/core/pull/N) | title | Yes (CI/infra) / No |

---

### VERDICT

ROLLBACK SAFE: YES / NO / CONDITIONAL

- YES         — no NOT_SAFE, no CONFLICTING, and all UNLABELED are low-risk (CI/infra)
- NO          — one or more NOT_SAFE PRs
- CONDITIONAL — no NOT_SAFE, but CONFLICTING or non-trivial UNLABELED PRs require
                manual engineering review before proceeding

Followed by a 2-3 sentence plain-English summary of the main risks.
```

---

## Step 6 — Offer to save the report

Ask: "Would you like me to save this as a .txt file?"

If yes, save to cwd as: `rollback-safety-<current-version>-to-<target-version>.txt`
