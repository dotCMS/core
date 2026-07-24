---
name: dot-release-backport-lts
owner: "@dotcms/platform"
status: active
description: Backport closed GitHub issues labeled LTS Next Patch to the current LTS release branch. Finds linked PRs, applies diffs, commits, and pushes. Use when applying pending LTS patches to the release branch.
---

# LTS Backport

**TRIGGER GUARD:** Only invoke this skill when the user explicitly requests an **"LTS backport"**. A generic mention of "backport" alone does NOT trigger this skill. If the user says "backport" without "LTS", respond normally without invoking this skill.

You are now backporting fixes to the current LTS branch. Execute the following steps directly in this conversation using your own tools — do not spawn agents or subprocesses.

## Step 1: Environment check

Run these in parallel. Stop with a clear error if any fail:

```bash
gh --version
gh auth status
git status --short
git branch --show-current
```

The repo must be `dotCMS/core`. Verify:
```bash
gh repo view --json nameWithOwner --jq '.nameWithOwner'
```

If not `dotCMS/core`, stop and tell the user to run this from the correct repo root.

## Step 2: Resolve LTS number and branch

```bash
CURRENT_BRANCH=$(git branch --show-current)
echo "$CURRENT_BRANCH"
```

**Case A — on an LTS release branch:** If `CURRENT_BRANCH` matches the pattern `release-X.Y.Z_lts`, extract the LTS number automatically:

- Branch: `release-25.07.10_lts` → LTS number: `25.07.10`
- Pattern: extract the portion between `release-` and `_lts`

```bash
LTS_NUMBER=$(git branch --show-current | sed 's/release-\(.*\)_lts/\1/')
echo "LTS: $LTS_NUMBER"
```

Verify the extracted value matches the version format `XX.YY.ZZ` (digits only, dot-separated, e.g. `24.12.27`). If extraction fails or the result does not match that format, treat the branch as non-LTS and fall through to Case B.

**Case B — on any other branch:** The skill can still run, but the user MUST supply the LTS version as an argument. Accept the version from the skill invocation argument (e.g., `/dot-release-backport-lts 24.12.27`).

The argument must match the format `\d{2}\.\d{2}\.\d{2}` (e.g., `24.12.27`). If it does not match or no argument was provided, stop and tell the user:

> "Current branch `CURRENT_BRANCH` is not an LTS release branch (expected format: `release-X.Y.Z_lts`). Please provide the LTS version number as an argument, e.g.: `/dot-release-backport-lts 24.12.27`"

If a valid version argument was provided, derive the target branch name:
```
TARGET_BRANCH=release-LTS_NUMBER_lts
```

Verify the branch exists on the remote before checking out — do NOT create it:
```bash
git fetch origin "$TARGET_BRANCH" 2>&1
```

If the fetch fails (branch not found on remote), stop and tell the user:
> "Branch `release-LTS_NUMBER_lts` does not exist on the remote. Verify the version number and try again."

If the fetch succeeds, check out the branch:
```bash
git checkout "$TARGET_BRANCH"
```

Update `CURRENT_BRANCH` to `TARGET_BRANCH` for all subsequent steps (commit, push, report).

## Step 3: Fetch issues labeled "LTS: Next Patch"

```bash
gh issue list --repo dotCMS/core \
  --label "LTS: Next Patch" \
  --state closed \
  --json number,title,url \
  --limit 100
```

If no issues are returned, tell the user "No closed issues with label 'LTS: Next Patch' found." and stop.

Print the list to the user before proceeding: "Found N issue(s) to backport."

Initialize three tracking lists (internal):
- `completed`: issue numbers successfully backported
- `skipped`: issue numbers skipped with reason
- `already_backported`: issue numbers already present in hotfix_tracking.md

## Step 4: Locate hotfix_tracking.md and determine next line number

First, search for the file anywhere in the repository:
```bash
find . -name "hotfix_tracking.md" -not -path "./.git/*" 2>/dev/null
```

If multiple matches are found, prefer the one closest to the repo root (shortest path). Store the resolved path as `HOTFIX_FILE`.

