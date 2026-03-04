---
name: skill-doctor
description: Use when a repo skill fails, produces errors, gives wrong instructions, or references stale information. Also use to manage local skill overrides, check skill staleness, or optimize skill token usage and quality.
---

# Skill Doctor

Diagnose issues with repository skills, manage local overrides, and optimize skill quality.

**Scope:** Skills in the current repository's `.claude/skills/` directory only.

## Argument Parsing

Arguments received: `$ARGUMENTS`

Parse the arguments to determine mode and target:

| Pattern | Mode | Action |
|---------|------|--------|
| (empty) | diagnose | Diagnose most recent skill issue |
| `<skill-name>` | diagnose | Diagnose specific skill |
| `--optimize <skill>` | optimize | Analyze skill quality |
| `--manage` | manage | Show status of all repo skills |
| `--manage <skill>` | manage | Show status of specific skill |
| `clone <skill>` | manage | Clone repo skill to local override |
| `diff <skill>` | manage | Diff local override vs repo |
| `sync <skill>` | manage | Pull repo updates into local |
| `revert <skill>` | manage | Delete local override |
| `report <skill>` | deliver | Submit feedback report (Discussion + Slack) |
| `gist <skill>` | deliver | Create gist with local changes |
| `pr <skill>` | deliver | Create PR with local changes |
| `resolve <skill>` | resolve | Mark reports as resolved with a fix PR |

Route to the appropriate section below based on the parsed mode.

---

## Mode: Manage

### Status (--manage with no skill name)

1. List all skills in `.claude/skills/` in the current repo:
   ```bash
   ls -d .claude/skills/*/
   ```

2. For each skill, check:
   - Does `~/.claude/skills/<name>/` exist? → local override active
   - If local override exists, read `~/.claude/skills/<name>/.skill-origin.json`
   - Compare `cloned_from_sha` against current repo HEAD for that path:
     ```bash
     git log --oneline <cloned_from_sha>..HEAD -- .claude/skills/<name>/
     ```
   - If no local override, check repo vs main:
     ```bash
     git diff main -- .claude/skills/<name>/
     ```

3. Display status table:
   ```
   Repo Skills (.claude/skills/):

     <name>    LOCAL OVERRIDE (diverged)
               Local: ~/.claude/skills/<name>/
               Cloned from: <sha> (<date>)
               Repo HEAD: <sha> (<date>) - N commits ahead
               Local edits: <files changed>

     <name>    Up to date with main

     <name>    Branch has uncommitted skill changes
   ```

### Clone (clone <skill>)

1. Verify `.claude/skills/<skill>/` exists in the repo. If not, stop with error.
2. Check `~/.claude/skills/<skill>/` does NOT already exist. If it does, stop: "Local override already exists. Use `/skill-doctor revert <skill>` first."
3. Copy the entire skill directory:
   ```bash
   cp -r .claude/skills/<skill> ~/.claude/skills/<skill>
   ```
4. Get the current commit SHA for the skill path:
   ```bash
   git log -1 --format=%H -- .claude/skills/<skill>/
   ```
5. Get repo identity:
   ```bash
   gh repo view --json nameWithOwner -q .nameWithOwner
   ```
6. Create `~/.claude/skills/<skill>/.skill-origin.json` with this content:
   ```json
   {
     "source_repo": "<repo nameWithOwner from step 5>",
     "source_path": ".claude/skills/<skill>",
     "cloned_from_sha": "<SHA from step 4>",
     "cloned_at": "<current ISO 8601 timestamp>",
     "cloned_by": "<output of git config user.name>"
   }
   ```
7. Confirm to developer:
   ```
   Cloned .claude/skills/<skill> → ~/.claude/skills/<skill>/
   Local override is now active. Changes apply immediately across all branches.
   Others will NOT see these changes until you submit them.
   Use /skill-doctor --manage <skill> for options.
   ```

### Diff (diff <skill>)

1. Verify local override exists at `~/.claude/skills/<skill>/`. If not, stop with error.
2. Run diff excluding `.skill-origin.json`:
   ```bash
   diff -ru .claude/skills/<skill>/ ~/.claude/skills/<skill>/ --exclude='.skill-origin.json'
   ```
3. Show the diff to the developer with a summary of lines added/removed.

### Sync (sync <skill>)

1. Verify local override exists. Read `~/.claude/skills/<skill>/.skill-origin.json`.
2. Show what changed in repo since clone:
   ```bash
   git diff <cloned_from_sha>..HEAD -- .claude/skills/<skill>/
   ```
