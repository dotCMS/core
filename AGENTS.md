# dotCMS

Headless/hybrid CMS — Java 21 backend (Maven), Angular frontend (Nx/Yarn), Docker infrastructure. Migrating to Java 25 with parallel CI workflows; core source uses Java 11 release compatibility.

> **Note:** `CLAUDE.md` is a symlink to this file (`CLAUDE.md → AGENTS.md`). Claude Code reads `CLAUDE.md`; Codex, Aider, and Cursor read `AGENTS.md`. They are the same content.

## Build, test, run

```bash
./mvnw install -DskipTests                        # full build (~8-15 min), or: just build
./mvnw install -pl :dotcms-core -DskipTests        # core only (~2-3 min), or: just build-quicker

just dev-start-on-port 8080                        # start stack (DB + OpenSearch + app) → :8080/dotAdmin
just dev-stop                                      # teardown

cd core-web && npx nx serve dotcms-ui              # frontend dev server :4200, proxies API to backend :8080
cd core-web && npx nx lint dotcms-ui               # frontend lint
cd core-web && npx nx test dotcms-ui               # frontend test
```

> **First run:** build first (`just build`), then start — the build produces the Docker image the start command uses.
>
> **Build scope:** Match scope to what changed — core-only change → `just build-quicker`; multi-module → add `--am`; full rebuild → `just build`. After a core rebuild, restart to pick up the new image: `just dev-stop && just dev-start-on-port 8080`. Silent failures are rare; slow builds are common.
>
> **Frontend dev:** the `nx serve` dev server proxies API calls to the running backend on `:8080` — no backend rebuild needed for frontend-only changes. Backend must be up for API calls to work.

### Testing

Target specific classes — never run the full suite (~60 min):

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
```

Tests are **silently skipped** without explicit `skip=false` flags: `-Dcoreit.test.skip=false`, `-Dpostman.test.skip=false`, `-Dkarate.test.skip=false`.

## References

- `justfile` — run with `just <command>` or read as a Maven command reference
- `docs/README.md` — full documentation index (Java, Angular, testing, REST, Docker, CI/CD)
- `docs/backend/REST_API_PATTERNS.md` — REST endpoints require dotCMS-specific auth init and response wrapping; generic JAX-RS patterns are incomplete here
- `core-web/AGENTS.md` — frontend standards and Nx commands
- `.cursor/rules/` — domain-specific rules loaded by file pattern (Java, frontend, tests, docs)
- `docs/infrastructure/CURSOR_CLOUD_SETUP.md` — Docker daemon setup for Cursor Cloud / headless Linux VMs

## Gotchas

- **Pre-commit hooks require SDKMAN** — use `--no-verify` on commits in environments without it
- **Frontend memory** — standalone Nx builds may OOM; set `NODE_OPTIONS="--max_old_space_size=4096"`
