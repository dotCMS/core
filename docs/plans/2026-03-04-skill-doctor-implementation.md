# skill-doctor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a skill that diagnoses issues with repo-local Claude Code skills, manages local override clones with tracking, and delivers feedback to GitHub Discussions + Slack.

**Architecture:** A single skill (`~/.claude/skills/skill-doctor/`) with three modes (diagnose, optimize, manage). Shell scripts handle override lifecycle operations (clone, diff, sync, revert). A PostToolUse hook script detects Bash failures after skill invocations and injects a prompt to capture feedback. PII scrubbing is done inline via sed/regex before developer review.

**Tech Stack:** SKILL.md (Claude Code skill format), POSIX shell scripts, `gh` CLI (GraphQL for Discussions), Slack MCP (`slack_send_message`), `jq` for JSON, `git` for staleness detection.

**Design doc:** `docs/plans/2026-03-04-skill-doctor-design.md`

---

## Task 1: Scaffold Skill Directory and SKILL.md Skeleton

**Files:**
- Create: `~/.claude/skills/skill-doctor/SKILL.md`

**Step 1: Create the skill directory**

```bash
mkdir -p ~/.claude/skills/skill-doctor
```

**Step 2: Write the SKILL.md skeleton with frontmatter and mode routing**

Create `~/.claude/skills/skill-doctor/SKILL.md` with:

