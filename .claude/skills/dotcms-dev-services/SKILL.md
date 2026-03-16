---
description: Dev infrastructure for running the dotCMS stack — dev-run modes, shared services for parallel worktrees, frontend dev server, and command chaining rules for AI agents. Use this skill when the user asks about starting/stopping dotCMS, dev-run options, shared services setup or cleanup, port discovery, build-then-restart workflows, or AI agent cleanup after finishing work. Trigger on "start the stack", "shared services", "dev-run", "dev environment", "frontend dev server", "dev-restart", "dev-stop", "clean up indexes", or any question about the development lifecycle beyond the basic commands shown in AGENTS.md. Also use proactively when an AI agent needs to run dev commands in sequence or manage shared infrastructure.
allowed-tools: Bash(just *), Bash(docker *), Bash(./mvnw *), Bash(curl *)
---

# Dev Services for dotCMS

Running, managing, and tearing down the dotCMS development stack. Covers starting services, shared infrastructure for parallel worktrees, frontend dev server, and rules AI agents should follow when chaining dev commands.

For creating and managing worktrees themselves (branching, merging, agent spawning), see the `dotcms-worktree` skill.

## Starting the Stack

`just dev-run` is the primary entry point. It resolves the port, Docker image, detects shared vs local mode, and starts the app.

```bash
just dev-run                         # start on worktree's port (.dev-port → else 8082)
just dev-run 8080                    # start on explicit port
just dev-run 8080 image=default      # use stock 1.0.0-SNAPSHOT image (skip build)
just dev-run 8080 mode=local         # force per-worktree sidecars (ignore shared services)
just dev-run 8080 mode=shared        # force shared services mode
```

### Port resolution

Port is resolved in order: explicit argument > `.dev-port` file > default `8082`.

The `.dev-port` file is **automatically created** when a worktree is set up via `wt switch --create`. The `hash_port` filter in `.config/wt.toml` generates a deterministic port in the 10000-19999 range from the branch name — the same branch always gets the same port on any machine. This means `just dev-run` (no args) works immediately in any worktree, and parallel worktrees never collide on ports.

To assign a port to an existing worktree that predates this feature, create the file manually:
```bash
echo 14500 > .dev-port      # pick any unused port
```

### Mode detection

When `mode` is omitted (the default), `dev-run` auto-detects:

1. Checks if `dotcms-shared-db` container is running
2. If yes → **shared mode** (app only, connects to shared DB + OpenSearch via Docker network)
3. If no → **local mode** (starts app + DB + OpenSearch sidecars via fabric8)

In shared mode, `dev-run`:
- Creates a per-worktree database in the shared PostgreSQL instance
- Sets `DOT_DOTCMS_CLUSTER_ID` for OpenSearch index isolation
- Activates the `shared-services` Maven profile (app joins `dotcms-shared` network, no sidecar links)

In local mode, `dev-run` uses the standard fabric8 docker-maven-plugin path with per-worktree container namespacing.

### Image resolution

When `image` is omitted:
1. Looks for `dotcms/dotcms-test:<worktree-slug>` (built by `just build`)
2. Falls back to `dotcms/dotcms-test:1.0.0-SNAPSHOT`
3. Fails with guidance if neither exists

`image=default` always uses `1.0.0-SNAPSHOT`. Any other value is used as-is.

### Stopping and restarting

```bash
just dev-stop                        # stop current worktree's containers (both modes)
just dev-stop-all                    # stop ALL dotCMS containers across all worktrees
just dev-restart                     # stop + restart on worktree's port (same resolution as dev-run)
just dev-restart 8080 mode=local     # restart forcing explicit port + local mode
just dev-clean-volumes               # remove Docker volumes for current worktree
```

`dev-stop` handles both modes: directly removes the app container (shared mode), then runs Maven docker-stop for sidecars (local mode). Either path is a safe no-op if not applicable.

## Shared Services

For running multiple worktrees simultaneously without duplicating DB + OpenSearch per worktree (~4GB+ RAM saved).

### Setup

```bash
just dev-shared-start                # starts PostgreSQL + OpenSearch 1.3 + OpenSearch 3.4
```

Fixed host ports (well outside dynamic allocation range):
- PostgreSQL → `localhost:15432`
- OpenSearch 1.3 → `localhost:19200`
- OpenSearch 3.4 → `localhost:19201`

After starting, `just dev-run` in any worktree auto-detects and uses them.

### Per-worktree isolation

**Database:** Each worktree gets its own PostgreSQL database (e.g., `dotcms_core_cursor_development_envir_0e809b4d`). Created automatically on first `dev-run`. Format: `<module>_<worktree-id>` where worktree-id is `<prefix_24>_<sha256_8>` — unique within PostgreSQL's 63-char identifier limit.

**OpenSearch indexes:** Prefixed with `cluster_<worktree-id>.` (see `ESIndexAPI.java:152`). Setting `DOT_DOTCMS_CLUSTER_ID` per worktree gives full index namespace isolation — the same mechanism used in production multi-cluster deployments.

### Management

```bash
just dev-shared-status               # compose ps for shared services
just dev-shared-list-dbs             # list all worktree databases with sizes
just dev-shared-stop                 # stop shared services (preserves volumes)
just dev-shared-clean                # stop + remove all data volumes
```

### Per-worktree cleanup

```bash
just dev-shared-drop-db              # drop current worktree's database
just dev-shared-drop-indexes         # delete OpenSearch indexes matching worktree prefix
just dev-shared-drop-worktree        # both: drop DB + indexes for current worktree
```