3. If no changes, report: "Repo version unchanged since clone. Nothing to sync."
4. If changes exist, ask the developer:
   - **Overwrite local** — replace local with repo version, keep `.skill-origin.json`, update `cloned_from_sha`
   - **Manual merge** — show both versions side-by-side, developer decides what to keep
   - **Cancel**

### Revert (revert <skill>)

1. Verify local override exists at `~/.claude/skills/<skill>/`. If not, stop with error.
2. Show what will be lost — run diff (same as `diff <skill>` above).
3. Ask for explicit confirmation: "This will delete `~/.claude/skills/<skill>/` and revert to the repo version. Confirm? [Yes/No]"
4. On confirm, delete the local override:
   ```bash
   rm -rf ~/.claude/skills/<skill>/
   ```
5. Confirm: "Reverted to repo version at `.claude/skills/<skill>/`"

---

## Mode: Diagnose

### Step 1: Identify the skill and issue

If a specific skill name was provided as argument, use that.

Otherwise, ask the developer: "Which skill had the issue?" and list available skills from `.claude/skills/` in the repo.

Check where the skill lives:
- `~/.claude/skills/<name>/` with `.skill-origin.json` → local override (can fix inline)
- `.claude/skills/<name>/` only → repo version (will need report, gist, or PR)

### Step 2: Classify the issue

Ask the developer to classify the issue:

| Type | Description |
|------|-------------|
| `invalid_command` | A shell command in the skill doesn't work (wrong syntax, missing tool, wrong flags) |
| `wrong_assumption` | The skill assumes something about the codebase or environment that isn't true |
| `stale_info` | Information in the skill is outdated (file paths, API endpoints, tool versions) |
| `other` | Something else went wrong |

### Step 3: Investigate and gather context

**Do NOT rush to send a report.** Before gathering report data, investigate the issue thoroughly:

1. **Read the full SKILL.md** for the affected skill. Understand what the skill was trying to do.
2. **Reproduce the issue** — run the failing command or recreate the condition. Confirm it actually fails and isn't a transient/environment issue.
3. **Identify the root cause** — determine WHY the skill is wrong, not just WHAT failed:
   - Wrong command? Check `--help` output or docs for the correct syntax.
   - Wrong assumption? Find what the skill assumed and what the codebase actually does.
   - Stale info? Identify when the referenced file/API/path changed and what replaced it.
4. **Determine the fix** — what specific change to SKILL.md would resolve the issue? A report without a suggested fix is incomplete.
5. **Check for related issues** — if you've found one problem, scan the rest of the skill for similar patterns. One consolidated report covering 3 related issues is far more valuable than 3 separate reports.

**Quality gate — a report MUST include:**
- A confirmed, reproducible problem (not speculation)
- The specific SKILL.md line(s) that are wrong
- Why they are wrong (root cause)
- A concrete suggested fix
- Verification that the fix works (if possible)

**If you cannot determine root cause or a fix**, tell the developer: "I've identified a symptom but can't determine the root cause yet. Would you like to investigate further before reporting?"

After investigation, collect the following (ask developer to confirm or supplement):

- **Skill name and source path** (repo or local override)
- **Issue type** from step 2
- **What happened:** Developer describes the problem in their own words
- **Root cause:** Why the skill is wrong (not just what failed)
- **Failed command** (if applicable): The exact command, exit code, stderr output
- **Expected behavior:** What should have happened instead
- **Suggested fix:** The specific change to SKILL.md that would resolve this
- **Related issues:** Any other problems found in the same skill during investigation
- **Relevant skill excerpt:** The section of SKILL.md that caused the issue
- **Environment:**
  ```bash
  echo "OS: $(uname -s) ($(uname -r))"
  echo "Shell: $SHELL"
  echo "gh version: $(gh --version | head -1)"
  ```

### Step 4: Check for duplicate reports

Before proceeding, check if this issue has already been reported in the skill's Discussion thread.

1. **Find the existing discussion thread** (if any):
   ```bash
   gh api graphql -f query='{ search(query: "repo:dotCMS/core category:\"Skill Feedback\" \"Feedback: <skill-name>\"", type: DISCUSSION, first: 1) { nodes { ... on Discussion { id title url comments(last: 20) { nodes { body author { login } createdAt url } } } } } }'
   ```

2. **Scan existing comments** for similar reports. Compare against your gathered context:
   - Same failed command or similar command pattern?
   - Same SKILL.md section referenced?
   - Same root cause described?
   - Same issue type?

