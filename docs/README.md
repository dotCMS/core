# dotCMS Documentation Index

Complete index of every file under `docs/` — 54 documents. If a doc is not listed here, it is not reachable, and neither a developer browsing nor an AI assistant loading context on demand will find it.

This file and root [`CLAUDE.md`](../CLAUDE.md) are the two entry points. `CLAUDE.md` carries always-loaded context and routes to the docs used most often in day-to-day work; this index covers **everything**, including the less-travelled corners. Both are checked by `scripts/validate-docs-reachability.py`, which fails if any file under `docs/` is unreachable from either.

Descriptions say **when to load** the doc, not what it contains.

---

## Core — architecture, process, and rules that cross every area

| Doc | Load it when |
|---|---|
| [ARCHITECTURE_OVERVIEW.md](core/ARCHITECTURE_OVERVIEW.md) | You need the system shape: modules, layering, where a subsystem lives |
| [SECURITY_PRINCIPLES.md](core/SECURITY_PRINCIPLES.md) | Handling input, secrets, or logging anything that might be sensitive |
| [PROGRESSIVE_ENHANCEMENT.md](core/PROGRESSIVE_ENHANCEMENT.md) | Editing legacy code and deciding how much to modernise without destabilising it |
| [ROLLBACK_UNSAFE_CATEGORIES.md](core/ROLLBACK_UNSAFE_CATEGORIES.md) | Changing DB schema, ES mappings, or an API contract — before deciding a release can be rolled back |
| [SDK_BREAKING_CHANGE_CATEGORIES.md](core/SDK_BREAKING_CHANGE_CATEGORIES.md) | Changing `@dotcms/client`, `@dotcms/react` or `@dotcms/angular` and deciding whether it breaks published consumers |
| [GIT_WORKFLOWS.md](core/GIT_WORKFLOWS.md) | Branch naming, conventional commits, opening a PR |
| [CICD_PIPELINE.md](core/CICD_PIPELINE.md) | Understanding or changing the GitHub Actions pipeline itself |
| [GITHUB_ISSUE_MANAGEMENT.md](core/GITHUB_ISSUE_MANAGEMENT.md) | Writing or managing issues, epics and subtasks by hand |
| [SPEC_KIT_QUICK_START.md](core/SPEC_KIT_QUICK_START.md) | Running spec-driven development: sizing the flow, the two-PR protocol, the TDD and ADR gates |

## Backend — Java, Maven, REST, data

| Doc | Load it when |
|---|---|
| [JAVA_STANDARDS.md](backend/JAVA_STANDARDS.md) | Writing any Java: coding patterns, immutables, exceptions, utilities, Javadoc, permission checks |
| [REST_API_PATTERNS.md](backend/REST_API_PATTERNS.md) | Adding or changing a JAX-RS resource — annotations, `WebResource.InitBuilder`, `@Schema` rules |
| [MAVEN_BUILD_SYSTEM.md](backend/MAVEN_BUILD_SYSTEM.md) | Adding a dependency or touching build configuration |
| [CONFIGURATION_PATTERNS.md](backend/CONFIGURATION_PATTERNS.md) | Reading configuration — `Config.getProperty()` and the resolution order |
| [DATABASE_PATTERNS.md](backend/DATABASE_PATTERNS.md) | Writing SQL, using `DotConnect`, or reasoning about transactions |
| [SECURITY_BACKEND.md](backend/SECURITY_BACKEND.md) | Input validation, auth, SQL/XSS prevention, secure logging on the backend specifically |
| [VIRTUAL_THREADS.md](backend/VIRTUAL_THREADS.md) | Deciding whether a workload belongs on a virtual thread — socket I/O yes, file I/O no |
| [JANDEX_METADATA_SCANNING.md](backend/JANDEX_METADATA_SCANNING.md) | Looking up classes or annotations at runtime — prefer this over reflection |
| [HEALTH_MONITORING.md](backend/HEALTH_MONITORING.md) | Working on health endpoints or runtime log levels |
| [TELEMETRY_IMPLEMENTATION.md](backend/TELEMETRY_IMPLEMENTATION.md) | Adding a metric or working on the `/v1/usage` endpoints |
| [SYSTEM_EVENTS.md](backend/SYSTEM_EVENTS.md) | Producing or consuming cross-node events — at-least-once delivery and idempotency rules |
| [INFERENCE_API.md](backend/INFERENCE_API.md) | Working on the OpenAI-compatible `/api/inference/v1` family |
| [INDEX_FIELD_EMISSION.md](backend/INDEX_FIELD_EMISSION.md) | Changing how a contentlet becomes an index document — includes the `_dotraw` zero-padding sort invariant |