If no file is found, stop and tell the user: "hotfix_tracking.md not found in repo in this branch. The file may have a different name or may not exist yet in this branch."

Once located, count existing numbered entries to determine the next sequential number:
```bash
grep -cP '^\d+\.' "$HOTFIX_FILE" 2>/dev/null || echo 0
```

Store as `NEXT_LINE_NUMBER`. Use `$HOTFIX_FILE` (the resolved path) for all subsequent reads, edits, and `git add` calls involving this file.

## Step 5: Process each issue

For each issue from Step 3, execute the following sub-steps. Announce which issue you're processing: "Processing issue #NUMBER: TITLE"

The commit and push for each issue happen **only after that issue's backport is 100% complete** — meaning all files from all PRs for that issue have been successfully applied (either automatically or manually confirmed by the user). Do not commit or push partial work.

### 5a. Check if already backported

Before doing anything else, check whether this issue is already committed in `$HOTFIX_FILE`. Always check the **committed** version of the file (not the working copy), to avoid false positives from abandoned partial runs that left an uncommitted entry:
```bash
git show HEAD:"$HOTFIX_FILE" | grep -q "#ISSUE_NUMBER" && echo "ALREADY_BACKPORTED" || echo "NEW"
```

If the issue number is found, mark the issue as **already backported** in the tracking list (separate from `completed` and `skipped`), print a one-line notice — "Issue #NUMBER already backported, skipping." — and continue to the next issue. Do not modify `$HOTFIX_FILE` or apply any patches.

### 5c. Add entry to hotfix_tracking.md

Append a new line at the end of the numbered list in `$HOTFIX_FILE` with this format:
```
N. ISSUE_URL : ISSUE_TITLE #ISSUE_NUMBER
```

Example:
```
26. https://github.com/dotCMS/core/issues/34500 : [DEFECT] Fix something broken #34500
```

Use the Edit tool to append the line to `$HOTFIX_FILE`. Do this BEFORE attempting to apply the diff — you will remove it later if the backport is skipped entirely.

Increment `NEXT_LINE_NUMBER` by 1 after each addition.

### 5d. Find linked PRs for the issue

Use the GitHub GraphQL API to retrieve PRs linked to this issue through the "Development" section (timeline events):

```bash
gh api graphql -f query='
{
  repository(owner: "dotCMS", name: "core") {
    issue(number: ISSUE_NUMBER) {
      timelineItems(first: 50, itemTypes: [CONNECTED_EVENT, CROSS_REFERENCED_EVENT, CLOSED_EVENT]) {
        nodes {
          ... on ConnectedEvent {
            subject {
              ... on PullRequest {
                number
                title
                url
                body
                state
                mergedAt
              }
            }
          }
          ... on CrossReferencedEvent {
            source {
              ... on PullRequest {
                number
                title
                url
                body
                state
                mergedAt
              }
            }
          }
          ... on ClosedEvent {
            closer {
              ... on PullRequest {
                number
                title
                url
                body
                state
                mergedAt
              }
            }
          }
        }
      }
    }
  }
}'
```

Also search for PRs that mention the issue directly:
```bash
gh pr list --repo dotCMS/core \
  --search "fixes:#ISSUE_NUMBER in:body" \
  --state merged \
  --json number,title,url,body \
  --limit 20
```

Deduplicate by PR number. Collect all unique PRs found from both queries.

**IMPORTANT:** An issue may have multiple merged PRs (e.g., a primary fix PR and a companion refactor/cleanup PR). You must find and apply ALL of them — not just the first one. Check both queries thoroughly and deduplicate.

If no PRs are found, mark the issue as **skipped** (reason: "no linked PRs found"), remove the line added in 5c from `hotfix_tracking.md`, and continue to the next issue.

### 5e. Verify PR-to-issue relation

For each candidate PR, examine the PR body for the text `This PR Fixes:` followed by the issue number. Accept any of these patterns (case-insensitive):
- `This PR Fixes: #ISSUE_NUMBER`
- `This PR Fixes #ISSUE_NUMBER`
- `Fixes #ISSUE_NUMBER`
- `Closes #ISSUE_NUMBER`
- `Resolves #ISSUE_NUMBER`