3. **If a matching report exists**, tell the developer:

   ```
   This issue appears to have already been reported:
     <comment-url>
     By: <author> on <date>
     Summary: <first line of "What happened" from existing report>

   Options:
     - Upvote: Add a 👍 reaction to confirm you also hit this issue
     - Add context: Reply to the existing report with your additional details
     - Report anyway: Submit a new report if your issue is different enough
     - Cancel: Skip reporting
   ```

4. **If "Upvote" chosen**, add a thumbs-up reaction to the existing comment:
   ```bash
   gh api graphql -f query='mutation { addReaction(input: { subjectId: "<comment-node-id>", content: THUMBS_UP }) { reaction { content } } }'
   ```
   Then optionally add a short reply with the developer's environment info (different OS, gh version, etc.) to help maintainers understand the scope.

5. **If "Add context" chosen**, post a reply to the existing comment thread with the new details (scrubbed).

6. **If "Report anyway" or no match found**, continue to Step 5.

### Step 5: Route based on skill location

**If the skill is a local override** (`~/.claude/skills/<name>/` with `.skill-origin.json`):
1. Read the SKILL.md
2. Identify the problematic section based on gathered context
3. Propose a specific fix
4. Apply the fix directly using the Edit tool
5. Tell the developer: "Fixed locally. When ready, use `/skill-doctor pr <skill>` to submit upstream."
6. **STOP here** — no report needed for inline fixes.

**If the skill is a repo version** (`.claude/skills/<name>/` only):
Continue to Step 6 (scrub and report).

### Step 6: Scrub report content

Before showing the report to the developer, apply these redactions to ALL gathered context:

| Pattern | Replacement |
|---------|-------------|
| `sk-[a-zA-Z0-9]{20,}` | `[REDACTED_TOKEN]` |
| `ghp_[a-zA-Z0-9]{36}` | `[REDACTED_TOKEN]` |
| `ghs_[a-zA-Z0-9]{36}` | `[REDACTED_TOKEN]` |
| `Bearer [a-zA-Z0-9._-]+` | `Bearer [REDACTED_TOKEN]` |
| `xoxb-[a-zA-Z0-9-]+` | `[REDACTED_SLACK_TOKEN]` |
| `xoxp-[a-zA-Z0-9-]+` | `[REDACTED_SLACK_TOKEN]` |
| `/Users/<any-username>/` or `/home/<any-username>/` | `~/` |
| `$HOME` literal in output | `~/` |
| Email addresses (`[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}`) | `[EMAIL_REDACTED]` |
| IPv4 addresses (`\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b`) | `[IP_REDACTED]` |
| Lines from `.env` files | `[ENV_REDACTED]` |

**Note:** The reporter's identity is NOT scrubbed — reports are posted using the developer's own GitHub and Slack credentials. Only the report **body content** is scrubbed.

### Step 7: Developer review

Show the complete scrubbed report using the Report Template (see Delivery section).

Ask: "Review the report above. You can:"
- **Send** — post to GitHub Discussion + Slack notification
- **Edit** — modify the report content, then re-show for approval
- **Cancel** — discard the report entirely

Three explicit consent points: (1) agreed to capture at Step 1, (2) reviewed content here, (3) confirms send.

### Step 8: Deliver

If the developer chooses Send, follow the Delivery section below.

---

## Mode: Optimize

When `--optimize <skill>`:

### Step 1: Read the skill

Read the target skill's SKILL.md. Use local override version if it exists, otherwise the repo version.

### Step 2: Analyze against best practices

Check these criteria (from superpowers:writing-skills):

**Frontmatter checks:**
- `name` uses only letters, numbers, hyphens? (no parentheses or special chars)
- `description` starts with "Use when..."?
- `description` is under 500 characters?
- `description` describes triggering conditions only (NOT a workflow summary)?
- Total frontmatter under 1024 characters?

**Token efficiency:**
- Word count — check with `wc -w <skill-path>/SKILL.md`
  - Target: <500 words for standard skills, <200 for frequently-loaded
- Redundant content that could be cross-referenced to other skills?
- Verbose examples that could be compressed?
- Details that belong in `--help` output of scripts instead of SKILL.md?

**Structure quality:**
- Clear overview with core principle in 1-2 sentences?
- Flowcharts used only for non-obvious decision points?
- Quick reference table for scanning common operations?
- Common mistakes section?
- One excellent example (not multiple mediocre ones, not multi-language)?

