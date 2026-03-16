---
description: Create and manage isolated worktrees for parallel development in the dotCMS monorepo using worktrunk (wt). Use this skill whenever the user wants to set up a new branch for a task, work on multiple things in parallel, split work into sub-tasks, spawn a Claude agent in a separate environment, merge sub-task branches back together, list or clean up worktrees, or mentions "worktree", "branch off", "parallel tasks", "spin up an agent", or "isolated environment". Also use this skill proactively when EnterWorktree is about to be used — it must be intercepted in this repo.
allowed-tools: Bash(wt *), Bash(mise exec -- wt *), Bash(git branch*), Bash(git worktree*), Bash(git log*), Bash(git rev-parse*), Bash(git fetch*), Bash(tmux *), Bash(zellij *), Bash(command -v *), Bash(cursor *), Bash(code *), Bash(idea *)
---

# Worktree Management for dotCMS

Create and manage isolated worktrees for parallel development. Each worktree gets a warm start — build artifacts, node_modules, and caches are copied from the source branch via copy-on-write (reflink), so new worktrees are ready to `just dev-run` immediately.

This skill is the **authoritative source** for worktree operations in this repository. It supersedes the generic `worktrunk:worktrunk` vendor plugin for all dotCMS-specific workflows.

## CRITICAL: Do not use EnterWorktree

**Never use Claude Code's built-in `EnterWorktree` tool** in this repository. It creates worktrees in `.claude/worktrees/` without running worktrunk's post-create hooks, resulting in:
- No build artifacts copied (requires full ~15 min rebuild)
- No `yarn install` (frontend broken)
- No lefthook hooks (git hooks missing)
- No `mise trust` (tool versions not resolved)

**Always use `wt switch --create` via Bash instead.** This runs the project's `.config/wt.toml` hooks and produces a ready-to-use worktree.

## Usage

```
/dotcms-worktree <action> [args]     # slash command entry point
```

## Workflow Detection

Before creating a worktree, determine which pattern the user needs. If the signals are ambiguous — for example the user says "set up a branch" without indicating whether it's independent work or a sub-task — ask a brief clarifying question rather than guessing:

- "Should this be its own PR (branching from main), or a sub-task of your current branch that merges back?"
- "Do you want to work on this yourself, or should I spawn a Claude agent for it?"

When the user is already on a feature branch (not `main`), default to Pattern 2 (sub-task) unless they explicitly say otherwise — developers on feature branches usually want sub-tasks that merge back.

### Pattern 1: Independent work (own PR)

For standalone features, bug fixes, or tasks that will be pushed as their own branch and PR. Branches from `main`.

```
main ──→ feature-branch ──→ own PR
```

**Signals:** "new feature", "fix bug #123", "independent task", "its own PR", or no indication of merging back to a parent feature branch.

**Ensure a fresh base** — the `copy-ignored` post-create hook needs a local worktree to copy build artifacts from, so `--base` must point to a local branch (not `origin/main`). The approach is: create from a local branch, then rebase to catch up with remote.

```bash
git fetch origin                          # update remote tracking refs
wt switch --create <branch-name>          # branches from local main (default)
# After creation, in the new worktree:
git rebase origin/main                    # align with latest remote main
```

If the current worktree is not on `main`, explicitly specify the base so `copy-ignored` can find build artifacts to copy:

```bash
wt switch --create <branch-name> --base main
git rebase origin/main
```

### Pattern 2: Sub-tasks of current work (merge back)

For parallel sub-tasks that will merge back into the current branch before submitting as a single PR. Branches from the current branch.

```
current-branch ──→ sub-task-1 ──┐
                ├→ sub-task-2 ──┤──→ merge back ──→ single PR
                └→ sub-task-3 ──┘
```

**Signals:** "split this into parallel tasks", "work on X and Y simultaneously", "merge back", "single PR", or the user is already on a feature branch.

```bash
# Record the parent branch name for merge-back
PARENT=$(git rev-parse --abbrev-ref HEAD)

# Use <short-parent>--<sub-task> naming (see Naming Conventions for why)
wt switch --create <short-parent>--<sub-task-name> --base "$PARENT"
```

