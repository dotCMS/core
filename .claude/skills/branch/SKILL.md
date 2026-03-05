---
name: branch
description: Create a feature branch for a dotCMS issue and assign it to @me. Pass issue number and title, or omit to use values already in context. Skips creation if already on the correct branch.
allowed-tools: Bash(git checkout:*), Bash(git pull:*), Bash(git branch:*), Bash(gh issue edit:*), Bash(bash .claude/skills/solve-issue/scripts/slugify.sh:*)
---

# Branch

Input: `number`, `title` (from `/fetch-issue`), optional `existing_branch`.

## Step 1 — Check current branch

```bash
git branch --show-current
```

If already on an `issue-<number>-*` branch → skip to Step 3.

If `existing_branch` was set by `/fetch-issue` and user chose `reuse` → check it out:

```bash
git checkout <existing_branch>
```

Skip to Step 3.

## Step 2 — Create branch

First detect whether we're inside a git worktree:

```bash
git rev-parse --git-dir
# Returns a path ending in /worktrees/<name> if inside a worktree
```

**If inside a worktree** (`git rev-parse --git-dir` contains `/worktrees/`):

```bash
BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
# Create a new worktree for the branch — do NOT use git checkout
git fetch origin main
git worktree add ../<BRANCH> -b "$BRANCH" origin/main
```

Then inform the user: `"Worktree created at ../<BRANCH> — cd into it to start work."`

**If in the main working tree** (standard checkout):

```bash
git checkout main && git pull origin main
BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
git checkout -b "$BRANCH"
git branch --show-current
```

## Step 3 — Assign issue

```bash
gh issue edit <number> --repo dotCMS/core --add-assignee @me
```

## Output

```
✅ Branch: <branch-name>
```

Return `branch_name` for use by downstream steps.
