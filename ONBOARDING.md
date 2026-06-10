# dotCMS Core — Engineering Onboarding

Welcome to **dotCMS/core**. This guide gets you from a fresh clone to your first
merged PR. It complements (does not replace) [`CLAUDE.md`](CLAUDE.md), which is the
canonical reference for build commands, project structure, and coding patterns.

> **How to use this with Claude Code:** open the repo and ask Claude things like
> *"help me set up my environment"*, *"run just the FooTest integration test"*, or
> *"walk me through opening my first PR"*. The repo-specific skills listed below are
> aware of dotCMS conventions.

> ⚠️ **Fill-in markers:** anything in `<<ANGLE BRACKETS>>` is a placeholder the
> owning team must replace with real values (Slack channels, doc links, etc.).
> Nothing secret (proxy passwords, tokens, internal IPs) belongs in this file —
> link to where credentials live instead.

---

## 1. Day-one environment setup

### Prerequisites (versions are enforced — wrong versions fail the build)

```bash
sdk env install   # Java 25 via SDKMAN, pinned in .sdkmanrc
nvm use           # Node 22.15+ via nvm, pinned in .nvmrc
```

- **Java:** core modules compile to and run on Java 25. A wrong JDK gives cryptic
  compile errors — check `java -version` first when a build misbehaves.
- **Node:** the `core-web/` frontend build fails on the wrong Node version. Always
  `nvm use` from the repo root before touching frontend code.
- **Docker:** required for `just dev-run` and Docker-backed tests. dotCMS runs in
  Docker by default, so **you do not need a local PostgreSQL or Elasticsearch
  install** — they come up in containers. On Apple Silicon,
  `<<DOCKER RUNTIME — e.g. Docker Desktop / Colima / Rancher; note memory + CPU settings the team recommends>>`.
- **`just`:** the command runner used throughout this guide.
  ```bash
  brew install just
  ```

### Recommended IDEs

- **Backend (Java):** IntelliJ IDEA — open the root `pom.xml` to import.
- **Frontend (Angular/TS):** VS Code or Cursor, optionally with the **Nx Console** extension.

### Install dependencies + first build

```bash
just install-all-mac-deps     # installs/validates Git, JDK, Docker, etc. (macOS)
```

Then build (pick by scope):

```bash
# Core + in-project deps — the everyday build (~2-3 min) ✅
./mvnw install -pl :dotcms-core --am -DskipTests

# Equivalent via just
just build-select-module dotcms-core

# Full rebuild, skip Docker image (~8-15 min)
just build-no-docker          # = ./mvnw clean install -DskipTests -Ddocker.skip
```

> `./mvnw install -pl :dotcms-core -DskipTests` (without `--am`) can fail with
> missing in-project deps — prefer the `--am` form or `just build-select-module`.

### Run it locally

```bash
# Backend in Docker (PostgreSQL + Elasticsearch + dotCMS)
just dev-start-on-port 8080   # default port is 8082 if omitted; `just dev-start` picks a random port
just dev-stop                 # stop it
just dev-run                  # run with Glowroot profiler

# Frontend dev server (use `yarn nx`, never bare `nx`)
cd core-web && yarn nx serve dotcms-ui
# served at http://localhost:4200/dotAdmin
```

### Enterprise proxy / network setup

If you're behind the corporate proxy or VPN, configure these **before** your first
build or the dependency download will hang/fail:

- **Maven:** proxy + mirror config in `~/.m2/settings.xml` — `<<LINK TO TEAM settings.xml TEMPLATE>>`
- **npm/yarn registry:** `<<INTERNAL REGISTRY URL + auth instructions>>`
- **Docker registry auth:** `<<docker login HOST + where creds live, e.g. 1Password / Vault>>`
- **TLS/cert bundle:** `<<corporate root CA import steps for the JDK truststore + Node, if applicable>>`

> Credentials live in `<<SECRETS MANAGER — e.g. 1Password vault / Vault path>>`.
> Never commit them or paste them into this file.

### Common day-one snags

| Symptom | Likely cause | Fix |
|---|---|---|
| Weird compile errors | Wrong JDK | `sdk env install`, confirm `java -version` is 25 |
| Frontend build fails immediately | Wrong Node | `nvm use` from repo root |
| Maven hangs downloading deps | Proxy not configured | See proxy section above |
| `nx: command not found` / odd nx behavior | Used bare `nx` | Use `yarn nx ...` |
| Build "missing dependency" | Forgot `--am` | `./mvnw install -pl :dotcms-core --am -DskipTests` |
| **Puppeteer Chromium ARM64 build failure** (Apple Silicon, `dotcms-core-web` FAILURE) | Puppeteer can't fetch an arm64 Chromium | See "Apple Silicon / Puppeteer" below |