### Pattern 3: Spawn with an AI agent

Launch a worktree with Claude Code running a specific task. `-x claude` tells worktrunk to start an interactive Claude session in the new worktree; arguments after `--` become its initial prompt.

Pick the simplest approach that fits the developer's setup:

#### Option A: Simplest — new terminal tab (no extra tools needed)

Create the worktree first, then open a new tab/window and start Claude there. This works everywhere — iTerm2, Terminal.app, VS Code terminal, any setup.

```bash
# Step 1: Create the worktree (from Claude Code or any terminal)
git fetch origin                                  # ensure tracking refs are current
wt switch --create fix-auth-bug --yes             # --yes for non-interactive (Claude Code, scripts)

# Step 2: Tell the developer to open a new terminal tab and run:
#   cd <worktree-path>    (shown in wt output)
#   git rebase origin/main                        # catch up with remote (Pattern 1 only)
#   claude 'Fix the session timeout bug in AuthResource.java'
```

For sub-tasks (Pattern 2), replace the create command with `wt switch --create <name> --base "$PARENT" --yes` — no rebase needed since you're branching from the current work.

When running from within Claude Code, prefer this approach — print the commands for the developer to copy-paste into a new tab. The `--yes` flag auto-approves post-create hooks since Claude Code can't answer interactive prompts.

#### Option B: One-liner — from the developer's own terminal

If the developer is typing directly in their terminal (not inside Claude Code), `-x claude` works in a single command:

```bash
wt switch --create fix-auth-bug -x claude -- 'Fix the session timeout bug in AuthResource.java'

# Sub-task from current branch
wt switch --create add-tests --base "$(git rev-parse --abbrev-ref HEAD)" -x claude -- 'Add integration tests for the new endpoint'
```

#### Option C: Background agents — tmux (for parallel work)

For spawning multiple agents that run simultaneously, tmux creates detached sessions each with their own terminal. Developers unfamiliar with tmux just need these commands:

```bash
# Spawn a background agent
tmux new-session -d -s fix-auth "wt switch --create fix-auth --yes -x claude -- 'Fix the session timeout'"

# Check on it later
tmux attach -t fix-auth         # connect to the session (Ctrl-B D to detach)
tmux ls                          # list all running sessions
```

Multiple agents in parallel:

```bash
PARENT=$(git rev-parse --abbrev-ref HEAD)
# Use a short recognizable prefix — see Naming Conventions re: git ref collisions
SHORT="my-feature"   # extract from $PARENT or ask the developer
tmux new-session -d -s task-a "wt switch --create ${SHORT}--api-tests    --base $PARENT --yes -x claude -- 'Add API integration tests'"
tmux new-session -d -s task-b "wt switch --create ${SHORT}--fix-styling  --base $PARENT --yes -x claude -- 'Fix the dashboard CSS regressions'"
tmux new-session -d -s task-c "wt switch --create ${SHORT}--update-docs  --base $PARENT --yes -x claude -- 'Update API documentation for new endpoints'"
```

Detect what's available before suggesting: check `$TMUX` (already in tmux), `command -v tmux` (tmux installed), `$ZELLIJ` (in zellij). If nothing is available and the developer only needs one agent, use Option A.

## Opening in an editor

After creating a worktree, open it in the developer's preferred editor. The `-x` flag runs any command after switching, and `{{ worktree_path }}` expands to the worktree directory.

| Editor | Command | Setup needed? |
|--------|---------|---------------|
| VS Code | `code <path>` | No — CLI installed automatically |
| Cursor | `cursor <path>` | No — CLI installed automatically |
| IntelliJ IDEA | `idea <path>` | Yes — see below |
| Terminal only | `cd <path>` | No |

**IntelliJ setup:** The `idea` command-line launcher is not available by default. To enable it: open IntelliJ → Tools → Create Command-line Launcher. This creates the `idea` shell script (typically at `/usr/local/bin/idea`). If `command -v idea` returns nothing, guide the developer through this step before using `-x idea`.

