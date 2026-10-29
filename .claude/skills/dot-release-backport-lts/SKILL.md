---
name: dot-release-backport-lts
owner: "@dotcms/maintenance"
status: active
description: Backport closed GitHub issues labeled LTS Next Patch to the current LTS release branch. Finds linked PRs, applies diffs, commits, and pushes. Then triggers a Docker build of the branch and, once given the resulting image, spins up a dotCMS instance from it to QA the backport and smoke-test basic functionality. Use when applying pending LTS patches to the release branch.
argument-hint: "[lts-version] (e.g. 25.07.10) — required unless you're already on a release-X.Y.Z_lts branch, which the skill reads it from automatically"
---

# LTS Backport

**TRIGGER GUARD:** Only invoke this skill when the user explicitly requests an **"LTS backport"**. A generic mention of "backport" alone does NOT trigger this skill. If the user says "backport" without "LTS", respond normally without invoking this skill.

You are now backporting fixes to the current LTS branch. Execute the following steps directly in this conversation using your own tools — do not spawn agents or subprocesses.

**Placeholders are not literal text:** Every `UPPER_SNAKE_CASE` token used below (`LTS_NUMBER`, `CURRENT_BRANCH`, `TARGET_BRANCH`, `ISSUE_NUMBER`, `PR_NUMBER`, `HOTFIX_FILE`, `DOCKER_IMAGE`, etc.) stands for a value resolved earlier in the process. Anywhere you print a message, error, prompt, or report to the user, substitute the actual resolved value — never print the placeholder name itself. For example, once `TARGET_BRANCH` resolves to `release-25.07.10_lts`, a report header must read "LTS Backport Report — release-25.07.10_lts", not "LTS Backport Report — release-LTS_NUMBER_lts". This applies to every step from here through the final report.

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

After all issues have been processed, print the final report in this format (with `TARGET_BRANCH` replaced by its actual resolved value, e.g. `release-25.07.10_lts`):

```
## LTS Backport Report — TARGET_BRANCH

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

If the `completed` list is empty (nothing new was actually backported this run — e.g. everything was already backported or skipped), stop here: there is no new commit to build or QA, so do not proceed to Step 7.

Otherwise, proceed directly to Step 7 — triggering the Docker build is part of this skill's standard flow, not a separate opt-in.

## Step 7: Trigger the Docker build

**Identify the GitHub account to use.** This skill may run under different users' `gh` setups, so never hardcode an account name. Run `gh auth status` and look at the authenticated accounts:

- If exactly **one** account is logged in and active, use it.
- If **multiple** accounts are logged in, ask the user which one to use for triggering this workflow (e.g. "I see accounts X and Y authenticated — which one should trigger the build?"), then run `gh auth switch --user ACCOUNT_NAME` to activate the chosen one before continuing.

**Trigger the workflow** against the LTS branch you just pushed to (`TARGET_BRANCH`). Use the workflow file name, not its display name — `gh workflow run` can be unreliable matching names containing `[`/`]`:

```bash
gh workflow run legacy-release_publish-dotcms-docker-image.yml \
  --repo dotCMS/core \
  --ref "TARGET_BRANCH" \
  -f multi_arch=true \
  -f docker_registry=DOCKER.IO