```markdown
---
name: skill-doctor
description: Use when a repo skill fails, produces errors, gives wrong instructions, or references stale information. Also use to manage local skill overrides, check skill staleness, or optimize skill token usage and quality.
---

# Skill Doctor

Diagnose issues with repository skills, manage local overrides, and optimize skill quality.

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

Route to the appropriate section below based on the parsed mode.

## Mode: Manage

### Status (all skills)

When `--manage` with no skill name:

1. List all skills in `.claude/skills/` in the current repo
2. For each skill, check:
   - Does `~/.claude/skills/<name>/` exist? (local override)
   - If local override exists, read `~/.claude/skills/<name>/.skill-origin.json`
   - Compare `cloned_from_sha` against current repo HEAD for that path:
     ```bash
     git log --oneline <cloned_from_sha>..HEAD -- .claude/skills/<name>/
     ```
   - Check repo vs main: `git diff main -- .claude/skills/<name>/`
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

### Clone

When `clone <skill>`:

1. Verify `.claude/skills/<skill>/` exists in the repo
2. Check `~/.claude/skills/<skill>/` does NOT already exist (abort if it does — tell user to revert first)
3. Copy the entire skill directory:
   ```bash
   cp -r .claude/skills/<skill> ~/.claude/skills/<skill>
   ```
4. Get the current commit SHA for the skill path:
   ```bash
   git log -1 --format=%H -- .claude/skills/<skill>/
   ```
5. Create `~/.claude/skills/<skill>/.skill-origin.json`:
   ```json
   {
     "source_repo": "<output of gh repo view --json nameWithOwner -q .nameWithOwner>",
     "source_path": ".claude/skills/<skill>",
     "cloned_from_sha": "<sha from step 4>",
     "cloned_at": "<ISO 8601 timestamp>",
     "cloned_by": "<output of git config user.name>"
   }
   ```
6. Confirm to developer:
   ```
   Cloned .claude/skills/<skill> → ~/.claude/skills/<skill>/
   Local override is now active. Changes apply immediately across all branches.
   Others will NOT see these changes until you submit them.
   Use /skill-doctor --manage <skill> for options.
   ```

### Diff

When `diff <skill>`:

1. Verify local override exists at `~/.claude/skills/<skill>/`
2. Run diff excluding `.skill-origin.json`:
   ```bash
   diff -ru .claude/skills/<skill>/ ~/.claude/skills/<skill>/ --exclude='.skill-origin.json'
   ```
3. Show the diff to the developer with a summary of changes

### Sync

When `sync <skill>`:

1. Read `~/.claude/skills/<skill>/.skill-origin.json`
2. Show what changed in repo since clone:
   ```bash
   git diff <cloned_from_sha>..HEAD -- .claude/skills/<skill>/
   ```
3. Ask developer how to proceed:
   - **Overwrite local** — replace local with repo version (keeps `.skill-origin.json`, updates SHA)
   - **Manual merge** — show both versions, developer decides
   - **Cancel**

### Revert

When `revert <skill>`:

1. Verify local override exists
2. Show what will be lost (diff local vs repo)
3. Ask for confirmation: "This will delete ~/.claude/skills/<skill>/ and revert to the repo version. Confirm?"
4. On confirm:
   ```bash
   rm -rf ~/.claude/skills/<skill>/
   ```
5. Confirm: "Reverted to repo version at .claude/skills/<skill>/"

## Mode: Diagnose

### Step 1: Identify the skill and issue

If a specific skill name was provided, use that. Otherwise, ask the developer which skill had the issue.

Check if the skill exists:
- In `~/.claude/skills/<name>/` (local override — can fix inline)
- In `.claude/skills/<name>/` (repo version — will need report, gist, or PR)

### Step 2: Classify the issue

Ask the developer to classify:

| Type | Description |
|------|-------------|
| `invalid_command` | A shell command in the skill doesn't work (wrong syntax, missing tool, wrong flags) |
| `wrong_assumption` | The skill assumes something about the codebase or environment that isn't true |
| `stale_info` | Information in the skill is outdated (file paths, API endpoints, tool versions) |
| `other` | Something else went wrong |

### Step 3: Gather context

Collect the following (ask developer to confirm/supplement):

- **Skill name and source path** (repo or local override)
- **Issue type** from step 2
- **What happened:** Developer describes the problem
- **Failed command** (if applicable): The command, exit code, stderr output
- **Expected behavior:** What should have happened
- **Relevant skill excerpt:** The section of SKILL.md that caused the issue
- **Environment:**
  ```bash
  echo "OS: $(uname -s) ($(uname -r))"
  echo "Shell: $SHELL"
  echo "gh version: $(gh --version | head -1)"
  ```

### Step 4: For local overrides — fix inline

If the skill is a local override (`~/.claude/skills/<name>/`):

1. Read the SKILL.md
2. Identify the problematic section
3. Propose a fix
4. Apply the fix directly using Edit tool
5. Tell the developer: "Fixed locally. When ready, use `/skill-doctor pr <skill>` to submit upstream."

Skip steps 5-7 (no report needed for inline fixes).

### Step 5: Scrub content

Apply these replacements to all gathered context before showing the report:

- Strings matching `sk-[a-zA-Z0-9]{20,}` → `[REDACTED_TOKEN]`
- Strings matching `ghp_[a-zA-Z0-9]{36}` → `[REDACTED_TOKEN]`
- Strings matching `ghs_[a-zA-Z0-9]{36}` → `[REDACTED_TOKEN]`
- Strings matching `Bearer [a-zA-Z0-9._-]+` → `Bearer [REDACTED_TOKEN]`
- Strings matching `xoxb-[a-zA-Z0-9-]+` → `[REDACTED_SLACK_TOKEN]`
- Strings matching `xoxp-[a-zA-Z0-9-]+` → `[REDACTED_SLACK_TOKEN]`
- Home directory path `$HOME` or `/Users/<username>` or `/home/<username>` → `~/`
- Strings matching email pattern `[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}` → `[EMAIL_REDACTED]`
- Strings matching IP pattern `\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b` → `[IP_REDACTED]`
- Any line from `.env` files → `[ENV_REDACTED]`

### Step 6: Developer review

Show the complete scrubbed report to the developer using the format from the Report Template section below.

Ask: "Review the report above. You can:"
- **Send** — post to GitHub Discussion + Slack
- **Edit** — modify the report content before sending
- **Cancel** — discard the report

### Step 7: Deliver

If the developer approves, follow the Delivery section below.

## Mode: Optimize

When `--optimize <skill>`:

### Step 1: Read the skill

Read the target skill's SKILL.md (local override if exists, otherwise repo version).

### Step 2: Analyze against best practices

Check these criteria (from superpowers:writing-skills):

**Frontmatter:**
- [ ] `name` uses only letters, numbers, hyphens?
- [ ] `description` starts with "Use when..."?
- [ ] `description` is under 500 characters?
- [ ] `description` describes triggering conditions only (not workflow summary)?
- [ ] Total frontmatter under 1024 characters?

**Token efficiency:**
- [ ] Word count (target: <500 words for frequently-loaded, <200 for getting-started)
  ```bash
  wc -w <skill-path>/SKILL.md
  ```
- [ ] Redundant content that could be cross-referenced to other skills?
- [ ] Verbose examples that could be compressed?
- [ ] Details that could be moved to `--help` output of scripts?

**Structure:**
- [ ] Clear overview with core principle in 1-2 sentences?
- [ ] Flowcharts only for non-obvious decision points?
- [ ] Quick reference table for scanning?
- [ ] Common mistakes section?
- [ ] One excellent example (not multi-language)?

**Staleness:**
- [ ] References to files that exist in the codebase?
  For each file path referenced in the skill, check:
  ```bash
  test -f <referenced-path> && echo "EXISTS" || echo "MISSING: <path>"
  ```
- [ ] Commands that work on the current system?
- [ ] API endpoints or tool flags that are current?

### Step 3: Present findings

Show a structured report with:
- Overall score (e.g., "7/12 checks passed")
- Specific issues found with line references
- Suggested fixes (concrete, not vague)

### Step 4: Apply fixes (local overrides only)

If the skill is a local override, offer to apply each fix directly.
If the skill is a repo version, offer to clone first, then apply fixes.

## Delivery

### Report Template

```markdown
## Skill Feedback Report