**Inline** — open the editor as part of creation:

```bash
wt switch --create fix-auth-bug -x 'cursor {{ worktree_path }}'
```

**After creation** — if the worktree already exists, just open it:

```bash
cursor "$(wt switch fix-auth-bug 2>&1 | grep -o '/.*')"
# or simply: open the path shown by wt switch
```

**Ask the developer** which editor they use if unknown — don't assume. To detect what's available:

```bash
# Check for common editors (use the first one found, or ask)
command -v cursor && echo "Cursor"
command -v code && echo "VS Code"
command -v idea && echo "IntelliJ"
```

**Shell aliases** — for developers who always want the same editor, suggest adding a shell alias:

```bash
# Add to ~/.zshrc or ~/.bashrc
alias wtc='wt switch --create -x "cursor {{ worktree_path }}"'
alias wtv='wt switch --create -x "code {{ worktree_path }}"'
alias wti='wt switch --create -x "idea {{ worktree_path }}"'

# Then: wtc fix-auth-bug   → creates worktree + opens in Cursor
```

## Actions

### create — Create a new worktree

Ask the user (or infer from context):
1. **Branch name** — suggest a descriptive name based on the task
2. **Base branch** — main (Pattern 1) or current branch (Pattern 2)
3. **Agent or editor** — launch Claude Code, open in an editor, or just create

```bash
# Pattern 1: Independent work (create from local main, then rebase to latest)
git fetch origin
wt switch --create <name>
git rebase origin/main

# Pattern 2: Sub-task
wt switch --create <name> --base <parent-branch>

# Open in editor after creation
cursor <worktree-path>              # or code, idea

# With agent — see Pattern 3 for the right option (A/B/C) based on context.
# Do NOT run `-x claude` directly from the Bash tool — it needs a TTY.
```

After creation, confirm what happened:
- Which post-create hooks ran (mise trust, copy-ignored, yarn install, lefthook)
- The worktree path
- How to open it: editor command or `wt switch <name>`

### list — Show all worktrees

```bash
wt list
```

### switch — Switch to an existing worktree

```bash
wt switch <branch-name>
# Interactive picker if no branch specified:
wt switch
```

### merge — Merge a sub-task back to its parent

From within the sub-task worktree:

```bash
wt merge <parent-branch>
```

This squashes commits, rebases onto the parent, merges, and removes the worktree.

For merging multiple sub-tasks back:

```bash
# In sub-task-1 worktree:
wt merge <parent-branch>

# In sub-task-2 worktree:
wt merge <parent-branch>

# In sub-task-3 worktree:
wt merge <parent-branch>

# Now parent-branch has all sub-task work, ready for a single PR
```

### cleanup — Remove completed worktrees

**Never remove worktrees without explicit developer confirmation.** Worktrees may contain uncommitted work, stashed changes, or in-progress experiments that aren't visible from a branch listing.

```bash
# List all worktrees — look for stale ones (old age, no recent commits, merged branches)
wt list

# Show details before removing — let the developer review
wt list   # developer confirms which to remove

# Remove a specific worktree (prompts if unmerged changes exist)
wt remove <branch-name>

# Bulk cleanup of merged worktrees (interactive — developer confirms each one)
wt cleanup
```

When suggesting cleanup, show `wt list` output and highlight candidates (e.g. worktrees older than 2 weeks with no uncommitted changes), but always ask before removing.

## Naming Conventions

Suggest branch names that follow the project's convention:
- Features: `feature/<short-description>` or `<issue-id>-<short-description>`
- Bug fixes: `fix/<short-description>`
- Sub-tasks: `<parent-short-name>--<sub-task-name>` (use `--` as separator)

**Git ref collision:** Git cannot create `A/B` if `A` already exists as a branch (refs are filesystem paths). This means the pattern `<parent-branch>/<sub-task>` **fails** when the parent branch contains slashes (e.g. `cursor/my-feature` blocks `cursor/my-feature/sub-task`). Always use `--` to join the parent's short name to the sub-task:

