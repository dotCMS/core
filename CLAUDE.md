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
sdk env install   # Java 25 via SDKMAN (.sdkmanrc) — build fails with wrong version
nvm use           # Node 22.15+ via nvm (.nvmrc) — frontend build fails with wrong version
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
cd core-web && yarn nx serve dotcms-ui   # Frontend dev server only (use yarn nx, not nx)
```

> All test modules need explicit `skip=false` flags or tests are silently skipped.

## Essential Java Patterns

```java
import com.dotmarketing.util.Config;        // Config.getStringProperty("key", "default")
import com.dotmarketing.util.Logger;        // Logger.info(this, "message")
import com.dotmarketing.util.UtilMethods;   // UtilMethods.isSet(value)
UserAPI userAPI = APILocator.getUserAPI();   // Service access pattern
```

## Critical Rules

- **Config/Logger only**: Never `System.out`, `System.getProperty`, or `System.getenv`
- **Maven versions**: Add to `bom/application/pom.xml` ONLY, never `dotCMS/pom.xml`
- **Java version**: Core modules compile to Java 25 by default (`dotcms.core.compiler.release`; override e.g. `-Ddotcms.core.compiler.release=11` for older bytecode). Java 25 runtime. CLI may target lower for portability.
- **Security**: No hardcoded secrets, validate all input, never log sensitive data
- **REST @Schema**: Must match actual return type — see [REST API Guide](dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md)
- **Frontend**: See [core-web/CLAUDE.md](core-web/CLAUDE.md) for Angular/TypeScript standards

### OpenAPI / Swagger

`openapi.yaml` is **auto-generated** by `swagger-maven-plugin` at compile phase — it writes directly to `src/main/webapp/WEB-INF/openapi/openapi.yaml`. The CI verifies the committed file matches what the build produces.

- All description changes must go in Java `@Operation` / `@Parameter` annotations, not in the yaml directly
- Regenerate after annotation changes: `./mvnw compile -pl :dotcms-core -DskipTests` (no Docker needed)
- Commit the regenerated yaml alongside the Java changes

### Progressive Enhancement

When editing ANY code, improve incrementally:
- Add missing generics: `List<String>` not `List`
- Replace legacy: `Logger.info()` not `System.out.println()`
- Modern Angular: `@if` not `*ngIf`, `input()` not `@Input()`
- Add missing annotations: `@Override`, `@Nullable`

## Tech Stack

- **Backend**: Java 25 (runtime + core compile target, override-able), Maven, Spring/CDI
- **Frontend**: Angular 19+, Nx, PrimeNG, Tailwind CSS, Jest/Spectator — [core-web/CLAUDE.md](core-web/CLAUDE.md)
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
- [REST API Patterns](docs/backend/REST_API_PATTERNS.md) — JAX-RS, Swagger, @Schema rules
- [Maven Build System](docs/backend/MAVEN_BUILD_SYSTEM.md) — Dependency management
- [Configuration Patterns](docs/backend/CONFIGURATION_PATTERNS.md) — Config.getProperty() usage
- [Database Patterns](docs/backend/DATABASE_PATTERNS.md) — DotConnect, transactions
- [Health Monitoring](docs/backend/HEALTH_MONITORING.md) — Health endpoints, log levels

### Frontend Development (Angular/TypeScript)
- [Angular Standards](docs/frontend/ANGULAR_STANDARDS.md) — Modern syntax, signals, components
- [Testing Frontend](docs/frontend/TESTING_FRONTEND.md) — Spectator patterns, Jest config
- [Component Architecture](docs/frontend/COMPONENT_ARCHITECTURE.md) — Structure, organization
- [Styling Standards](docs/frontend/STYLING_STANDARDS.md) — SCSS, BEM, Tailwind

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
