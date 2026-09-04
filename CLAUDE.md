# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

```
core/
├── dotCMS/                          # Main backend Java code
│   └── src/main/java/com/
│       ├── dotcms/                  # Modern domain-driven packages (prefer these)
│       └── dotmarketing/            # Legacy packages (15+ yr old code, still active)
├── core-web/                        # Frontend (Angular/Nx monorepo) → see core-web/CLAUDE.md
├── dotcms-integration/              # Integration tests
├── dotcms-postman/                  # Postman API tests
├── bom/application/pom.xml          # Dependency versions (ONLY place for versions)
├── parent/pom.xml                   # Plugin management
└── .github/workflows/               # CI/CD pipelines
```

## Environment Prerequisites

```bash
sdk env install   # installs the Java version pinned in .sdkmanrc — build fails with wrong version
nvm use           # installs the Node version pinned in .nvmrc — frontend build fails with wrong version
```

## Build & Test Commands

```bash
# Build (choose based on scope)
./mvnw install -pl :dotcms-core --am -DskipTests          # Core + in-project deps (~2-3 min) ✅
./mvnw install -pl :dotcms-core -DskipTests                # ⚠️ Can fail: missing in-project deps
./mvnw clean install -DskipTests                            # Full rebuild (~8-15 min)
./mvnw clean install -DskipTests -Ddocker.skip             # Full rebuild, skip Docker image

# Test (⚠️ NEVER run full integration suite — 60+ min)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass        # Specific class
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest#testMethod  # Specific method
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=all       # Postman

# IDE Testing (fastest iteration)
just test-integration-ide     # Start PostgreSQL + Elasticsearch + dotCMS
just test-integration-stop    # Stop services when done

# Run
just dev-run                         # Start dotCMS in Docker with Glowroot
cd core-web && pnpm nx serve dotcms-ui   # Frontend dev server only (Nx is not global — always via pnpm)
```

> All test modules need explicit `skip=false` flags or tests are silently skipped.

## Essential Java Patterns

```java
import com.dotmarketing.util.Config;        // Config.getStringProperty("key", "default")
import com.dotmarketing.util.Logger;        // Logger.info(this, "message")
import com.dotmarketing.util.UtilMethods;   // UtilMethods.isSet(value)
UserAPI userAPI = APILocator.getUserAPI();   // Service access pattern
```