**Skill:** `<name>` (<source-path>)
**Type:** `<issue_type>`
**Date:** <YYYY-MM-DD>
**Repo:** <repo-name> @ <short-sha>

### What happened
<developer description>

### Failed command
```
<command>
```
Exit code: <code>
Stderr: <stderr output, truncated to 500 chars>

### Expected behavior
<what should have happened>

### Relevant skill excerpt
> <quoted section from SKILL.md>

### Environment
- OS: <uname -s> (<uname -r>)
- Shell: <$SHELL>
- gh version: <gh --version>
```

### GitHub Discussion Delivery

1. Find or create the skill's discussion thread:
   ```bash
   # Get the "Skill Feedback" category ID
   gh api graphql -f query='{ repository(owner: "dotCMS", name: "core") { discussionCategories(first: 20) { nodes { id name } } } }'
   ```
   Store the category ID for "Skill Feedback" (one-time lookup, can cache).

2. Search for existing discussion for this skill:
   ```bash
   gh api graphql -f query='{ search(query: "repo:dotCMS/core category:\"Skill Feedback\" \"Feedback: <skill-name>\"", type: DISCUSSION, first: 1) { nodes { ... on Discussion { id title url } } } }'
   ```

3. If no discussion exists, create one:
   ```bash
   gh api graphql -f query='mutation { createDiscussion(input: { repositoryId: "<repo-id>", categoryId: "<category-id>", title: "Feedback: <skill-name>", body: "Feedback thread for the `<skill-name>` skill in `.claude/skills/<skill-name>/`.\n\nReports are appended as comments below." }) { discussion { id url } } }'
   ```

4. Add the report as a comment:
   ```bash
   gh api graphql -f query='mutation { addDiscussionComment(input: { discussionId: "<discussion-id>", body: "<scrubbed-report-markdown>" }) { comment { url } } }'
   ```

5. Capture the comment URL for the Slack notification.

### Slack Delivery

After the GitHub Discussion comment is posted, send a Slack notification using the Slack MCP `slack_send_message` tool:

- Channel: `#skill-feedback` (look up channel ID first with `slack_search_channels`)
- Message format:
  ```
  New skill feedback: **<skill-name>**
  Type: `<issue_type>` — <one-line summary>
  Full report: <discussion-comment-url>
  ```

### Gist Delivery

When `gist <skill>`:

