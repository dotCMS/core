# dotCMS

Headless/hybrid CMS — Java 21 backend (Maven), Angular frontend (Nx/Yarn), Docker infrastructure. Migrating to Java 25 with parallel CI workflows; core source uses Java 11 release compatibility.

> **Note:** `CLAUDE.md` is a symlink to this file (`CLAUDE.md → AGENTS.md`). Claude Code reads `CLAUDE.md`; Codex, Aider, and Cursor read `AGENTS.md`. They are the same content.

## Cross-platform requirement

All scripts, justfile recipes, and shell commands in this repository **must work on both**:
- **macOS** — developer workstation (Apple Silicon or Intel)
- **Linux** — CI runners, Docker containers, and headless VMs

Avoid tools that exist only on one platform. Common pitfalls:

| Avoid | Use instead |
|-------|-------------|
| `lsof -ti :PORT` (macOS default) | `lsof` with fallback to `ss` or `fuser` |
| `brew install ...` | document OS-conditional install paths |
| `sed -i ''` (macOS BSD sed) | `sed -i` (GNU) — test on both or use `perl -i` |
| `/proc/` paths | avoid; not present on macOS |
| `mktemp -t name` (BSD form) | `mktemp /tmp/name.XXXXXX` (POSIX, works on both) |

When adding a new script or recipe, test or reason through both environments before committing.

## Build, test, run

```bash
./mvnw install -DskipTests                        # full build (~8-15 min), or: just build
./mvnw install -pl :dotcms-core -DskipTests        # core only (~2-3 min), or: just build-quicker

just dev-start-on-port 8080                        # start stack (DB + OpenSearch + app) with app on :8080
just dev-stop                                      # teardown
just dev-urls                                      # discover and print all live URLs from Docker (dotAdmin, REST API, health)
just dev-health                                    # check server health (management port discovered live from Docker)
just dev-wait                                      # block until healthy — use in scripts after start or restart

just dev-start-frontend                            # start Angular dev server :4200 (background, PID-managed)
just dev-stop-frontend                             # stop it cleanly
just dev-frontend-logs                             # tail the dev server log

cd core-web && npx nx lint dotcms-ui               # frontend lint
cd core-web && npx nx test dotcms-ui               # frontend test
```

> **First run:** build first (`just build`), then start — the build produces the Docker image the start command uses.
>
> **Build scope:** Match scope to what changed — core-only change → `just build-quicker`; multi-module → add `--am`; full rebuild → `just build`. After a core rebuild, restart to pick up the new image: `just dev-stop && just dev-start-on-port 8080`. Silent failures are rare; slow builds are common.
>
> **Build visibility:** Both `just build` and `just build-quicker` filter Maven output to phase transitions and errors — full log is saved to `/tmp/dotcms-build.*.log` for detailed inspection. To investigate a failure without loading the full log: `grep '^\[ERROR\]' /tmp/dotcms-build.<id>` shows only error lines. If the Docker image build fails with a stale layer SHA, run `docker builder prune -f` then retry.
>
> **Server URLs:** `dev-start-on-port PORT` fixes the app port to what you specify (e.g. `:8080`). The management port (8090 inside container) is **always** mapped to a dynamic host port regardless. Use `just dev-urls` after any start or restart to get the actual URLs — it queries Docker live and is the only reliable source for the health endpoint port.
>
> **Agent log strategy:** Prefer targeted commands over loading full logs into context. Use `just dev-health` (one line) to check liveness. On build failure, read only the `[ERROR]` lines from the saved log. Load the full log only when errors alone are insufficient to diagnose the problem.
>
> **Frontend dev:** `nx serve` serves Angular files directly from disk at `:4200` with hot-reload — it does NOT use the WAR embedded in Docker. Edit files in `core-web/` and changes appear instantly; no backend rebuild or restart needed for frontend-only changes. Access the dev app at `:4200`, not `:8080/dotAdmin`.
>
> **Frontend proxy:** `just dev-start-frontend` discovers the backend app port from Docker at startup and injects it as `DOTCMS_HOST` into the nx serve proxy config (`proxy-dev.conf.mjs`). If no backend container is running it falls back to `:8080`. The proxy target is fixed at frontend startup time — if the backend is restarted on a different port, restart the frontend dev server too.

### Testing

Target specific classes — never run the full suite (~60 min):

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
```

Tests are **silently skipped** without explicit `skip=false` flags: `-Dcoreit.test.skip=false`, `-Dpostman.test.skip=false`, `-Dkarate.test.skip=false`.

### E2E Tests (Playwright)

```bash
just test-e2e-node                                          # full suite — spins up Docker automatically
just test-e2e-node-specific test=login.spec.ts              # single spec file
just test-e2e-node-debug-ui test=login.spec.ts              # Playwright UI mode

# Frontend developer approach — runs against nx serve at :4200
cd e2e/dotcms-e2e-node/frontend
yarn install --frozen-lockfile && yarn playwright-install   # first-time setup
yarn run start-dev                                          # tests against :4200 (requires backend on :8080)
yarn run start-local                                        # tests against :8080
```

> See `e2e/dotcms-e2e-node/README.md` for full E2E documentation and options.

## References

- `justfile` — run with `just <command>` or read as a Maven command reference
- `docs/README.md` — full documentation index (Java, Angular, testing, REST, Docker, CI/CD)
- `docs/backend/REST_API_PATTERNS.md` — REST endpoints require dotCMS-specific auth init and response wrapping; generic JAX-RS patterns are incomplete here
- `core-web/AGENTS.md` — frontend standards and Nx commands
- `.cursor/rules/` — domain-specific rules loaded by file pattern (Java, frontend, tests, docs)
- `docs/infrastructure/CURSOR_CLOUD_SETUP.md` — Docker daemon setup for Cursor Cloud / headless Linux VMs
- `e2e/dotcms-e2e-node/README.md` — E2E test setup, Maven and frontend-developer approaches, Playwright options

## Gotchas

- **Pre-commit hooks require SDKMAN** — use `--no-verify` on commits in environments without it
- **Frontend memory** — standalone Nx builds may OOM; set `NODE_OPTIONS="--max_old_space_size=4096"`
- **Frontend changes only show at `:4200`** — `core-web/` edits are served live by `nx serve` at `:4200/dotAdmin`. They do NOT appear at `:8080/dotAdmin`, which serves the Angular WAR compiled into the Docker image at build time. If changes are invisible, check which URL you're using.
- **E2E tests are silently skipped** — like integration tests, requires `-De2e.test.skip=false` or nothing runs
