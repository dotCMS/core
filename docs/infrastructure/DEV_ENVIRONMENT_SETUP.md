# Developer Environment Setup

## Overview

This document describes the standard approach for managing tool versions, environment variables, and developer tooling across local shells, AI agent tools (Claude Code), and CI/CD pipelines.

The solution uses two tools that bootstrap each other:

- **[mise](https://mise.jdx.dev)** — unified tool version manager, environment variable manager, and task runner. Replaces nvm, sdkman, pyenv, and similar per-language version managers.
- **[just](https://just.systems)** — command runner for defining and sharing project tasks. More widely pre-installed than mise, making it the reliable entry point for developers new to the project.

---

## Quick Start

```sh
git clone https://github.com/dotcms/core.git
cd core
just setup
# restart your shell
```

---

## How Each Developer Gets Started

### Path 1 — Developer already has `just`

```sh
just setup
# installs mise, configures shell, installs all tools
```

### Path 2 — Developer already has `mise`

```sh
mise install       # installs just and all tools from .mise.toml
just setup         # configures shell profiles
```

After either path, both `just setup` and `mise run setup` do the same thing. The setup script is **idempotent** — safe to re-run at any time if the environment seems misconfigured.

---

## Project Files

### `.mise.toml`

The primary configuration file, committed to the repository. Declares all tool versions, environment variables, and a task alias.

Key tools managed:
- `lefthook` — git hooks manager (replaces husky + lint-staged)
- `just` — command runner for build/dev tasks
- `github:max-sixty/worktrunk` — git worktree manager for parallel AI agent sessions
- `gh` — GitHub CLI
- `python` — for automation scripts and CI diagnostics
- `actionlint` + `shellcheck` — GitHub Actions and shell linting

Java and Node versions are **not** declared in `.mise.toml` — they are read from the existing `.sdkmanrc` and `.nvmrc` files via the `idiomatic_version_file_enable_tools` setting.

### `justfile`

Defines the `setup` task and all build/dev/test commands. The setup recipe installs mise if not present, configures the developer's shell for both interactive and non-interactive sessions, installs all tools, and sets up worktrunk shell integration.

### `.gitignore`

Local override files are never committed:

```gitignore
.env.local
.mise.local.toml
```

### `.mise.local.toml` (per-developer, gitignored)

Per-developer overrides, never committed:

```toml
[env]
DOTCMS_LICENSE_PATH = "/path/to/my/license.dat"
```

---

## How Shell Configuration Works

The setup script writes two lines per developer based on their detected shell. This two-file approach is intentional and conflict-free.

| File | Line added | Purpose |
|---|---|---|
| `~/.zprofile` / `~/.bash_profile` | `eval "$(mise activate zsh --shims)"` | Non-interactive sessions |
| `~/.zshrc` / `~/.bashrc` | `eval "$(mise activate zsh)"` | Interactive terminal sessions |

When an interactive shell starts, `mise activate` automatically removes the shims directory from PATH and replaces it with direct tool paths, so there is no duplication or conflict between the two lines.

### Why two files matter for Claude Code

Tools like Claude Code spawn non-interactive shells that never source `.zshrc`, so `mise activate` never runs in those processes. By placing `mise activate --shims` in the profile file — which is sourced by all login shells — the shims directory is on PATH for every child process, including those spawned by Claude Code, without requiring any further activation.

```
Login shell starts
  └── ~/.zprofile runs → shims added to PATH
        └── ~/.zshrc runs (interactive only) → full activate replaces shims
              └── Claude Code spawns subprocess
                    └── inherits shims PATH → correct tool versions ✓
```

---

## Idiomatic Version Files

Existing `.nvmrc` and `.sdkmanrc` files are supported natively. The `idiomatic_version_file_enable_tools` setting in `.mise.toml` opts in per tool.

```
# .nvmrc
v22.15.0
```

```
# .sdkmanrc
java=21.0.8-ms
```

Mise reads these automatically and uses them as the version source when no version is specified in `[tools]`. Developers already using nvm or sdkman do not need to change or remove their existing setup — both tools read the same version files and resolve to the same version. If both are active in the same shell, mise wins because `mise activate` re-runs on every prompt. The only practical side effect is that both tools maintain their own separate installation of the same version, resulting in duplicate disk usage but no functional conflict.

Note that not all sdkman Java vendors are supported by mise. The following are **not** supported: `bsg` (Bisheng), `graal` (GraalVM), `nik` (Liberica NIK). If your project requires one of these, manual installation is needed for those developers.

---

## Worktrunk (`wt`)

[Worktrunk](https://worktrunk.dev) is a CLI for Git worktree management designed for running AI agents in parallel. It makes it trivial to give each Claude Code session its own isolated branch and working directory.

It is installed automatically via mise using the GitHub backend:

```toml
[tools]
"github:max-sixty/worktrunk" = "latest"
```

The `just setup` script runs `wt config shell install` after tool installation to enable the shell integration required for `wt switch` to `cd` automatically.

### Warm starts — no rebuild needed

When `wt switch --create` creates a new worktree, the project config (`.config/wt.toml`) runs `post-create` hooks that:

1. **Trust mise** — so tool versions resolve immediately
2. **Copy build artifacts** from the source worktree via `wt step copy-ignored` — uses copy-on-write (reflink) on APFS/btrfs, copying ~6GB in ~20s with near-zero disk cost
3. **Run `yarn install`** — fast no-op if `node_modules/` was copied and the lockfile matches
4. **Install lefthook** — wires up git hooks in the new worktree

What gets copied is scoped by `.worktreeinclude`:

| Pattern | Size | Why |
|---------|------|-----|
| `target/` | ~3GB | Maven build output — avoids full rebuild |
| `core-web/node_modules/` | ~2.8GB | Frontend dependencies |
| `core-web/.nx/` | varies | Nx build cache |
| `core-web/.angular/cache` | varies | Angular compilation cache |
| `.cache/` | varies | Maven build cache |
| `.venv/` | small | Python virtual environment |

The net effect: a new worktree is ready to `just dev-run` immediately after creation — no build step required unless the branch changes Java or frontend code that invalidates the copied artifacts.

### Core workflow

```sh
# Create a new branch and worktree, launch Claude Code in it
wt switch --create feature-auth -x claude

# Run multiple agents in parallel
wt switch -x claude -c feature-a -- 'Add user authentication'
wt switch -x claude -c feature-b -- 'Fix the pagination bug'
wt switch -x claude -c feature-c -- 'Write tests for the API'

# Check status across all worktrees
wt list

# Merge completed work
wt merge main
```

### Project config (`.config/wt.toml`)

The project config is committed and shared with the team. It defines lifecycle hooks:

```toml
[post-create]
mise = "command -v mise >/dev/null 2>&1 && mise trust || true"
copy = "wt step copy-ignored --from {{ base }}"
deps = "cd core-web && yarn install --frozen-lockfile"
hooks = "lefthook install 2>/dev/null || true"
```

All `post-create` hooks are **blocking** — they complete before any `--execute` command runs, ensuring the worktree is fully ready for use.

> **Windows note:** `wt` conflicts with Windows Terminal's command. Windows developers should use `git-wt` instead, or disable the Windows Terminal alias via Settings → Privacy & security → For developers → App Execution Aliases.

---

## Environment Variables

Environment variables are declared in `.mise.toml` and are automatically available to any process that uses mise tools. Sensitive or per-developer values go in `.env.local`, which is gitignored.

### Limitation with shims

Environment variables from `.mise.toml` are only injected when a mise shim is invoked. This means `echo $NODE_ENV` in a plain shell won't see the value, but `node -e "console.log(process.env.NODE_ENV)"` will, because the node shim loads the mise environment before executing.

For scripts that need environment variables without invoking a tool, use `mise exec` or `mise run`:

```sh
mise exec -- bash -c "echo $NODE_ENV"
mise run some-task
```

---

## CI/CD

### GitHub Actions

Use the official action which handles installation and caching automatically:

```yaml
steps:
  - uses: actions/checkout@v4

  - name: Install mise
    uses: jdx/mise-action@v2

  - name: Install tools
    run: mise install
```

### GitLab CI / Other CI systems

```yaml
build:
  script:
    - curl https://mise.run | sh
    - export PATH="$HOME/.local/share/mise/shims:$PATH"
    - mise install
    - mise run build
```

---

## Summary

| Context | How correct tool versions are resolved |
|---|---|
| Interactive terminal (any shell) | `mise activate` via `.zshrc` / `.bashrc` |
| Non-interactive shell (scripts, hooks) | Shims via `.zprofile` / `.bash_profile` |
| Claude Code and agent tools | Inherits shims from login shell environment |
| `mise run` / `just` tasks | mise injects tools directly regardless of PATH |
| CI/CD | Explicit `mise install` + shims on PATH |
| Developers with existing nvm/sdkman | Both tools read the same version files; mise wins on PATH; duplicate disk usage only |

### Files at a glance

| File | Committed | Purpose |
|---|---|---|
| `.mise.toml` | ✅ | Tool versions, shared env vars, task alias |
| `justfile` | ✅ | Setup task and project commands |
| `.nvmrc` | ✅ | Node version (read by both mise and nvm) |
| `.sdkmanrc` | ✅ | Java version (read by both mise and sdkman) |
| `.mise.local.toml` | ❌ | Per-developer tool overrides |
| `.env.local` | ❌ | Per-developer secrets |
