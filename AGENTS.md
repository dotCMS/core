# dotCMS Development Guide

## Build, test, run

```bash
# Full build (backend + frontend + Docker image, ~8-15 min)
./mvnw install -DskipTests                        # or: just build

# Quick core-only rebuild (~2-3 min)
./mvnw install -pl :dotcms-core -DskipTests        # or: just build-quicker

# Core + its dependencies (~3-5 min)
./mvnw install -pl :dotcms-core --am -DskipTests

# Start dotCMS (Docker-managed DB, OpenSearch, app container)
just dev-start-on-port 8082
just dev-stop

# Frontend (from core-web/)
cd core-web && npx nx serve dotcms-ui              # dev server on :4200
cd core-web && npx nx lint dotcms-ui
cd core-web && npx nx test dotcms-ui
```

### Testing

Target specific classes — never run the full suite (~60 min):

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai
```

Tests are **silently skipped** without explicit `skip=false` flags: `-Dcoreit.test.skip=false`, `-Dpostman.test.skip=false`, `-Dkarate.test.skip=false`.

## Critical rules

- **Config/Logger only**: `Config.getStringProperty()`, `Logger.info(this, ...)` — never `System.out` or `System.getProperty`
- **Maven versions**: declare in `bom/application/pom.xml` only, never in `dotCMS/pom.xml`
- **No hardcoded secrets**: use `Config` for sensitive values, never log passwords/tokens
- **Angular modern syntax**: `@if`/`@for`, `input()`/`output()`, `data-testid` — never `*ngIf`/`@Input()`
- **Conventional commits**: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`
- **Branch naming**: `issue-{number}-description` for automatic issue linking

## Tech stack

- **Backend**: Java 21 runtime with Java 11 release compatibility (core). Migrating to Java 25 — parallel CI workflows override the Java version to validate forward compatibility. See `.sdkmanrc` for the current default JDK.
- **Frontend**: Angular 20+, Nx monorepo, PrimeNG, NgRx signals, Jest/Spectator — see `core-web/CLAUDE.md`
- **Infrastructure**: Docker, PostgreSQL (pgvector), OpenSearch, GitHub Actions

## Documentation (load on demand)

| When working on… | Read |
|---|---|
| Java patterns, APIs, services | `docs/backend/JAVA_STANDARDS.md` |
| Maven deps, build config | `docs/backend/MAVEN_BUILD_SYSTEM.md` |
| REST endpoints, Swagger/OpenAPI | `docs/backend/REST_API_PATTERNS.md` |
| Database queries, transactions | `docs/backend/DATABASE_PATTERNS.md` |
| Angular components, signals | `docs/frontend/ANGULAR_STANDARDS.md` |
| Frontend tests (Spectator/Jest) | `docs/frontend/TESTING_FRONTEND.md` |
| Integration/API tests | `docs/testing/INTEGRATION_TESTS.md` |
| Docker build, images | `docs/infrastructure/DOCKER_BUILD_PROCESS.md` |
| Git workflow, PRs, issues | `docs/core/GIT_WORKFLOWS.md` |
| CI/CD pipeline | `docs/core/CICD_PIPELINE.md` |

Full index: `docs/README.md`. Frontend specifics: `core-web/CLAUDE.md`. REST API detail: `dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md`. Common Maven commands and aliases: `justfile` (run directly with `just <command>` or read as a syntax reference).

## Cursor Cloud specific instructions

### Starting services

1. Start Docker: `sudo dockerd &>/tmp/dockerd.log &` then `sudo chmod 666 /var/run/docker.sock`
2. Build: `./mvnw install -DskipTests`
3. Run: `just dev-start-on-port 8082`
4. Admin UI: `http://localhost:8082/dotAdmin` — `admin@dotcms.com` / `admin`

### Gotchas

- **Docker storage driver must be `vfs`** — `fuse-overlayfs` causes dpkg-divert failures during image build. Config: `/etc/docker/daemon.json` → `{"storage-driver": "vfs"}`
- **iptables must be legacy** — `sudo update-alternatives --set iptables /usr/sbin/iptables-legacy`
- **Pre-commit hooks require SDKMAN** — use `--no-verify` on commits in Cloud VMs
- **Frontend OOM** — standalone NX builds need `NODE_OPTIONS="--max_old_space_size=4096" --parallel=2`. Maven handles this internally.