```
# Parent: cursor/development-environment-setup-e028
# Good:  dev-env-setup--api-tests
# Bad:   cursor/development-environment-setup-e028/api-tests  ← git ref collision
```

Extract a short, recognizable portion of the parent branch name rather than using the full name.

## What Happens on Creation

The project config `.config/wt.toml` runs `post-create` hooks (blocking — all complete before you can work):

1. **`wt step copy-ignored --from <base>`** — copies Nx/Angular caches via reflink (near-zero disk cost on APFS/btrfs). Scoped by `.worktreeinclude` — copies caches only, NOT `node_modules`.
2. **`just worktree-init`** — installs mise tools, runs `yarn install` (fresh, matching lockfile), builds Stencil webcomponents, wires lefthook git hooks.
3. **Docker image re-tag** — tags the source worktree's image for the new branch so `just dev-run` works immediately.
4. **Port assignment** — writes a deterministic port (10000-19999 range, derived from branch name via `hash_port`) to `.dev-port`.

See `.config/wt.toml` for the hook definitions and `just worktree-init` for the init recipe. These are the source of truth — this section is a summary.

Files copied (scoped by `.worktreeinclude`):

| Pattern | Size | Purpose |
|---------|------|---------|
| `core-web/.nx/` | ~150MB | Nx build cache |
| `core-web/.angular/cache` | ~400MB | Angular compilation cache |
| `core-web/.sass-cache` | small | Sass compilation cache |
| `.venv/` | small | Python virtual environment |

`node_modules/` is NOT copied — `just worktree-init` runs `yarn install` fresh to ensure deps match the branch's lockfile. `target/` and `.cache/` are also excluded.

## Post-Creation Dev Commands

After switching to a worktree, the developer can immediately:

```bash
just dev-run 8080                  # start the stack (uses worktree's image, falls back to 1.0.0-SNAPSHOT)
just dev-run 8080 image=default    # start with stock 1.0.0-SNAPSHOT (no build needed — frontend devs)
just dev-start-frontend            # start Angular dev server at :4200 proxying to backend
just build-quicker                 # rebuild if Java code differs from base
just dev-images                    # see all locally available images
just dev-containers                # see all running dotCMS stacks across worktrees
```

**Frontend-only workflow (no build):** New worktrees inherit the parent's Docker image tag via the `image` post-create hook. Frontend devs can skip building entirely:

```bash
wt switch --create ui-redesign
just dev-run 8084 image=default    # start stock image on a free port
just dev-start-frontend            # nx serve at :4200, auto-discovers backend port
```

**Parallel worktrees:** Each worktree gets its own container namespace, so multiple stacks can run simultaneously on different ports without collision. Use `just dev-stop` to stop only the current worktree's containers, or `just dev-stop-all` to stop everything.

**Shared services (resource-efficient parallel):** Running 3+ worktrees wastes ~4GB+ RAM on duplicate DB + OpenSearch sidecars. Use `just dev-shared-start` once, then `just dev-run` auto-detects and shares them. Each worktree gets its own database and index namespace — fully isolated.

For shared services setup, dev-run mode options, command chaining rules, frontend dev server details, and AI agent cleanup lifecycle, see the `dotcms-dev-services` skill.

## Configuration Reference

### Project config: `.config/wt.toml`

Committed, shared with the team. Defines lifecycle hooks for worktree creation/cleanup. Edit this to change what runs on `wt switch --create`.

### Copied files: `.worktreeinclude`

Committed. Controls which gitignored files are copied between worktrees. Also read by Claude Code desktop's native worktree support.

### User config: `~/.config/worktrunk/config.toml`

Personal preferences — worktree path template, LLM commit message generation, command defaults. Not committed. Run `wt config create` to initialize.

### LLM commit messages

Worktrunk can generate commit messages during `wt merge` using an LLM. This is configured in the user config:

```toml
[commit.generation]
command = "claude -p --model=haiku --tools='' --disable-slash-commands --setting-sources='' --system-prompt=''"
```

Run `wt config show` to check current configuration.