`dev-shared-drop-indexes` calls `DELETE /cluster_<worktree-id>.*` on both shared OpenSearch instances. Safe to run while shared services are still serving other worktrees.

### Lifecycle management

```bash
just dev-shared-stop-if-idle         # stop only if no app containers are connected
```

Inspects the `dotcms-shared` Docker network for `dotbuild_*` containers. If any are attached (from other worktrees), prints them and does nothing. Safe to call unconditionally from automation.

## AI Agent Cleanup Lifecycle

When an AI agent finishes work in a worktree, clean up in this order:

1. `just dev-stop` — stop the app container
2. `just dev-shared-drop-worktree` — remove DB + OpenSearch indexes for this worktree
3. `just dev-shared-stop-if-idle` — stop shared services if no other worktrees need them

Step 3 is safe to call unconditionally — it checks before acting. Always run step 2 to prevent OpenSearch index bloat (indexes accumulate indefinitely if not cleaned).

## Frontend Dev Server

```bash
just dev-start-frontend              # start nx serve at :4200 (background, PID-managed)
just dev-start-frontend 4201         # custom port — for parallel worktrees
just dev-frontend-wait               # block until ready (fails fast on build errors)
just dev-stop-frontend               # stop it (uses PID file, port-agnostic)
just dev-frontend-logs               # tail the log (blocking — Ctrl-C to exit)
just dev-frontend-status             # last 40 lines (non-blocking snapshot)
```

Auto-discovers the backend port from Docker and injects it into the proxy config. Falls back to `:8080` if no container is running. The proxy target is fixed at startup — restart the frontend dev server if the backend moves to a different port.

For parallel worktrees, use different frontend ports (e.g., 4200, 4201, 4202) — each proxies to its own worktree's backend container (auto-discovered from Docker). `NX_DAEMON=false` is set automatically to prevent the Nx daemon from serializing parallel worktree builds.

**Important:** The frontend dev server URL serves live `core-web/` edits via hot-reload. The backend's `/dotAdmin` serves the Angular WAR compiled into the Docker image at build time and does NOT reflect `core-web/` changes.

### Troubleshooting `nx serve` build errors

If `dev-start-frontend` starts but the Angular build fails with TypeScript errors (check `just dev-frontend-status`), common causes:

1. **Missing Stencil build** — `dotcms-webcomponents` is a Stencil library that must be pre-built (generates `loader/` directory). `just worktree-init` handles this automatically for new worktrees. For existing worktrees: `cd core-web && NX_DAEMON=false yarn nx build dotcms-webcomponents`.
2. **Branch mismatch** — If the worktree branched from a stale base, the frontend code may reference APIs that don't exist yet in library types. Rebase onto `main` or the correct base branch.
3. **Stale `node_modules`** — After rebases that pull in new dependencies: `cd core-web && yarn install`. If yarn says "already up-to-date" but packages are missing, delete `node_modules` and reinstall.

The dev server enters watch mode even with errors — fixing the source files will trigger a hot-reload rebuild without restarting.

## Build scope: `build` vs `build-quicker`

`just build` runs a full Maven build — all modules including `core-web` (yarn install + nx build all + WAR packaging). The resulting Docker image is fully self-consistent.

`just build-quicker` builds ONLY `dotcms-core` (`-pl :dotcms-core`). Dependencies like the `dotcms-core-web` WAR (Angular frontend) are pulled from `~/.m2/repository/` — **whatever was last installed by any worktree's `just build`**. The staleness check at the top of `build-quicker` warns when the embedded frontend is from a different branch.

**When to use which:**

| Scenario | Use | Why |
|---|---|---|
| Backend-only change + testing at `:4200` | `build-quicker` | Frontend comes from `nx serve`, not the WAR |
| Backend-only change + testing at `:PORT/dotAdmin` | `build-quicker` | Frontend in WAR may be stale but backend API is fresh |
| Frontend + backend change | `build` | Need consistent WAR with both changes |
| E2E tests | `build` | Tests need a self-consistent image |
| First build in a new worktree | `build` | No cached WAR in `.m2` for this branch |

**The `.m2` sharing problem:** All worktrees share `~/.m2/repository/`. A `just build` in worktree A writes `dotcms-core-web-1.0.0-SNAPSHOT.war` to `.m2`, then `just build-quicker` in worktree B picks up A's frontend. The `build-quicker` recipe detects this and warns.

## Command Chaining Rules

Rules for AI agents running dev commands:

1. **`dev-run` blocks until healthy** — Maven's docker-start profile waits for health checks (3-8 minutes). From Claude Code's Bash tool (which has a timeout), long starts may get killed.

2. **Long-running starts: background + poll** — When `dev-run` may exceed the Bash timeout:
   ```bash
   nohup just dev-run > /tmp/dotcms-start.log 2>&1 &
   # then poll:
   just dev-wait              # blocks until healthy (max 3 min)
   just dev-urls              # print actual ports
   ```
   Always use `just dev-run` rather than raw `./mvnw` — it handles shared vs local mode detection, image resolution, and worktree namespacing.

3. **`dev-urls` is the source of truth for ports** — The management port is always dynamic. Never hardcode it; always call `just dev-urls` or `just dev-health` to discover live port mappings.

4. **`dev-health` before `dev-urls`** — If unsure whether the app is up, check `just dev-health` first (single line, fast). Only call `dev-urls` after confirming liveness.

5. **Build scope matches what changed** — `just build-quicker` (core only, ~2-3 min) when only Java changed; `just build` (full, ~8-15 min) when multi-module or frontend WAR changed. After any build, `just dev-restart` picks up the new image (uses the worktree's `.dev-port` automatically).