**Staleness (file/command verification):**
- For each file path referenced in the skill, check:
  ```bash
  test -f <referenced-path> && echo "EXISTS" || echo "MISSING: <path>"
  ```
- For each shell command referenced, verify the command exists and flags are valid
- Check API endpoints or tool flags are current

### Step 3: Present findings

Show a structured report:
- **Score:** e.g., "7/12 checks passed"
- **Issues found:** Each with line reference in SKILL.md and severity (critical/warning/info)
- **Suggested fixes:** Concrete, actionable (not vague "improve this")

### Step 4: Apply fixes

- **Local override:** Offer to apply each fix directly using Edit tool
- **Repo version:** Offer to clone first (`/skill-doctor clone <skill>`), then apply fixes to the local copy

---

## Delivery

### Prerequisites Check

**Before ANY delivery action** (report, gist, pr), run these checks and stop with guidance if any fail:

#### 1. GitHub CLI Authentication

```bash
gh auth status 2>&1
```

**If exit code is non-zero** (not authenticated), tell the developer:

```
GitHub CLI is not authenticated. Delivery requires `gh` to be logged in.

To fix:
  gh auth login

Choose:
  - Account: GitHub.com
  - Protocol: HTTPS
  - Authentication: Login with a web browser

This is a one-time setup. After authenticating, re-run your /skill-doctor command.
```

**If authenticated but missing scopes**, check the output for required scopes. The `gh api graphql` calls for Discussions require the `read:discussion` and `write:discussion` scopes. If the output shows these are missing:

```
Your GitHub token is missing required scopes for Discussion access.

To fix:
  gh auth refresh -s read:discussion -s write:discussion

This adds the scopes to your existing token. Re-run your /skill-doctor command after.
```

#### 2. GitHub Repo Access

```bash
gh api repos/dotCMS/core --jq '.permissions.push' 2>&1
```

**If this fails or returns `false`**, the developer may not have write access:

```
You don't appear to have write access to dotCMS/core.

Delivery options that still work:
  - /skill-doctor gist <skill>  (creates a personal gist — no repo access needed)
  - /skill-doctor diff <skill>  (shows changes locally)

For Discussion and PR delivery, you need write access to dotCMS/core.
Contact a repo admin or open a GitHub access request.
```

#### 3. Slack MCP Connection (for Slack notification only)

After GitHub delivery succeeds, before attempting Slack notification, test connectivity by searching for the channel:

Use `slack_search_channels` with query "log-skill-feedback".

**If the tool call fails or returns an error** (MCP server not running, not authenticated), tell the developer:

```
Slack MCP is not connected. The GitHub Discussion was posted successfully,
but the Slack notification could not be sent.

The report is available at: <discussion-comment-url>

To set up Slack MCP for future notifications:
  1. Ensure the Slack MCP server is configured in your Claude Code settings.
     Check ~/.claude/settings.json for an "mcpServers" entry with "slack".

  2. If missing, add Slack MCP to your settings:
     - For the Anthropic first-party Slack integration, add it via:
       claude mcp add slack

     - For a local Slack MCP server, add to ~/.claude/settings.json:
       {
         "mcpServers": {
           "slack": {
             "type": "sse",
             "url": "http://localhost:3001/sse"
           }
         }
       }

  3. Restart Claude Code after making changes to settings.json.

  4. If the server is configured but failing, check that it's running:
     - For SSE servers: verify the URL is reachable
     - Check Claude Code logs for MCP connection errors

This is optional — GitHub Discussion delivery works independently of Slack.
```

**If the channel search succeeds but `#log-skill-feedback` is not found:**

```
Slack is connected but #log-skill-feedback channel doesn't exist.

The report was posted to GitHub Discussions: <discussion-comment-url>

To enable Slack notifications:
  - Ask a Slack workspace admin to create #log-skill-feedback
  - Or specify an alternative channel when prompted
```

#### Check Order

Run checks in this order — stop at first failure for the required service:

1. **For `report`:** GitHub auth → repo access → post Discussion → Slack (optional, warn but don't fail)
2. **For `gist`:** GitHub auth only (no repo access needed) → create gist → Slack (optional)
3. **For `pr`:** GitHub auth → repo access → create PR (no Slack needed)

**Key principle:** GitHub Discussion is the primary delivery. Slack is a notification layer. If Slack fails, the report still succeeds — inform the developer and provide the Discussion URL.

### Report Template

```markdown
## Skill Feedback Report

**Skill:** `<name>` (<source-path>)
**Type:** `<issue_type>`
**Date:** <YYYY-MM-DD>
**Repo:** <repo-name> @ <short-sha>
**Skill version:** `<skill-commit-sha>` (<commit-date>) — [view commit](<github-commit-url>)
**Status:** Open

### What happened
<developer description>

### Root cause
<why the skill is wrong — not just what failed, but the underlying reason>

### Failed command
<command>

Exit code: <code>
Stderr: <stderr output, truncated to 500 chars>

### Expected behavior
<what should have happened>

### Suggested fix
<specific change to SKILL.md that would resolve this, with line reference>

### Related issues
<any other problems found in the same skill during investigation, or "None">

### Relevant skill excerpt
> <quoted section from SKILL.md>

### Environment
- OS: <uname output>
- Shell: <shell>
- gh version: <version>
```

**Skill version** is determined by:
```bash
git log -1 --format="%H %ai" -- .claude/skills/<skill>/
```
This records the exact commit that introduced or last changed the skill, so maintainers can identify when the issue was introduced and what changed.

### GitHub Discussion Delivery

1. **Get the "Skill Feedback" category ID** (one-time lookup):
   ```bash
   gh api graphql -f query='{ repository(owner: "dotCMS", name: "core") { discussionCategories(first: 20) { nodes { id name } } } }'
   ```
   Extract the `id` where `name` is "Skill Feedback". If the category doesn't exist, tell the developer: "The 'Skill Feedback' discussion category needs to be created on dotCMS/core by a repo admin."

2. **Search for existing discussion** for this skill:
   ```bash
   gh api graphql -f query='{ search(query: "repo:dotCMS/core category:\"Skill Feedback\" \"Feedback: <skill-name>\"", type: DISCUSSION, first: 1) { nodes { ... on Discussion { id title url } } } }'
   ```

3. **If no discussion exists**, create one:
   ```bash
   gh api graphql -f query='mutation { createDiscussion(input: { repositoryId: "<repo-id>", categoryId: "<category-id>", title: "Feedback: <skill-name>", body: "Feedback thread for the `<skill-name>` skill in `.claude/skills/<skill-name>/`.\n\nReports are appended as comments below." }) { discussion { id url } } }'
   ```
   Get the repo ID with: `gh api graphql -f query='{ repository(owner: "dotCMS", name: "core") { id } }'`

4. **Add the report as a comment**:
   ```bash
   gh api graphql -f query='mutation { addDiscussionComment(input: { discussionId: "<discussion-id>", body: "<scrubbed-report-markdown>" }) { comment { url } } }'
   ```

5. Capture the comment URL for the Slack notification.

### Slack Delivery

After the GitHub Discussion comment is posted:

1. Find the `#log-skill-feedback` channel ID using `slack_search_channels` tool with query "log-skill-feedback"
2. Post a message using `slack_send_message` tool:
   - **channel_id:** The channel ID from step 1
   - **message:** Format as:
     ```
     New skill feedback: **<skill-name>**
     Type: `<issue_type>` — <one-line summary>
     Full report: <discussion-comment-url>
     ```

If `#log-skill-feedback` channel doesn't exist, tell the developer: "No #log-skill-feedback channel found. Create one or specify an alternative channel."

### Gist Delivery (gist <skill>)

1. Verify local override exists with changes (run diff first).
2. Generate diff excluding tracking file:
   ```bash
   diff -ru .claude/skills/<skill>/ ~/.claude/skills/<skill>/ --exclude='.skill-origin.json' > /tmp/skill-doctor-diff.patch
   ```
3. Create gist:
   ```bash
   gh gist create /tmp/skill-doctor-diff.patch --desc "skill-doctor: proposed changes to <skill-name>" --public
   ```
4. Post gist URL to Slack `#log-skill-feedback` using `slack_send_message`:
   ```
   Skill changes shared: **<skill-name>**
   Gist with proposed changes: <gist-url>
   ```
5. Clean up: `rm /tmp/skill-doctor-diff.patch`

### PR Delivery (pr <skill>)

1. Verify local override exists with changes.
2. Show the diff to the developer for final review.
3. Ask for confirmation before creating the PR.
4. Create a branch:
   ```bash
   git checkout -b skill-doctor/<skill>-update
   ```
5. Copy local override back to repo (excluding tracking file):
   ```bash
   rsync -av --exclude='.skill-origin.json' ~/.claude/skills/<skill>/ .claude/skills/<skill>/
   ```
6. Stage and commit:
   ```bash
   git add .claude/skills/<skill>/
   git commit -m "fix(skills): update <skill> skill based on feedback"
   ```
7. Push and create PR:
   ```bash
   git push -u origin skill-doctor/<skill>-update
   gh pr create --title "fix(skills): update <skill> skill" --body "$(cat <<'EOF'
   ## Summary
   Updates to the `<skill>` skill based on developer feedback.

   ## Changes
   <diff summary>

   ## Test plan
   - [ ] Invoke the skill and verify the fix
   - [ ] Check that the skill still works for its primary use case
   EOF
   )"
   ```
8. Show the PR URL to the developer.
9. **Link PR to open reports:** If the skill has a Discussion thread with open reports, add a comment linking the PR:
   ```
   Fix PR created: <pr-url>
   This PR addresses the following reports in this thread:
   - <comment-url-1> (invalid_command — <summary>)
   - <comment-url-2> (stale_info — <summary>)
   ```
10. Offer to revert local override now that changes are in a PR.

---

## Mode: Resolve

When `resolve <skill>`:

This mode is used after a fix PR has been merged to clean up the Discussion thread.

### Step 1: Find the Discussion thread

```bash
gh api graphql -f query='{ search(query: "repo:dotCMS/core category:\"Skill Feedback\" \"Feedback: <skill-name>\"", type: DISCUSSION, first: 1) { nodes { ... on Discussion { id title url comments(last: 50) { nodes { id body author { login } createdAt url } } } } } }'
```

If no thread exists, stop: "No feedback thread found for `<skill-name>`."

### Step 2: List open reports

Scan comments for reports that contain `**Status:** Open`. Display them:

```
Open reports for <skill-name>:

  1. <date> by <author> — <issue_type>: <first line of "What happened">
     Skill version: <commit-sha>
     <comment-url>

  2. <date> by <author> — <issue_type>: <first line of "What happened">
     Skill version: <commit-sha>
     <comment-url>

  No open reports.
```

If no open reports, stop: "All reports for `<skill-name>` are already resolved."

### Step 3: Identify the fix

Ask the developer to provide the fix reference:

- **PR number or URL** — a merged PR that fixes the reported issues
- **Commit SHA** — a specific commit that contains the fix

Verify the PR/commit exists:
```bash
gh pr view <pr-number> --json state,mergedAt,url,title
```

If the PR is not merged, warn: "PR #<number> is not yet merged. Resolve after it merges?"

### Step 4: Select reports to resolve

Ask the developer which open reports are addressed by this fix:
- **All** — resolve all open reports
- **Select** — choose specific reports by number

### Step 5: Mark reports as resolved

For each selected report, reply to the comment with a resolution note:

```bash
gh api graphql -f query='mutation { addDiscussionComment(input: { discussionId: "<discussion-id>", body: "**Resolved** in <pr-url> (merged <merge-date>).\n\nThis report has been addressed. The fix is available from the next pull of main.", replyToId: "<comment-node-id>" }) { comment { url } } }'
```

### Step 6: Post Slack summary

Post a resolution summary to `#log-skill-feedback`:

```
Skill feedback resolved: **<skill-name>**
Fix: <pr-url> (<pr-title>)
Reports resolved: <count>
Thread: <discussion-url>
```

### Step 7: Offer cleanup

If ALL reports in the thread are now resolved, ask:

```
All reports for <skill-name> are resolved. Options:
  - Lock thread — prevent new comments (maintainer can unlock later)
  - Leave open — keep the thread open for future reports
```

If "Lock thread" chosen:
```bash
gh api graphql -f query='mutation { lockLockable(input: { lockableId: "<discussion-id>", lockReason: RESOLVED }) { lockedRecord { locked } } }'
```

---

## Override Warning

**IMPORTANT:** Whenever ANY skill is about to be used and a local override exists for it, check first:

```bash
test -f ~/.claude/skills/<skill-name>/.skill-origin.json
```

If the file exists, output this warning BEFORE following the skill content:

```
SKILL OVERRIDE ACTIVE: <name>
  Using: ~/.claude/skills/<name>/ (local)
  Instead of: .claude/skills/<name>/ (repo)
  Cloned from: <sha> | Repo is N commits ahead
  Run /skill-doctor --manage <name> for options
```

To calculate "N commits ahead":
```bash
git log --oneline <cloned_from_sha>..HEAD -- .claude/skills/<name>/ | wc -l
```