Also accept PRs that mention the issue in any context (e.g., `partial`, `companion to`) as long as the issue number appears — these are companion PRs that are part of the same fix.

**Only accept PRs where `state == "MERGED"`** (i.e., `mergedAt` is non-null). Discard open or closed-without-merge PRs.

Keep only PRs that contain a reference to the exact issue number being processed. If none pass this check, then skip the issue and flag an error to later specify in the report.

Sort the accepted PRs by PR number ascending (apply oldest first).

### 5f. Apply each PR's diff

For each accepted PR, in PR number ascending order:

Save the name of the file to later add it in a git add command for committing.

**Get the diff:**
```bash
gh pr diff PR_NUMBER --repo dotCMS/core > /tmp/backport_patch_ISSUE_NUMBER_PR_NUMBER.patch
```

**Check if the patch applies cleanly (dry run):**
```bash
git apply --check /tmp/backport_patch_ISSUE_NUMBER_PR_NUMBER.patch 2>&1
```

**Interpret the result:**

- **Success (exit 0)**: Apply the patch for real:
  ```bash
  git apply /tmp/backport_patch_ISSUE_NUMBER_PR_NUMBER.patch
  ```

- **Failure — file not found or hunk does not apply**: This means the target file or code context does not exist in the current branch. **Do NOT skip the issue and do NOT make any commit.**

  Instead:
    1. Capture and display the full error output from `git apply --check`.
    2. Identify the specific file(s) that could not be patched.
    3. **Stop processing this issue** and report to the user in this format:

  ```
  ⚠️ Backport blocked for issue #ISSUE_NUMBER (PR #PR_NUMBER)

  The following file(s) could not be patched automatically:
    - path/to/file.ext — <reason: e.g., "hunk does not apply", "file not found">

  Error details:
  <full error output from git apply --check>

  Files already modified for this issue so far:
  <list of files successfully patched for this issue before the failure>

  Please apply the changes for these file(s) manually, then tell me to continue.
  ```

    4. **Wait** for the user to confirm they have manually applied the changes before proceeding.
    5. When the user confirms (e.g., "done", "continue", "I've applied it"), resume processing the remaining files/PRs for this issue.
       If the user instead says to skip or abandon this issue (e.g., "skip", "abandon", "skip this one"), remove the line added in 5c from `$HOTFIX_FILE` using the Edit tool, then continue to the next issue without committing.
    6. **Do NOT remove the hotfix_tracking.md entry** unless the user explicitly abandons the issue — the issue is still being backported, just with manual assistance.

- **Failure — other reason (context mismatch, whitespace, etc.)**: Try with 3-way merge:
  ```bash
  git apply --3way /tmp/backport_patch_ISSUE_NUMBER_PR_NUMBER.patch 2>&1
  ```

  If the 3-way merge **succeeds** but leaves conflict markers (files marked `UU` in `git status`), **do NOT auto-resolve**. Instead, stop and report to the user using the same blocking format above, listing the conflicted files and waiting for manual resolution confirmation before continuing.

  If the 3-way merge **fails entirely** (exit non-zero with no partial apply), split the patch into per-file patches and apply each one individually:

  ```python
  # Split patch into per-file patches
  python3 - << 'EOF'
  import re, os
  with open('/tmp/backport_patch_ISSUE_NUMBER_PR_NUMBER.patch', 'r') as f:
      content = f.read()
  parts = re.split(r'(?=^diff --git )', content, flags=re.MULTILINE)
  parts = [p for p in parts if p.strip()]
  for i, part in enumerate(parts):
      m = re.match(r'diff --git a/(.*) b/', part)
      if m:
          fname = m.group(1).replace('/', '_').replace('.', '_')
          out = f'/tmp/patch_{i:03d}_{fname[:80]}.patch'
          with open(out, 'w') as f:
              f.write(part)
          print(f"Created: {out}")
  EOF
  ```

  Then apply each per-file patch individually:
  ```bash
  for patch in $(ls /tmp/patch_*.patch | sort); do
    if git apply --check "$patch" 2>/dev/null; then
      git apply "$patch" && echo "✅ Applied: $patch"
    elif git apply --3way "$patch" 2>/dev/null; then
      echo "✅ Applied (3way): $patch"
    else
      fname=$(grep "^diff --git" "$patch" | head -1 | sed 's/diff --git a\///' | sed 's/ b\/.*//')
      echo "❌ Failed: $fname"
    fi
  done
  ```

  For every file that failed in the per-file loop, **stop and report it to the user using the same blocking format above**. Do not continue until the user confirms each failed file has been manually resolved.

  After the per-file loop (and any manual confirmations), check for any files left with conflict markers (`UU` in `git status`) and report those too before proceeding.

