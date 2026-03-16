# dotCMS

Headless/hybrid CMS — Java 21 backend (Maven), Angular frontend (Nx/Yarn), Docker infrastructure. Migrating to Java 25 with parallel CI workflows; core source uses Java 11 release compatibility.

> **Note:** `CLAUDE.md` is a symlink to this file (`CLAUDE.md → AGENTS.md`). Claude Code reads `CLAUDE.md`; Codex, Aider, and Cursor read `AGENTS.md`. They are the same content.

## Setup

```bash
just setup                                         # one command: installs mise, configures shell, installs all tools
# restart your shell, then:
just build                                         # full build (~8-15 min)
just dev                                           # start backend + frontend + wait for both
```

Tool versions (Java, Node) are read from `.sdkmanrc` and `.nvmrc` via [mise](https://mise.jdx.dev). See `docs/infrastructure/DEV_ENVIRONMENT_SETUP.md` for details.

## Build, test, run

```bash
just build                                         # full build (~8-15 min)
just build-quicker                                 # core only (~2-3 min, uses cached frontend WAR)
just dev                                           # start backend + frontend + wait for both
just dev-run                                       # start backend on worktree's port (.dev-port, else 8082)
just dev-stop                                      # stop current worktree's containers
just dev-restart                                   # stop + restart — use after a rebuild
just dev-urls                                      # print live URLs (dotAdmin, REST API, health)
just dev-health                                    # check server health
```

Run `just --list` for all available commands. For dev lifecycle details (shared services, modes, port resolution, frontend dev server, command chaining), see the `/dotcms-dev-services` skill.

> **Dev loop:** `just build` → `just dev` → edit code → `just build-quicker` → `just dev-restart`. Port resolution: explicit arg > `.dev-port` file (set by `wt switch --create`) > 8082. The management port is always dynamic — use `just dev-urls`.

### Testing

Target specific classes — never run the full suite (~60 min):

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
just test-e2e-node                                 # Playwright E2E (full suite)
just test-e2e-node-specific test=login.spec.ts     # single spec file
```

Tests are **silently skipped** without explicit `skip=false` flags: `-Dcoreit.test.skip=false`, `-Dpostman.test.skip=false`, `-Dkarate.test.skip=false`, `-De2e.test.skip=false`.

## References

- `justfile` — run with `just <command>` or read as a Maven command reference
- `docs/README.md` — full documentation index (Java, Angular, testing, REST, Docker, CI/CD)
- `docs/backend/REST_API_PATTERNS.md` — REST endpoints require dotCMS-specific auth init and response wrapping
- `core-web/AGENTS.md` — frontend standards and Nx commands
- `.claude/rules/` — Claude Code path-scoped rules (Java, frontend, testing, shell)
- `.cursor/rules/` — Cursor path-scoped rules (same domains)
- `docs/infrastructure/DEV_ENVIRONMENT_SETUP.md` — mise/just setup, shell configuration, CI/CD
- `docs/claude/CONTEXT_ARCHITECTURE.md` — where to place new instructions for AI agents

## Worktrees

Use [worktrunk](https://worktrunk.dev) (`wt`) to create isolated worktrees — **do not use Claude Code's built-in `EnterWorktree`**, which bypasses the project's post-create hooks and produces cold-start worktrees (no deps, no hooks, no port assignment).

```bash
wt switch --create feature-x                       # branch from main → own PR
wt switch --create sub-task --base current-branch   # branch from current → merge back
```

New worktrees start warm — `.config/wt.toml` hooks run `just worktree-init`, re-tag the Docker image, and assign a deterministic port. Ready to `just dev` immediately. See the `/dotcms-worktree` skill for full workflow patterns.

## Gotchas

- **Git hooks require mise** — run `mise install` to set up lefthook, or `LEFTHOOK=0 git commit` to skip
- **Frontend memory** — standalone Nx builds may OOM; set `NODE_OPTIONS="--max_old_space_size=4096"`
- **Frontend changes only show via `nx serve`** — `core-web/` edits are served live by `just dev-start-frontend`. They do NOT appear at the backend's `/dotAdmin`, which serves the Angular WAR from the Docker image. If changes are invisible, check which URL you're using.
- **`build-quicker` shares `.m2` across worktrees** — `just build-quicker` uses the frontend WAR last installed by any worktree's `just build`. The recipe detects and warns when the WAR is from a different branch. Use `nx serve` for frontend dev, or `just build` for a consistent image.
- **Docker image build fails with stale SHA** — run `docker builder prune -f` then retry
- **Tests are silently skipped** — integration, postman, karate, and E2E tests all require explicit `-Dskip=false` flags or nothing runs
- **Cross-platform scripts** — all justfile recipes and shell commands must work on both macOS and Linux. See `.claude/rules/shell-cross-platform.md` for the pitfall table.