1. Generate diff:
   ```bash
   diff -ru .claude/skills/<skill>/ ~/.claude/skills/<skill>/ --exclude='.skill-origin.json' > /tmp/skill-doctor-diff.patch
   ```
2. Create gist:
   ```bash
   gh gist create /tmp/skill-doctor-diff.patch --desc "skill-doctor: changes to <skill-name>" --public
   ```
3. Post gist URL to Slack `#skill-feedback`

### PR Delivery

When `pr <skill>`:

1. Create a branch:
   ```bash
   git checkout -b skill-doctor/<skill>-update
   ```
2. Copy local override back to repo (excluding tracking file):
   ```bash
   rsync -av --exclude='.skill-origin.json' ~/.claude/skills/<skill>/ .claude/skills/<skill>/
   ```
3. Stage and commit:
   ```bash
   git add .claude/skills/<skill>/
   git commit -m "fix(skills): update <skill> skill based on feedback"
   ```
4. Push and create PR:
   ```bash
   git push -u origin skill-doctor/<skill>-update
   gh pr create --title "fix(skills): update <skill> skill" --body "## Summary\n\nUpdates to the \`<skill>\` skill based on developer feedback.\n\n## Changes\n\n<diff summary>\n\n## Test plan\n\n- [ ] Invoke the skill and verify the fix\n- [ ] Check that the skill still works for its primary use case"
   ```
5. Offer to revert local override now that changes are in a PR

## Override Warning

Whenever ANY skill is about to be used and a local override exists for it, output this warning BEFORE the skill content:

```
SKILL OVERRIDE ACTIVE: <name>
  Using: ~/.claude/skills/<name>/ (local)
  Instead of: .claude/skills/<name>/ (repo)
  Cloned from: <sha> | Repo is N commits ahead
  Run /skill-doctor --manage <name> for options
```

To detect this: check if `~/.claude/skills/<name>/` exists AND `~/.claude/skills/<name>/.skill-origin.json` exists.

---

## Implementation Order

Build in this order, testing each component before moving to the next:

1. **Task 1** (above): Scaffold + SKILL.md skeleton with argument parsing
2. **Task 2**: Manage mode — `clone` and `.skill-origin.json` creation
3. **Task 3**: Manage mode — `--manage` status display and staleness detection
4. **Task 4**: Manage mode — `diff`, `sync`, `revert`
5. **Task 5**: Diagnose mode — context gathering and issue classification
6. **Task 6**: PII scrubbing
7. **Task 7**: Diagnose mode — inline fix for local overrides
8. **Task 8**: Delivery — GitHub Discussion (find/create thread + add comment)
9. **Task 9**: Delivery — Slack notification
10. **Task 10**: Delivery — gist and PR
11. **Task 11**: Optimize mode — best practices analysis
12. **Task 12**: Optimize mode — staleness checking (file/command verification)
13. **Task 13**: Integration testing — end-to-end workflow test

---

## Testing Strategy

Since this is a skill (documentation that guides Claude's behavior), testing follows the writing-skills TDD approach:

**For each task:**
1. Write a test scenario (pressure scenario or application scenario)
2. Run it WITHOUT the skill section → document baseline behavior
3. Write the skill section
4. Run it WITH the skill section → verify compliance
5. Close loopholes found during testing

**Test scenarios to write:**

| Task | Test scenario |
|------|--------------|
| Clone | "Clone the triage skill locally" — verify `.skill-origin.json` created correctly |
| Status | "Show me skill status" with a mix of overridden and non-overridden skills |
| Diff | "What changed in my local cicd-diagnostics?" — verify diff excludes tracking file |
| Diagnose | Simulate a skill failure (wrong `gh` flag) — verify context is captured correctly |
| PII scrub | Include API keys, home paths, emails in context — verify all scrubbed |
| Report delivery | Post to Discussion — verify GraphQL mutations are correct |
| Optimize | Run against a known-bad skill — verify all checks fire |

**IMPORTANT:** The superpowers:test-driven-development and superpowers:writing-skills skills define how to test skills. Follow their methodology — baseline first, then write, then verify.