```

(Display name: "Build/Push dotCMS docker image [DEPRECATED]".) If the user wants a custom tag on the resulting image, pass `-f custom_tag=THEIR_TAG` as well.

Immediately after dispatching, fetch the run URL so the user can follow progress:
```bash
gh run list --repo dotCMS/core --workflow "legacy-release_publish-dotcms-docker-image.yml" --limit 1 --json databaseId,url,status
```

Report the run URL to the user and tell them:

> "Docker build triggered on `TARGET_BRANCH` (run: RUN_URL). Let me know once it finishes, then share the resulting Docker image (e.g. `dotcms/dotcms:TAG`) so I can QA the backported fix(es) in it."

Then stop and wait — do not poll the workflow yourself; the user will tell you when it's done and give you the image.

## Step 8: Receive and validate the Docker image

When the user provides a Docker image reference, sanity-check its shape (e.g. `dotcms/dotcms:TAG` or a `ghcr.io/...` reference) before using it. If it looks malformed or unrelated to this build, ask the user to confirm or re-check before proceeding.

## Step 9: Spin up a dotCMS instance from the image and QA the backport

Use `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml` as the base — the example built exactly for this: one dotCMS node + Postgres + OpenSearch, auto-loading a starter with demo content. Confirm the file still exists at that path (example paths can shift between versions); if not, search for its replacement with `find . -iname docker-compose.yml -path "*demo-site*"`.

This file has no variable-substitution for the fields below — they are hardcoded values in the YAML — so do not edit the tracked repo file in place. Copy it to a scratch location first and edit the copy:

```bash
mkdir -p /tmp/lts-backport-qa && cp docker/docker-compose-examples/single-node-demo-site/docker-compose.yml /tmp/lts-backport-qa/
```

- **Image**: in the copy, change the `dotcms:` service's `image:` line from `dotcms/dotcms:latest` to `DOCKER_IMAGE` (the exact reference the user supplied, e.g. `dotcms/dotcms:TAG`).
- **Starter**: the `CUSTOM_STARTER_URL` env var on the `dotcms` service controls which starter zip loads on first boot. Resolve the correct starter for the branch under test by checking `dotCMS/build.gradle` (grep `starter group`) **on `TARGET_BRANCH`** for the `starter_date`, then set the value to `https://dotcms-repo.b-cdn.net/libs-release/com/dotcms/starter/STARTER_DATE/starter-STARTER_DATE.zip`. If `build.gradle` doesn't exist on that branch (fully-Maven branches), run `docker/docker-compose-examples/scripts/dotcms-get-demo-site-starter-urls.sh` and pick the entry matching the closest released version instead.
- **Credentials**: default admin login is `admin@dotcms.com` / `admin` (from `DOT_INITIAL_ADMIN_PASSWORD` in the compose file) unless the user overrides it.
- **Ports — check for conflicts before starting, never free a port by stopping something already running:** the compose file's default host ports are `8082` (dotCMS HTTP), `8443` (dotCMS HTTPS), `9200` and `9600` (OpenSearch). Any of these may already be bound by something else on the user's machine (another dotCMS instance, a different project, etc.) — **do not stop, kill, or touch whatever is using them.** Instead, for each of the four, probe whether the default is free and pick the next free one if not, then rewrite only the **host side** of that port's mapping in the copied compose file (the container-side port stays the same, e.g. `8085:8082`):

  ```bash
  port_in_use() {
    (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && { exec 3<&- 3>&-; return 0; } || return 1
  }
  pick_free_port() {
    local port=$1
    while port_in_use "$port"; do port=$((port + 1)); done
    echo "$port"
  }
  DOTCMS_HTTP_PORT=$(pick_free_port 8082)
  DOTCMS_HTTPS_PORT=$(pick_free_port 8443)
  OPENSEARCH_PORT=$(pick_free_port 9200)
  OPENSEARCH_PERF_PORT=$(pick_free_port 9600)
  ```

  Then edit the copied compose file's `ports:` entries so each reads `"$DOTCMS_HTTP_PORT:8082"`, `"$DOTCMS_HTTPS_PORT:8443"`, `"$OPENSEARCH_PORT:9200"`, `"$OPENSEARCH_PERF_PORT:9600"` respectively — substituting the resolved values, not the literal variable names. If any port had to move off its default, tell the user which one and why (e.g. "8082 was already in use, dotCMS will be reachable on 8085 instead") so they know where to point the browser later.
- Start it with `docker compose -f /tmp/lts-backport-qa/docker-compose.yml up -d`, and tear it down afterward with `docker compose -f /tmp/lts-backport-qa/docker-compose.yml down -v` once QA is done (confirm with the user before destroying volumes if they want to keep the instance running).
- Wait for the instance to report healthy — tail `docker compose -f /tmp/lts-backport-qa/docker-compose.yml logs -f dotcms` until you see it finish initializing (or poll `http://localhost:$DOTCMS_HTTP_PORT/api/v1/appconfiguration`) before testing.

**Verify the specific backport(s):** For each issue backported in this run, re-derive what the fix actually changed (from the PR diff already fetched in Step 5f) and exercise that code path against the running instance — via REST API calls (`curl`), CLI, or whatever reproduces the original bug fastest. Confirm the bug is actually fixed in this image, not just that the server boots.

**Smoke test basic functionality** (to confirm nothing else broke):
1. Create a new Content Type.
2. Add a relationship field relating it to an existing/second Content Type.
3. Create a Content instance of that new Content Type.
4. Create a Template.
5. Create a new Page using that Template.
6. Add the content created in step 3 onto that page.

Do as much of this as practical via REST API/CLI first. For the parts that benefit from visual confirmation (page rendering, editor UX), ask the user:

> "I've verified the backport and basic smoke tests via API. Can you grant me browser access via the Claude Chrome extension so we can visually confirm the page renders correctly with the new content? The instance is at http://localhost:$DOTCMS_HTTP_PORT."

Use that browser access, once granted, to visually confirm the content type, content, template, and page all work end-to-end in the UI.

## Step 10: Final combined report

Report to the user:
- Which issue(s)/PR(s) were backported (carried over from the Step 6 table).
- The Docker image tested (`DOCKER_IMAGE`) and the workflow run it came from.
- Pass/fail for each specific backport verification, with how it was verified (API call, UI check) and what was observed.
- Pass/fail for each smoke-test step (content type, relationship, content, template, page, page content), noting any that failed or looked off.
- Any findings, anomalies, or regressions discovered — even if unrelated to the backported issues.

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