#### Apple Silicon / Puppeteer Chromium fix

On M-series Macs the frontend build can fail with *"The chromium binary is not
available for arm64"*. Fix it by pointing Puppeteer at a Homebrew Chromium and
skipping its download (add the exports to your `~/.zshrc` / `~/.bashrc`):

```bash
brew install chromium
export PUPPETEER_EXECUTABLE_PATH=$(which chromium)
export PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
source ~/.zshrc        # or ~/.bashrc
```

---

## 2. Mental model of the codebase

```
core/
├── dotCMS/                 # Backend Java
│   └── src/main/java/com/
│       ├── dotcms/         # Modern, domain-driven — PREFER THIS for new code
│       └── dotmarketing/   # Legacy (15+ yrs), still very much alive
├── core-web/               # Frontend: Angular 19+ / Nx monorepo (see core-web/CLAUDE.md)
├── dotcms-integration/     # Integration tests (DB + Elasticsearch)
├── dotcms-postman/         # Postman API tests
├── bom/application/pom.xml # THE place for dependency versions
└── parent/pom.xml          # Maven plugin management
```

**Two things to internalize early:**

1. **`com.dotcms.*` vs `com.dotmarketing.*`** — new work goes in `dotcms.*`. You'll
   still read and patch `dotmarketing.*` constantly; touch it surgically and follow
   its existing patterns rather than modernizing wholesale mid-PR.
2. **Backend vs frontend are separate build worlds.** Many tasks are backend-only or
   frontend-only. Know which one your change lives in before you start.

Deeper architecture: [`docs/core/ARCHITECTURE_OVERVIEW.md`](docs/core/ARCHITECTURE_OVERVIEW.md).

---

## 3. Testing — fast iteration without the 60-minute trap

> ⚠️ **Never run the full integration suite** (`dotcms-integration`) locally — it's
> 60+ minutes. Always target a class or method. Test modules also **silently skip**
> unless you pass the explicit `skip=false` flag.

### Backend integration tests

```bash
# One integration test class / method
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest#testMethod
```

**Fastest loop (IDE):** boot services once, then run/debug individual tests with
breakpoints from IntelliJ.

```bash
just test-integration-ide     # boots PostgreSQL + Elasticsearch + dotCMS
# → run/debug your test class in the IDE
just test-integration-stop    # tear down when done
```

**Debug the running app:** `just dev-run-debug-suspend` starts dotCMS in Docker
waiting on debug port **5005** for your IDE to attach.

### Postman API tests

```bash
just test-postman ai          # a single collection (recommended)
just test-postman all         # all collections (slower)
```

### Frontend tests

```bash
cd core-web && yarn nx test <project>     # Jest + Spectator
```

**Spectator conventions** (enforced in review):

```typescript
// ✅ select elements by data-testid
const button = spectator.query(byTestId('submit-button'));
// ✅ set inputs via the API, never assign the property directly
spectator.setInput('inputProperty', 'value');
// ✅ test user-visible behavior, not implementation details
spectator.click(byTestId('save-button'));
expect(spectator.query(byTestId('success-message'))).toBeVisible();
```

More detail: [`docs/testing/`](docs/testing/) and [`docs/frontend/TESTING_FRONTEND.md`](docs/frontend/TESTING_FRONTEND.md).

---

## 4. Repo-specific landmines (memorize these)

- **Dependency versions → `bom/application/pom.xml` ONLY.** Never add versions to
  `dotCMS/pom.xml`.
- **`openapi.yaml` is auto-generated.** Don't hand-edit it. Change the Java
  `@Operation` / `@Parameter` annotations, regenerate with
  `./mvnw compile -pl :dotcms-core -DskipTests`, and commit the regenerated yaml
  with your Java change. CI verifies they match.
- **Config & logging:** use `Config.getStringProperty(...)` and `Logger.info(this, ...)`.
  Never `System.out`, `System.getProperty`, or `System.getenv`.
- **REST `@Schema` must match the actual return type** — see
  [`dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md`](dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md).
- **Security:** no hardcoded secrets, validate all input, never log sensitive data.
- **Progressive enhancement:** when you edit a file, leave it a little better —
  add generics, replace legacy logging, use modern Angular (`@if` not `*ngIf`,
  `input()` not `@Input()`), add missing `@Override`/`@Nullable`.

---

## 5. Useful local configuration