### ES → OpenSearch migration

| Doc | Load it when |
|---|---|
| [OPENSEARCH_MIGRATION.md](backend/OPENSEARCH_MIGRATION.md) | You need the design: phased dual-write/read rollout, architecture, configuration |
| [OPENSEARCH_MIGRATION_RUNBOOK.md](backend/OPENSEARCH_MIGRATION_RUNBOOK.md) | Actually running a migration — the operational procedure for Support and Cloud |
| [OPENSEARCH_MIGRATION_TEST_PLAN.md](backend/OPENSEARCH_MIGRATION_TEST_PLAN.md) | Planning or executing QA across the migration phases |
| [OPENSEARCH_MIGRATION_TESTER_GUIDE.md](backend/OPENSEARCH_MIGRATION_TESTER_GUIDE.md) | Getting started as a tester validating the migration |
| [OPENSEARCH_CLIENT_CONFIGURATION.md](backend/OPENSEARCH_CLIENT_CONFIGURATION.md) | Setting `OS_*`/`ES_*` properties — the reference and fallback chain |
| [SEARCH_API_MIGRATION.md](backend/SEARCH_API_MIGRATION.md) | Migrating a plugin off deprecated `ContentletAPI` search methods |

## Frontend — Angular and TypeScript

**Start at [frontend/README.md](frontend/README.md)** — it lists every frontend doc and when to load each.

| Doc | Load it when |
|---|---|
| [ANGULAR_STANDARDS.md](frontend/ANGULAR_STANDARDS.md) | Any Angular work — single source of truth for syntax, signals, change detection, forms, icons |
| [COMPONENT_ARCHITECTURE.md](frontend/COMPONENT_ARCHITECTURE.md) | Deciding component structure, file layout, or data flow |
| [STATE_MANAGEMENT.md](frontend/STATE_MANAGEMENT.md) | Adding or changing feature state — NgRx Signal Store, `rxMethod`, `patchState` |
| [STYLING_STANDARDS.md](frontend/STYLING_STANDARDS.md) | Writing styles, or building form markup — Tailwind, PrimeNG theme, BEM, the global `.form`/`.field` conventions |
| [TYPESCRIPT_STANDARDS.md](frontend/TYPESCRIPT_STANDARDS.md) | Strict types, `as const`, `#` private fields |
| [TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md) | Writing frontend tests — Spectator, Vitest, `byTestId` |
| [TESTING_REVIEW_RULES.md](frontend/TESTING_REVIEW_RULES.md) | Reviewing someone else's tests — the violation checklist |
| [TESTING_PERFORMANCE.md](frontend/TESTING_PERFORMANCE.md) | About to change a Vitest option "to make tests faster" — what was measured and what did **not** help |
| [BREADCRUMBS.md](frontend/BREADCRUMBS.md) | Working on the GlobalStore breadcrumb trail |
| [KEYBOARD_SHORTCUTS.md](frontend/KEYBOARD_SHORTCUTS.md) | Adding a shortcut to the admin UI, or working out how one is arbitrated |

## Testing