> **Batch permission filtering**: prefer `permissionAPI.filterCollection(Collection<P>, int, User, boolean)` over per-item `doesUserHavePermission` loops — one SQL round-trip vs N. See [Java Standards → Permission Checks](docs/backend/JAVA_STANDARDS.md#permission-checks--batch-vs-scalar).

## Critical Rules

- **Config/Logger only**: Never `System.out`, `System.getProperty`, or `System.getenv`
- **Maven versions**: Add to `bom/application/pom.xml` ONLY, never `dotCMS/pom.xml`
- **Java version**: see `.sdkmanrc` for the runtime version. Core modules compile to whatever `dotcms.core.compiler.release` is set to in `parent/pom.xml` (override e.g. `-Ddotcms.core.compiler.release=11` for older bytecode); `tools/dotcms-cli` targets whatever `maven.compiler.release` is set to in its own `pom.xml`, historically lower for portability.
- **Security**: No hardcoded secrets, validate all input, never log sensitive data
- **REST @Schema**: Must match actual return type — see [REST API Guide](dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md)
- **Integration test registration**: A new integration test class not added to a `MainSuite*`/`Junit5Suite*` `@SuiteClasses` list compiles fine but is **silently never run in CI** (green build, zero coverage) — it only runs locally via `-Dit.test=`. See [Integration Tests → Registering Tests in a MainSuite](docs/testing/INTEGRATION_TESTS.md#registering-tests-in-a-mainsuite-ci-gate).
- **Frontend**: See [core-web/CLAUDE.md](core-web/CLAUDE.md) for Angular/TypeScript standards

### OpenAPI / Swagger

`openapi.yaml` is **auto-generated** by `swagger-maven-plugin` at compile phase — it writes directly to `src/main/webapp/WEB-INF/openapi/openapi.yaml`. The CI verifies the committed file matches what the build produces.

- All description changes must go in Java `@Operation` / `@Parameter` annotations, not in the yaml directly
- Regenerate after annotation changes: `./mvnw compile -pl :dotcms-core --am -DskipTests` (no Docker needed; `--am` avoids the same missing-in-project-deps failure noted above)
- Commit the regenerated yaml alongside the Java changes

### Progressive Enhancement

When editing ANY code, improve incrementally:
- Add missing generics: `List<String>` not `List`
- Replace legacy: `Logger.info()` not `System.out.println()`
- Modern Angular: `@if` not `*ngIf`, `input()` not `@Input()`
- Add missing annotations: `@Override`, `@Nullable`

## Spec-Driven Development (Spec-Kit)

This repo uses [GitHub Spec-Kit](https://github.com/github/spec-kit) for spec-driven work,
customized for dotCMS. How to run it: [Spec-Kit Quick Start](docs/core/SPEC_KIT_QUICK_START.md).
How it's built + upgrade re-apply notes: [.specify/CUSTOMIZATIONS.md](.specify/CUSTOMIZATIONS.md).

- **Flow**: `/speckit-specify` (new feature) **or** `/speckit-specify-fix` (issue/bug resolution) → **PR 1 (spec) approved** → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement` → PR 2 (implementation).
- **Two PRs, gated on approval — not merge**: PR 1 carries `spec.md` **alone** and another dev must **approve** it before `/speckit-plan` runs. Do **not** wait for PR 1 to merge — branch off the spec branch (the spec isn't on `main` yet) and open PR 2 with the implementation. If the spec changes after sign-off, get it re-approved. See [Quick Start §3](docs/core/SPEC_KIT_QUICK_START.md).
- **Constitution**: [.specify/memory/constitution.md](.specify/memory/constitution.md) — legacy-awareness + Critical Rules; loaded by every skill.
- **TDD (Principle V, non-negotiable)**: no implementation code before tests are written, **dev-approved**, and confirmed **failing (Red)**. If a test type can't be done, the dev must say so and why. Enforced in the constitution + `tasks-template` `[GATE]` tasks + plan Test Strategy.
- **ADRs**: live only in the private repo `dotCMS/platform-adrs`. `/speckit-plan` **always consults** relevant ADRs (auto `before_plan` hook → `/speckit-adr-context`, read-only via `gh`). Spec-Kit **never creates ADRs** — it only *proposes* them; ADRs are authored in `platform-adrs` via its `new-adr.sh`.

## Tech Stack

- **Backend**: Java (see `.sdkmanrc` / `parent/pom.xml`'s `dotcms.core.compiler.release`, override-able), Maven, CDI
- **Frontend**: Angular (see `core-web/package.json`'s `@angular/core`), Nx, PrimeNG, Tailwind CSS, Jest/Spectator — [core-web/CLAUDE.md](core-web/CLAUDE.md)
- **Infrastructure**: Docker, PostgreSQL, Elasticsearch, GitHub Actions

## Documentation (Load On-Demand)

### Core Architecture & Workflows
- [Architecture Overview](docs/core/ARCHITECTURE_OVERVIEW.md) — System design, modules, patterns
- [Git Workflows](docs/core/GIT_WORKFLOWS.md) — Branch naming, PR process, conventional commits
- [CI/CD Pipeline](docs/core/CICD_PIPELINE.md) — Build process, testing, deployment
- [Security Principles](docs/core/SECURITY_PRINCIPLES.md) — Input validation, secrets, logging
- [GitHub Issue Management](docs/core/GITHUB_ISSUE_MANAGEMENT.md) — Issues, PRs, epics
- [Rollback-Unsafe Change Categories](docs/core/ROLLBACK_UNSAFE_CATEGORIES.md) — DB schema, ES mapping, API contract risks

### Backend Development (Java/Maven)
- [Java Standards](docs/backend/JAVA_STANDARDS.md) — Coding patterns, immutables, exceptions, utilities
- [When to Use Virtual Threads](docs/backend/VIRTUAL_THREADS.md) — Socket I/O yes, file I/O no; carrier pinning
- [REST API Patterns](docs/backend/REST_API_PATTERNS.md) — JAX-RS, Swagger, @Schema rules
- [Maven Build System](docs/backend/MAVEN_BUILD_SYSTEM.md) — Dependency management
- [Configuration Patterns](docs/backend/CONFIGURATION_PATTERNS.md) — Config.getProperty() usage
- [Database Patterns](docs/backend/DATABASE_PATTERNS.md) — DotConnect, transactions
- [Health Monitoring](docs/backend/HEALTH_MONITORING.md) — Health endpoints, log levels
- [Security Patterns](docs/backend/SECURITY_BACKEND.md) — Input validation, auth, SQL/XSS prevention, secure logging
- [Search API Migration](docs/backend/SEARCH_API_MIGRATION.md) — ES → OpenSearch: deprecated `ContentletAPI` search methods, plugin migration guide
- [Telemetry Implementation](docs/backend/TELEMETRY_IMPLEMENTATION.md) — CDI-based metrics system, creating new metrics, `/v1/usage` endpoints
- [Jandex Metadata Scanning](docs/backend/JANDEX_METADATA_SCANNING.md) — Fast class/annotation metadata lookup, prefer over reflection
- **ES → OpenSearch Migration** — infra migration from ElasticSearch to OpenSearch, phased dual-write/read rollout
  - [Migration Design](docs/backend/OPENSEARCH_MIGRATION.md) — Architecture, phased rollout, configuration
  - [Migration Test Plan](docs/backend/OPENSEARCH_MIGRATION_TEST_PLAN.md) — QA test plan for the migration phases
  - [Client Configuration](docs/backend/OPENSEARCH_CLIENT_CONFIGURATION.md) — `OS_*`/`ES_*` config property reference and fallback chain
  - [Migration Tester Guide](docs/backend/OPENSEARCH_MIGRATION_TESTER_GUIDE.md) — Getting-started guide for QA testers validating the migration

### Frontend Development (Angular/TypeScript)
- **[docs/frontend/README.md](docs/frontend/README.md) — index of all frontend docs and when to load each. Start here if unsure.**
- [Angular Standards](docs/frontend/ANGULAR_STANDARDS.md) — **single source of truth**: syntax, signals, change detection, forms, icons
- [Component Architecture](docs/frontend/COMPONENT_ARCHITECTURE.md) — Structure, file layout, data flow
- [State Management](docs/frontend/STATE_MANAGEMENT.md) — NgRx Signal Store, rxMethod, patchState
- [Styling Standards](docs/frontend/STYLING_STANDARDS.md) — Tailwind, PrimeNG theme, BEM, SCSS
- [TypeScript Standards](docs/frontend/TYPESCRIPT_STANDARDS.md) — Strict types, as const, `#` private
- [Testing Frontend](docs/frontend/TESTING_FRONTEND.md) — Writing tests: Spectator, Jest, byTestId
- [Testing Review Rules](docs/frontend/TESTING_REVIEW_RULES.md) — Reviewing tests: violation checklist
- [Breadcrumbs](docs/frontend/BREADCRUMBS.md) — GlobalStore breadcrumb trail

### Testing
- [Backend Unit Tests](docs/testing/BACKEND_UNIT_TESTS.md) — JUnit, integration patterns
- [Integration Tests](docs/testing/INTEGRATION_TESTS.md) — API testing, database setup
- [E2E Tests](docs/testing/E2E_TESTS.md) — Playwright, user workflows

### Infrastructure
- [Docker Build Process](docs/infrastructure/DOCKER_BUILD_PROCESS.md) — Container setup, optimization

## Context Management

### For Claude
- Use this guide for always-available context
- Load `/docs/` files on-demand with Read tool
- Use `/clear` between different work contexts

### For Cursor
- Project rules: `.cursor/rules/` (`.mdc` files with globs); see `.cursor/rules/README.md`
- Use `@docs/path/file.md` syntax for detailed patterns
- Domain-specific rules load by file pattern (Java, Angular, tests, docs)

## Documentation Maintenance

- **CLAUDE.md**: Navigation hub + essential quick-reference only
- **`/docs/`**: Full patterns by domain — single source of truth
- **`.cursor/rules/`**: Short reminders with globs, link to `/docs/`
- When patterns are missing: update the relevant `/docs/{domain}/` file, not this file