- **Admin password:** `export DOT_INITIAL_ADMIN_PASSWORD=admin` before first run.
- **Feature flags:** enable features via `DOT_FEATURE_FLAG_*` env vars in your
  docker-compose (e.g. `DOT_FEATURE_FLAG_SEO_IMPROVEMENTS: true`). Which flags exist
  and what they gate: `<<ASK THE TEAM / LINK TO FLAG REFERENCE>>`.
- **Edit JSPs without rebuilding:** mount the webapp html dir into the Tomcat root
  via a docker-compose volume (see [`docs/infrastructure/`](docs/infrastructure/) /
  the frontend docker-compose for the exact mapping).
- **Override language files:** mount your local
  `dotCMS/src/main/webapp/WEB-INF/messages` into the container the same way.

---

## 6. Your first PR — end to end

1. **Branch** from up-to-date `main` following the naming convention in
   [`docs/core/GIT_WORKFLOWS.md`](docs/core/GIT_WORKFLOWS.md).
2. **Commit** using conventional commits (`feat:`, `fix:`, `docs:`, …).
3. **Open the PR** linked to its issue. Expectations and the PR template are in
   [`docs/core/GITHUB_ISSUE_MANAGEMENT.md`](docs/core/GITHUB_ISSUE_MANAGEMENT.md).
4. **CI / merge queue:** `<<DESCRIBE: required checks, how the merge queue works, who can approve, typical wait times>>`.
   Pipeline reference: [`docs/core/CICD_PIPELINE.md`](docs/core/CICD_PIPELINE.md).
5. **When CI fails:** ask Claude Code to run the **`cicd-diagnostics`** skill — it
   knows how to read dotCMS build failures and flaky tests.

**Good first issues:** `<<LINK TO good-first-issue LABEL OR STARTER BOARD>>`.

---

## 7. Claude Code skills available in this repo

You don't have to discover these the hard way — they encode dotCMS conventions:

- **`triage`** — triage GitHub issues (validate, dedupe, research) before they're worked.
- **`cicd-diagnostics`** — diagnose failing CI runs, broken builds, flaky tests, merge-queue blocks.
- **`gh-issue-troubleshoot`** — take a GitHub issue from description to a proposed code fix.
- **`dotcms-github-issues`** (create / find / query / update) — manage issues via repo templates.
- **`vtl-migration`** — migrate legacy VTL custom-field templates (Dojo/Dijit → `DotCustomFieldApi`).
- **`check-release-rollback`** — assess whether a release can be safely rolled back.
- **`angular-developer`** + Nx skills (`nx-generate`, `nx-run-tasks`, `nx-workspace`) — frontend work.

Ask Claude *"what skills can help with X?"* if you're unsure.

---

## 8. People, comms & where knowledge lives

> This is the highest-value section and the least discoverable by reading code.
> The owning team should keep it current.

- **Slack channels:** `<<#eng-general / #core-dev / #ci-alerts / #help — list the ones that matter>>`
- **Who owns what:** `<<LINK TO CODEOWNERS, team-to-area map, or on-call schedule>>`
- **Design docs / RFCs / runbooks:** `<<NOTION / CONFLUENCE SPACE LINK>>`
- **Issue tracker & boards:** `<<GITHUB PROJECT / JIRA LINK>>`
- **Where to ask "is this expected?"** `<<channel or person>>`
- **Release process & cadence:** `<<LINK>>`

---

## 9. Reference docs (load on demand)

- Architecture: [`docs/core/ARCHITECTURE_OVERVIEW.md`](docs/core/ARCHITECTURE_OVERVIEW.md)
- Git workflows: [`docs/core/GIT_WORKFLOWS.md`](docs/core/GIT_WORKFLOWS.md)
- CI/CD: [`docs/core/CICD_PIPELINE.md`](docs/core/CICD_PIPELINE.md)
- Security: [`docs/core/SECURITY_PRINCIPLES.md`](docs/core/SECURITY_PRINCIPLES.md)
- Backend (Java/Maven): [`docs/backend/`](docs/backend/) — start with
  [`JAVA_STANDARDS.md`](docs/backend/JAVA_STANDARDS.md) and [`MAVEN_BUILD_SYSTEM.md`](docs/backend/MAVEN_BUILD_SYSTEM.md)
- Frontend (Angular/TS): [`core-web/CLAUDE.md`](core-web/CLAUDE.md) and
  [`docs/frontend/`](docs/frontend/) — start with [`ANGULAR_STANDARDS.md`](docs/frontend/ANGULAR_STANDARDS.md)
- Testing: [`docs/testing/`](docs/testing/)
- The `justfile` at the repo root — the source of truth for `just` commands.

---

*Maintained by `<<OWNING TEAM>>`. Spot something stale or wrong? `<<HOW TO PROPOSE A FIX — PR against this file / ping channel>>`.*
