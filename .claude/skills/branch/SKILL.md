---
name: branch
description: Create a feature branch for a dotCMS issue and assign it to @me. Pass issue number and title, or omit to use values already in context. Skips creation if already on the correct branch.
allowed-tools: Bash(git checkout:*), Bash(git pull:*), Bash(git branch:*), Bash(git status:*), Bash(git stash:*), Bash(git diff:*), Bash(git fetch:*), Bash(git worktree:*), Bash(git rev-parse:*), Bash(gh issue edit:*), Bash(bash .claude/skills/solve-issue/scripts/slugify.sh:*)
---

# Branch

Input: `number`, `title` (from `/fetch-issue`), optional `existing_branch`.

## Step 1 — Check current branch

```bash
git branch --show-current
```

If already on an `issue-<number>-*` branch → skip to Step 3.

If `existing_branch` was set by `/fetch-issue` and user chose `reuse` → go to Step 1b.

### Step 1b — Switch to existing branch

Check for uncommitted changes first:

```bash
git status --porcelain
```

**If dirty (uncommitted changes exist):**

Ask the user:
> You have uncommitted changes. How would you like to handle them?
> 1. **Stash** — stash changes, switch branch, pop the stash onto the target branch
> 2. **Leave here** — switch branch cleanly (changes stay on current branch)
> 3. **Cancel** — abort

- Option 1 → `git stash push -m "WIP before switching to <existing_branch>"` → checkout → `git stash pop`
- Option 2 → proceed with checkout (git will error if files conflict; inform user)
- Option 3 → abort with message

**If clean:**

```bash
git checkout <existing_branch>
git pull origin <existing_branch> --ff-only
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
git fetch origin main
git worktree add ../$BRANCH -b "$BRANCH" origin/main
```

Inform the user: `"Worktree created at ../<BRANCH> — open it in a new terminal to start work."`
Skip to Step 3.

**If in the main working tree** (standard checkout):

### Step 2a — Handle uncommitted changes

```bash
git status --porcelain
```

**If dirty (uncommitted changes exist):**

Ask the user:
> You have uncommitted changes on `<current-branch>`. How would you like to handle them?
> 1. **Move to new branch** — stash now, create branch from main, pop stash there (start fresh from main with your WIP)
> 2. **Branch from current state** — create branch from current HEAD (keeps your WIP commits and changes in the new branch)
> 3. **Leave here and start clean** — stash changes on current branch, create clean branch from main
> 4. **Cancel** — abort

- **Option 1 (Move to new branch):**
  ```bash
  git stash push -m "WIP for issue-<number>"
  git checkout main && git pull origin main
  BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
  git checkout -b "$BRANCH"
  git stash pop
  ```

- **Option 2 (Branch from current state):**
  ```bash
  BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
  git checkout -b "$BRANCH"
  # No pull from main — branch starts from wherever HEAD currently is
  ```

- **Option 3 (Leave here, start clean):**
  ```bash
  git stash push -m "WIP on <current-branch>"
  git checkout main && git pull origin main
  BRANCH=$(bash .claude/skills/solve-issue/scripts/slugify.sh <number> "<title>")
  git checkout -b "$BRANCH"
  ```
  Inform user: `"Your WIP is stashed on <current-branch>. Run 'git stash pop' there to recover it."`

- **Option 4** → abort.

**If clean (no uncommitted changes):**

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