**Collect list of modified and newly added files** after each successful `git apply` using:
```bash
git diff --name-only          # modified tracked files
git ls-files --others --exclude-standard  # new untracked files added by the patch
```
Accumulate these lists across all PRs for the current issue. They are used both for the commit step (5g) and for targeted rollback if a later PR in the same issue fails entirely and the issue must be abandoned.

### 5g. Commit and push — only when the issue is 100% complete

**Only reach this step when ALL files from ALL PRs for the current issue have been applied** — whether automatically or confirmed manually by the user. Partial backports must never be committed.

**Stage all modified files plus the hotfix tracking file:**
Do not execute a git add all, you need to git add all the modified files, one by one:
```bash
git add $HOTFIX_FILE
git add {file}
```

**Commit with the exact format:**
Use `--no-verify` to skip pre-commit hooks (linters and test runners that are irrelevant for cherry-picked LTS backport commits):
```bash
git commit -m "#ISSUE_NUMBER include in LTS_NUMBER LTS" --no-verify
```

Example: `#34500 include in 25.07.10 LTS`

**Push to the current branch:**
```bash
git push origin CURRENT_BRANCH
```

If the push fails, report the error and stop processing further issues. The user will need to resolve the push issue manually.

**Mark the issue as `completed` in the tracking list.**

**Clean up temp files:**
```bash
rm -f /tmp/backport_patch_ISSUE_NUMBER_*.patch /tmp/patch_*.patch
```

## Step 6: Final report

After all issues have been processed, print the final report in this format:

```
## LTS Backport Report — release-LTS_NUMBER_lts

| Issue | Title | State | Notes |
|-------|-------|-------|-------|
| #N    | Title | ✅ completed          | — |
| #N    | Title | ✅ already backported | — |
| #N    | Title | ⏭️ skipped            | target code not found in current branch |
| #N    | Title | ⏭️ skipped            | no linked PRs found |

Summary: X completed, Y already backported, Z skipped
```

If any issues were skipped, add a section:
```
### Issues Requiring Manual Review
The following issues were skipped and need manual attention:
- #N: REASON — check if prerequisite commits need to be backported first
```

---

## Error Handling Reference

| Situation | Action |
|-----------|--------|
| `git apply --check` fails with "file not found" | Stop, report blocked file(s) to user, wait for manual confirmation, then continue |
| `git apply --check` fails with "hunk does not apply" | Stop, report blocked file(s) to user, wait for manual confirmation, then continue |
| `git apply --3way` leaves conflict markers (`UU`) | Stop, report conflicted file(s) to user, wait for manual confirmation, then continue |
| `git apply --3way` fails entirely | Split into per-file patches; for each failed file, stop and wait for manual confirmation |
| No PRs linked to issue | Skip issue, remove hotfix_tracking.md entry |
| User says "skip" / "abandon" when blocked | Remove hotfix_tracking.md entry for this issue, continue to next issue — never commit partial work |
| Push fails | Stop all processing, report error with instructions |
| `hotfix_tracking.md` not found | Stop and tell user the file doesn't exist in this branch |
| Issue number found in committed `hotfix_tracking.md` | Mark as already backported, skip silently |
| Patch failure of any kind | **Never commit partial work** — always wait for 100% completion of the issue before committing |
