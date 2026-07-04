# dotCMS Core — Engineering Onboarding

Welcome to **dotCMS/core**. This guide gets you from a fresh clone to your first
merged PR. It complements (does not replace) [`CLAUDE.md`](CLAUDE.md), which is the
canonical reference for build commands, project structure, and coding patterns.

> **How to use this with Claude Code:** open the repo and ask Claude things like
> *"help me set up my environment"*, *"run just the FooTest integration test"*, or
> *"walk me through opening my first PR"*. The repo-specific skills listed below are
> aware of dotCMS conventions.

> **See also:** [`dotFrontendOnboarding.md`](dotFrontendOnboarding.md) and
> [`dotBackendOnboarding.md`](dotBackendOnboarding.md) for deeper, discipline-specific
> onboarding.

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
  install** — they come up in containers.
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

### Common day-one snags

| Symptom | Likely cause | Fix |
|---|---|---|
| Weird compile errors | Wrong JDK | `sdk env install`, confirm `java -version` is 25 |
| Frontend build fails immediately | Wrong Node | `nvm use` from repo root |
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
├── core-web/               # Frontend: Angular 21+ / Nx monorepo (see core-web/CLAUDE.md)
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

### API tests — Postman & Karate

dotCMS uses two API testing frameworks: **Postman** (legacy) and **Karate** (the
modern replacement, in `test-karate/` — prefer it for new API tests).

```bash
# Postman
just test-postman ai          # a single collection (recommended)
just test-postman all         # all collections (slower)

# Karate
just test-karate                          # default collection
just test-karate KarateCITests#defaults   # a specific runner/collection
just test-karate-ide                      # boot services to run Karate from your IDE
```

More detail: [`docs/testing/API_TESTING.md`](docs/testing/API_TESTING.md).

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
- **Feature flags:** flags are **on by default**. To turn one off, set it to `false`
  in your docker-compose file or in `dotmarketing-config.properties`. When a feature
  ships, its `FF=false` entry must be **removed** from `dotmarketing-config.properties`
  — removing it enables the implemented feature by default.
  - When referencing a flag, add the **`DOT_`** prefix to its name (e.g. the flag
    `FEATURE_FLAG_SEO_IMPROVEMENTS` becomes `DOT_FEATURE_FLAG_SEO_IMPROVEMENTS`).
  - The list of available flags lives in
    [`dotCMS/src/main/java/com/dotcms/featureflag/FeatureFlagName.java`](dotCMS/src/main/java/com/dotcms/featureflag/FeatureFlagName.java).
- **Starter site:** a **starter** is a ZIP of seed content (sites, content types,
  pages, assets) that dotCMS loads on first startup to give you a populated instance
  instead of an empty one. Published starters live in our Artifactory:
  [`repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/`](https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/).
  To pick one, set the `<starter.deploy.version>` property in `parent/pom.xml`, drop a
  renamed `starter.zip` into `dotCMS/target/starter/`, or point `DOT_STARTER_DATA_LOAD`
  at a starter URL. Full details in [`dotBackendOnboarding.md`](dotBackendOnboarding.md).
- **Edit JSPs without rebuilding:** mount the webapp html dir into the Tomcat root
  via a docker-compose volume (see [`docs/infrastructure/`](docs/infrastructure/) /
  the frontend docker-compose for the exact mapping).
- **Override language files:** mount your local
  `dotCMS/src/main/webapp/WEB-INF/messages` into the container the same way.

---

## 6. Your first PR — end to end

1. **One-time setup — GPG commit signing.** Before your first commit, configure GPG
   signing so your commits show as **Verified** on GitHub — including commits made on
   your behalf by Claude Code. Follow
   [`docs/claude/GPG_COMMIT_SIGNING.md`](docs/claude/GPG_COMMIT_SIGNING.md).
2. **Branch** from up-to-date `main` following the naming convention in
   [`docs/core/GIT_WORKFLOWS.md`](docs/core/GIT_WORKFLOWS.md).
3. **Commit** using conventional commits (`feat:`, `fix:`, `docs:`, …).
4. **Open the PR** linked to its issue. Expectations and the PR template are in
   [`docs/core/GITHUB_ISSUE_MANAGEMENT.md`](docs/core/GITHUB_ISSUE_MANAGEMENT.md).
5. **CI / merge queue:** dotCMS uses **GitHub's native merge queue** — the final gate
   before code lands on `main`. An approved, ready PR isn't merged directly. It's added
   to a queue where GitHub creates a temporary `merge_group` branch containing your PR
   plus any PRs already ahead of it, then runs the merge-queue workflow against that
   combined code. Only if it passes does GitHub fast-forward `main`.
   - **Some required checks to enter the queue:** Unit / Integration / Postman test
     validation (test run green), security checks for code vulnerabilities, and at
     least **1 reviewer approval**.
   - **Typical wait:** ~1 hour average to merge.
   - Pipeline reference: [`docs/core/CICD_PIPELINE.md`](docs/core/CICD_PIPELINE.md).
6. **When CI fails:** ask Claude Code to run the **`cicd-diagnostics`** skill — it
   knows how to read dotCMS build failures and flaky tests.

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

- **Slack channels:**
  - `#eng` — general engineering.
  - `#eng-adrs` — ADR (architecture decision record) discussions.
  - `#be-code-review` — look here for PR review requests ("likes").
  - `#guild-*` — topic-specific discussions (per guild).
  - `#feat-*` — feature-related discussions.
  - `#team-*` — team-specific discussions.
- **Everything else** (who owns what, runbooks, issue tracker &
  boards, release process & cadence): see our
  **How we do Engineering** doc.

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

*Maintained by the Engineering Team. Spot something stale or wrong? Open a PR against
this file and ping the `#eng` or `#be-code-review` channels.*
