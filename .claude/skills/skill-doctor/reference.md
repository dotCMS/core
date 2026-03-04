# Skill Doctor — Detailed Reference

Read the relevant section below based on the mode determined by SKILL.md routing.

---

## Manage Mode

### Status (--manage with no skill name)

1. List all skills in `.claude/skills/` in the current repo:
   ```bash
   ls -d .claude/skills/*/
   ```

2. For each skill, check:
   - Does `~/.claude/skills/<name>/` exist? -> local override active
   - If local override exists, read `~/.claude/skills/<name>/.skill-origin.json`
   - Compare `cloned_from_sha` against current repo HEAD for that path:
     ```bash
     git log --oneline <cloned_from_sha>..HEAD -- .claude/skills/<name>/
     ```
   - If no local override, check repo vs main (use `HEAD` if main ref doesn't exist):
     ```bash
     git rev-parse main &>/dev/null && git diff main -- .claude/skills/<name>/ || echo "No main branch — skipping branch diff"
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

1. Verify `.claude/skills/<skill>/` exists in the repo:
   ```bash
   test -d .claude/skills/<skill>/ || echo "ERROR: No repo skill named '<skill>'"
   ```
   If not found, list available skills and stop.

2. Check `~/.claude/skills/<skill>/` does NOT already exist:
   - If directory exists AND `.skill-origin.json` exists: "Local override already exists. Use `/skill-doctor revert <skill>` first."
   - If directory exists WITHOUT `.skill-origin.json`: "Directory `~/.claude/skills/<skill>/` exists but is not a tracked clone. It may have been manually created. Options: (a) manually remove it with `rm -rf ~/.claude/skills/<skill>/`, (b) back it up first, (c) cancel."

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
   If `gh` is not installed or not authenticated, fall back to parsing the git remote:
   ```bash
   git remote get-url origin | sed 's|.*github.com[:/]||;s|\.git$||'
   ```

6. Create `~/.claude/skills/<skill>/.skill-origin.json`:
   ```json
   {
     "source_repo": "<repo nameWithOwner>",
     "source_path": ".claude/skills/<skill>",
     "cloned_from_sha": "<SHA from step 4>",
     "cloned_at": "<current ISO 8601 timestamp>",
     "cloned_by": "<output of git config user.name>"
   }
   ```

7. Confirm to developer:
   ```
   Cloned .claude/skills/<skill> -> ~/.claude/skills/<skill>/
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
3. If diff output is empty: "Local override is identical to repo version. No changes to show. You may want to `/skill-doctor revert <skill>` or `/skill-doctor sync <skill>`."
4. If diff has content, show it with a summary of lines added/removed.

### Sync (sync <skill>)

1. Verify local override exists. Read `~/.claude/skills/<skill>/.skill-origin.json`.
2. Show what changed in repo since clone:
   ```bash
   git diff <cloned_from_sha>..HEAD -- .claude/skills/<skill>/
   ```
3. If no changes: "Repo version unchanged since clone. Nothing to sync."
4. If changes exist, ask the developer:
   - **Overwrite local** -- replace local with repo version, keep `.skill-origin.json`, update `cloned_from_sha`
   - **Manual merge** -- see procedure below
   - **Cancel**

**Manual merge procedure:**
1. Show the repo diff (what changed upstream since clone)
2. For each changed file, show the repo version and the local version side-by-side using diff
3. Ask the developer for each conflict: "Keep local change, take repo change, or edit manually?"
4. Apply the developer's choices using the Edit tool
5. Update `cloned_from_sha` in `.skill-origin.json` to current repo HEAD:
   ```bash
   git log -1 --format=%H -- .claude/skills/<skill>/
   ```

### Revert (revert <skill>)

1. Verify local override exists at `~/.claude/skills/<skill>/`. If not, stop with error.
2. Show what will be lost -- run diff (same as `diff <skill>` above).
3. Warn the developer clearly:
   ```
   WARNING: This will PERMANENTLY DELETE ~/.claude/skills/<skill>/
   This cannot be undone. All local edits will be lost.
   The repo version at .claude/skills/<skill>/ will take effect immediately.
   ```
4. Ask for explicit confirmation: "Type the skill name to confirm deletion:"
5. On confirm:
   ```bash
   rm -rf ~/.claude/skills/<skill>/
   ```
6. Confirm: "Reverted to repo version at `.claude/skills/<skill>/`"

---

## Diagnose Mode

### Step 1: Identify the skill and issue

If a specific skill name was provided, use that. Otherwise, ask the developer.

**Validate the skill exists:**
```bash
test -d .claude/skills/<name>/ || test -d ~/.claude/skills/<name>/
```
If neither exists, list available skills and stop: "No skill named `<name>` found."

Check where it lives:
- `~/.claude/skills/<name>/` with `.skill-origin.json` -> local override (can fix inline)
- `.claude/skills/<name>/` only -> repo version (will need report, gist, or PR)

### Step 2: Classify the issue

Ask the developer:

| Type | Description |
|------|-------------|
| `invalid_command` | Shell command doesn't work (wrong syntax, missing tool, wrong flags) |
| `wrong_assumption` | Skill assumes something about the codebase/environment that isn't true |
| `stale_info` | Information is outdated (file paths, API endpoints, tool versions) |
| `other` | Something else went wrong |

### Step 3: Investigate and gather context

**Do NOT rush to send a report.** Investigate thoroughly first.

1. **Read the SKILL.md** for the affected skill. For large skills (300+ lines), search for the failing command/pattern first:
   ```bash
   grep -n "<failing-command-or-keyword>" <skill-path>/SKILL.md
   ```
   Then read that section plus surrounding context.

2. **Reproduce the issue** -- run the failing command or recreate the condition. Confirm it fails consistently, not just transiently.

3. **Identify the root cause** -- WHY the skill is wrong:
   - Wrong command? Check `--help` output or docs for correct syntax.
   - Wrong assumption? Find what the skill assumed vs what the codebase actually does.
   - Stale info? Identify when the referenced file/API/path changed.

4. **Determine the fix** -- what specific SKILL.md change resolves it? A report without a suggested fix is incomplete.

5. **Check for related issues** -- scan for similar patterns in the rest of the skill. One consolidated report beats multiple separate ones.

**Quality gate -- a report MUST include:**
- Confirmed reproducible problem (not speculation)
- Specific SKILL.md line(s) that are wrong
- Root cause
- Concrete suggested fix
- Verification that the fix works (if possible)

**If root cause is unclear:** Tell the developer: "I've identified a symptom but can't determine the root cause. Investigate further before reporting?"

Collect (ask developer to confirm/supplement):
- Skill name and source path
- Issue type
- What happened (developer's own words)
- Root cause
- Failed command + exit code + stderr (if applicable)
- Expected behavior
- Suggested fix (specific SKILL.md change with line reference)
- Related issues found during investigation
- Relevant skill excerpt
- Environment: `uname -s`, `uname -r`, `$SHELL`, `gh --version`

### Step 4: Check for duplicate reports

Search the skill's Discussion thread for existing reports:

```bash
gh api graphql -f query='{ search(query: "repo:dotCMS/core category:\"Skill Feedback\" \"Feedback: <skill-name>\"", type: DISCUSSION, first: 1) { nodes { ... on Discussion { id title url comments(last: 20) { nodes { id body author { login } createdAt url } } } } } }'
```

**If no Discussion thread exists:** This is the first report for this skill. Skip duplicate check -- continue to Step 5.

**If thread exists but has no comments:** Same -- continue to Step 5.

**If comments exist**, scan for similar reports (same command, same section, same root cause). If match found:

```
This issue appears to have already been reported:
  <comment-url>
  By: <author> on <date>
  Summary: <first line of "What happened">

Options:
  - Upvote: Add a thumbs-up reaction to confirm you also hit this
  - Add context: Reply with your additional details
  - Report anyway: Submit new report if your issue is different enough
  - Cancel
```

**Upvote** uses GraphQL `addReaction` mutation with `THUMBS_UP` content on the comment node ID.

### Step 5: Route based on skill location

**Local override** (`~/.claude/skills/<name>/` with `.skill-origin.json`):
1. Read the SKILL.md, identify the problematic section
2. Propose a specific fix
3. Apply directly using Edit tool
4. Tell developer: "Fixed locally. Use `/skill-doctor pr <skill>` to submit upstream."
5. **STOP** -- no report needed for inline fixes.

**Repo version** (`.claude/skills/<name>/` only):
Offer the developer a choice:
- **Clone and fix** (recommended): Clone to local override, fix inline, then PR
- **Report only**: Continue to Step 6 to submit feedback for maintainers
- **Cancel**

If "Clone and fix": run the clone procedure, then fix inline as above.
If "Report only": continue to Step 6.

### Step 6: Scrub report content

Apply these redactions to ALL gathered context:

| Pattern | Replacement |
|---------|-------------|
| `sk-[a-zA-Z0-9]{20,}` | `[REDACTED_TOKEN]` |
| `ghp_[a-zA-Z0-9]{36}` | `[REDACTED_TOKEN]` |
| `ghs_[a-zA-Z0-9]{36}` | `[REDACTED_TOKEN]` |
| `Bearer [a-zA-Z0-9._-]+` | `Bearer [REDACTED_TOKEN]` |
| `xoxb-[a-zA-Z0-9-]+` | `[REDACTED_SLACK_TOKEN]` |
| `xoxp-[a-zA-Z0-9-]+` | `[REDACTED_SLACK_TOKEN]` |
| `/Users/<any-username>/` or `/home/<any-username>/` | `~/` |
| `C:\Users\<any-username>\` or `C:\\Users\\<any-username>\\` | `~/` |
| `$HOME` literal in output | `~/` |
| Email addresses | `[EMAIL_REDACTED]` |
| IPv4 addresses (`\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b`) | `[IP_REDACTED]` |
| Lines from `.env` files | `[ENV_REDACTED]` |

Reporter identity is NOT scrubbed -- reports use the developer's own credentials.

### Step 7: Developer review

Show the scrubbed report using the Report Template below. Ask:
- **Send** -- post to GitHub Discussion + Slack
- **Edit** -- developer specifies which sections to change; after edits, re-run scrubbing and re-show
- **Cancel** -- discard

### Step 8: Deliver

If developer approves, follow the Delivery procedures below.

---

## Report Template

```markdown
## Skill Feedback Report

**Skill:** `<name>` (<source-path>)
**Type:** `<issue_type>`
**Date:** <YYYY-MM-DD>
**Repo:** <repo-name> @ <short-sha>
**Skill version:** `<skill-commit-sha>` (<commit-date>) -- [view commit](<github-commit-url>)
**Status:** Open

### What happened
<developer description>

### Root cause
<why the skill is wrong>

### Failed command
<command>

Exit code: <code>
Stderr: <stderr, truncated to 500 chars>

### Expected behavior
<what should have happened>

### Suggested fix
<specific SKILL.md change with line reference>

### Related issues
<other problems found, or "None">

### Relevant skill excerpt
> <quoted section from SKILL.md>

### Environment
- OS: <uname -s> (<uname -r>)
- Shell: <$SHELL>
- gh version: <version>
```

**Skill version** is determined by:
```bash
git log -1 --format="%H %ai" -- .claude/skills/<skill>/
```

---

## Delivery Prerequisites

**Before ANY delivery action**, check in this order -- stop at first failure:

### GitHub CLI Authentication
```bash
gh auth status 2>&1
```
If not authenticated: guide developer to run `gh auth login`.
If missing Discussion scopes: `gh auth refresh -s read:discussion -s write:discussion`

### GitHub Repo Access (for report and pr only)
```bash
gh api repos/dotCMS/core --jq '.permissions.push' 2>&1
```
If no write access, suggest `gist` or `diff` instead.

### Slack MCP (optional, for notifications only)
Test by searching for `#log-skill-feedback` channel. If Slack fails, the GitHub delivery still succeeds -- inform the developer and provide the Discussion URL. Slack is a notification layer, not a hard requirement.

---

## GitHub Discussion Delivery

1. **Get "Skill Feedback" category ID:**
   ```bash
   gh api graphql -f query='{ repository(owner: "dotCMS", name: "core") { discussionCategories(first: 20) { nodes { id name } } } }'
   ```
   If category doesn't exist: "The 'Skill Feedback' category needs to be created on dotCMS/core by a repo admin."

2. **Search for existing discussion:**
   ```bash
   gh api graphql -f query='{ search(query: "repo:dotCMS/core category:\"Skill Feedback\" \"Feedback: <skill-name>\"", type: DISCUSSION, first: 1) { nodes { ... on Discussion { id title url } } } }'
   ```

3. **If no discussion**, create one using `createDiscussion` mutation with repo ID (`R_kgDOADjo3Q`) and category ID.

4. **Add the report as a comment** using `addDiscussionComment` mutation.

5. Capture comment URL for Slack notification.

## Slack Delivery

After GitHub Discussion is posted, find `#log-skill-feedback` (channel ID: `C0AJ9MGBMH9`) and post:
```
New skill feedback: **<skill-name>**
Type: `<issue_type>` -- <one-line summary>
Full report: <discussion-comment-url>
```

## Gist Delivery (gist <skill>)

1. Verify local override exists with changes.
2. Generate diff: `diff -ru .claude/skills/<skill>/ ~/.claude/skills/<skill>/ --exclude='.skill-origin.json'`
3. Create gist: `gh gist create <diff-file> --desc "skill-doctor: proposed changes to <skill-name>"`
4. Post gist URL to Slack.

## PR Delivery (pr <skill>)

1. Verify local override exists with changes. Show diff for final review.
2. Create branch: `git checkout -b skill-doctor/<skill>-update`
3. Copy local changes back: `rsync -av --exclude='.skill-origin.json' ~/.claude/skills/<skill>/ .claude/skills/<skill>/`
4. Stage, commit, push, create PR linking to open Discussion reports.
5. Offer to revert local override.

---

## Resolve Mode (resolve <skill>)

Used after a fix PR is merged to clean up the Discussion thread.

1. **Find Discussion thread** using the same search query as Diagnose Step 4, with `comments(last: 50)`.
2. **List open reports** -- scan comments for `**Status:** Open`.
3. **Identify the fix** -- ask for PR number/URL, verify it's merged via `gh pr view`.
4. **Select reports to resolve** -- all or specific ones.
5. **Mark resolved** -- reply to each selected comment:
   ```
   **Resolved** in <pr-url> (merged <date>).
   This report has been addressed. The fix is available from the next pull of main.
   ```
6. **Post Slack summary** to `#log-skill-feedback`.
7. **Offer cleanup** -- if all reports resolved, offer to lock thread via `lockLockable` mutation.

---

## Override Warning

Whenever ANY skill is about to be used and a local override exists, check:
```bash
test -f ~/.claude/skills/<skill-name>/.skill-origin.json
```

If exists, output BEFORE the skill content:
```
SKILL OVERRIDE ACTIVE: <name>
  Using: ~/.claude/skills/<name>/ (local)
  Instead of: .claude/skills/<name>/ (repo)
  Cloned from: <sha> | Repo is N commits ahead
  Run /skill-doctor --manage <name> for options
```