# dotCMS Core Constitution

<!--
  This constitution is loaded by every /speckit-* skill and is the supreme source
  of project law for spec-driven work in dotCMS/core. It intentionally mirrors the
  "Critical Rules" and "Progressive Enhancement" guidance in the repository root
  CLAUDE.md and the /docs standards. When CLAUDE.md and this file disagree, update
  both — they must stay in sync.
-->

## Core Principles

### I. Legacy-Aware Development (NON-NEGOTIABLE)

dotCMS/core is a mixed-age codebase and must be treated as one, not as greenfield.

- Modern domain code lives under `com.dotcms.*`; ~15+ yr old but **still-active** legacy
  lives under `com.dotmarketing.*`. New code SHOULD prefer modern `com.dotcms.*` packages
  and patterns.
- When touching legacy code, apply **progressive enhancement** (add generics, `@Override`,
  `@Nullable`, replace `System.out` with `Logger`, `*ngIf` → `@if`, etc.) — improve
  incrementally; do **not** rewrite legacy subsystems wholesale as a side effect of a
  feature or fix.
- Respect the patterns already present in the module being edited. New code must read like
  the code around it (naming, comment density, service access via `APILocator`).
- Every plan MUST state its **Legacy Impact**: does it touch `com.dotmarketing.*`? What are
  the backward-compatibility and migration implications?

### II. Configuration & Logging Discipline (NON-NEGOTIABLE)

- Use `Config.getStringProperty(...)` / `Logger.info(this, ...)` only. **Never** `System.out`,
  `System.getProperty`, or `System.getenv`.
- Dependency versions go in `bom/application/pom.xml` **only** — never in `dotCMS/pom.xml`.
- Core modules compile to Java 25 by default; runtime is Java 25.

### III. Security by Default (NON-NEGOTIABLE)

- No hardcoded secrets. Validate all input. Never log sensitive data.
- Follow [Security Principles](docs/core/SECURITY_PRINCIPLES.md).

### IV. Contract Correctness

- REST endpoints: `@Schema` MUST match the actual return type. `openapi.yaml` is
  auto-generated — description changes go in Java `@Operation`/`@Parameter` annotations and
  the regenerated yaml is committed alongside the Java change.
- Be mindful of rollback-unsafe changes (DB schema, ES mapping, API contracts) — see
  [Rollback-Unsafe Change Categories](docs/core/ROLLBACK_UNSAFE_CATEGORIES.md).

### V. Test-First / TDD (NON-NEGOTIABLE)

Test-Driven Development is mandatory. **No implementation code shall be written before:**

1. **Tests are written** whenever possible — using the layer-appropriate type(s): unit,
   Postman, integration, Karate, and/or e2e.
2. **Tests are validated and approved by the developer.** If a test type genuinely cannot be
   implemented, the developer must **explicitly say so and explain why** — silence is not
   consent, and "no tests" is never the default.
3. **Tests are confirmed to FAIL** for the right reason (the **Red** phase) before any
   implementation is written.

Only after these three gates pass may implementation proceed (Green), followed by refactor.
`/speckit-tasks` MUST order every user story as tests → approval gate → Red gate →
implementation, and `/speckit-implement` MUST honor that order and halt at each gate.

Operational notes:

- Never run the full integration suite; target specific classes/methods
  (`-Dit.test=Class#method`). Postman via `-Dpostman.collections=`.
- Prefer reusing existing utilities (`UtilMethods`, `APILocator`, batch
  `permissionAPI.filterCollection`) over new bespoke helpers, in both tests and code.

## Architecture Decision Records (ADRs)

ADRs are binding architectural context and live **only** in the private repository
`dotCMS/platform-adrs` (catalogued in its `INDEX.md`, stored under `decisions/`).

- **ADR consultation is mandatory in the plan phase.** Before an implementation plan is
  finalized, relevant existing ADRs MUST be consulted (via the `/speckit-adr-context` skill /
  `.specify/scripts/bash/adr-context.sh`) and treated as binding input. A plan that conflicts
  with an **accepted** ADR must resolve or explicitly justify the conflict.
- **Spec-Kit MUST NOT create, edit, or commit ADRs — anywhere.** ADRs are authored only in
  `dotCMS/platform-adrs` through its own process (`new-adr.sh` + `template.md`). Any
  `/speckit-*` skill may **propose** an ADR (title + rationale, recorded under "Proposed
  ADRs" in the plan), but must never write an ADR file in this repo or that one.

## Governance

- This constitution supersedes ad-hoc practices for spec-driven work. All specs, plans, and
  tasks are validated against it; the plan phase enforces the Constitution Check and the ADR
  gate.
- Complexity that violates a principle must be justified in the plan's Complexity Tracking
  table, or the plan is rejected.
- Amendments: keep this file and root `CLAUDE.md` in sync; bump the version below.

**Version**: 1.1.0 | **Ratified**: 2026-07-03 | **Last Amended**: 2026-07-03

<!-- 1.1.0: Principle V strengthened to Test-First / TDD (NON-NEGOTIABLE) with the 3-gate rule. -->