| Doc | Load it when |
|---|---|
| [INTEGRATION_TESTS.md](testing/INTEGRATION_TESTS.md) | Writing or running an integration test — **includes the MainSuite registration rule: an unregistered test is silently never run in CI** |
| [BACKEND_UNIT_TESTS.md](testing/BACKEND_UNIT_TESTS.md) | Writing a unit test in `:dotcms-core` — Surefire, Mockito setup, naming (there is no category/tag mechanism) |
| [E2E_TESTS.md](testing/E2E_TESTS.md) | Working on the Playwright suite in `core-web/apps/dotcms-ui-e2e` |
| [API_TESTING.md](testing/API_TESTING.md) | Writing API tests — Postman (`dotcms-postman/`) and Karate (`test-karate/`) |
| [PERFORMANCE_TESTS.md](testing/PERFORMANCE_TESTS.md) | Load testing with JMeter (`test-jmeter/`) or the Kubernetes analytics suite |
| [test-cases/README.md](test-cases/README.md) | Looking for the manual/feature test-suite index |

## CLI (`tools/dotcms-cli`)

| Doc | Load it when |
|---|---|
| [CLI_OVERVIEW.md](cli/CLI_OVERVIEW.md) | Working on the CLI — Quarkus + PicocLI architecture, modules, command patterns. **It compiles to a lower `maven.compiler.release` than the core modules; read the property before using modern syntax** |
| [CLI_BUILD_SYSTEM.md](cli/CLI_BUILD_SYSTEM.md) | Building or releasing the CLI — `dist`/`native`/`release` profiles, Quarkus dev mode, native image, testcontainers |

## Integration

| Doc | Load it when |
|---|---|
| [API_CONTRACTS.md](integration/API_CONTRACTS.md) | Building a REST endpoint that Angular consumes, or consuming one — frontend↔backend contract patterns. **Jointly owned by Falcon and Scout**: touch base with the other team before changing content covering their endpoints |

## Infrastructure

| Doc | Load it when |
|---|---|
| [DOCKER_BUILD_PROCESS.md](infrastructure/DOCKER_BUILD_PROCESS.md) | Building or optimising the container image |

## Working with AI assistants

| Doc | Load it when |
|---|---|
| [DOCUMENTATION_MAINTENANCE.md](claude/DOCUMENTATION_MAINTENANCE.md) | Deciding where a piece of documentation belongs, or when to update one |
| [WORKFLOW_PATTERNS.md](claude/WORKFLOW_PATTERNS.md) | Structuring how an assistant approaches a task — context detection, information gathering |
| [GITHUB_AUTOMATION.md](claude/GITHUB_AUTOMATION.md) | Automating GitHub issue management with dotCMS utilities |
| [GPG_COMMIT_SIGNING.md](claude/GPG_COMMIT_SIGNING.md) | Setting up signed commits — required so commits made through AI tools are verifiably authorized by you |

Cursor rules live in [`.cursor/rules/`](../.cursor/rules/README.md) (short reminders with globs, linking back to these docs). Slash commands live in `.claude/commands/`, and the skills they call in `.claude/skills/` — see its [CATALOG.md](../.claude/skills/CATALOG.md).

---

## Keeping this index honest

An index is only useful if it is complete, and completeness decays silently — a doc gets added, nobody links it, and it is invisible from then on. `scripts/validate-docs-reachability.py` walks the link graph from `CLAUDE.md` and this file and exits non-zero on any unreachable `.md` under `docs/`, so the decay fails a check instead of going unnoticed.

```bash
python3 scripts/validate-docs-reachability.py           # report
python3 scripts/validate-docs-reachability.py --strict  # exit 1 if anything is unreachable
```

When you add a doc, add it here. When you write one, say when to load it, not what it contains — that is what makes an index usable by someone who does not already know the answer.

See [Documentation Maintenance](claude/DOCUMENTATION_MAINTENANCE.md) for what belongs where.
