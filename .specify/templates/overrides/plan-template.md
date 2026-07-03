# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This is the dotCMS override of the plan template (resolved from
`.specify/templates/overrides/plan-template.md`). It adds a **Legacy Impact**
section and a mandatory **ADR Alignment (Gate)** to the stock Spec-Kit plan.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Java 25, TypeScript 5.x / Angular 19 or NEEDS CLARIFICATION]

**Primary Dependencies**: [e.g., Spring/CDI, JAX-RS, PrimeNG, Nx or NEEDS CLARIFICATION]

**Storage**: [if applicable, e.g., PostgreSQL, Elasticsearch/OpenSearch, files or N/A]

**Testing**: [e.g., JUnit integration (`-Dit.test=`), Postman, Jest/Spectator, Playwright]

**Target Platform**: [e.g., dotCMS server (Docker), core-web SPA or NEEDS CLARIFICATION]

**Project Type**: [backend `com.dotcms`/`com.dotmarketing` / REST API / Angular lib/app / mixed]

**Performance Goals**: [domain-specific, e.g., query p95, indexing throughput or NEEDS CLARIFICATION]

**Constraints**: [domain-specific, e.g., rollback-safety, backward compat, memory or NEEDS CLARIFICATION]

**Scale/Scope**: [domain-specific, e.g., tenants, content volume, screens or NEEDS CLARIFICATION]

## Legacy Impact

*dotCMS is a mixed-age codebase — see Constitution Principle I. Fill this out; do not skip.*

- **Touches legacy?**: [Does this change reach into `com.dotmarketing.*`? Which subsystems?]
- **Modern vs legacy placement**: [New code should live under `com.dotcms.*` unless it must
  extend an existing legacy component — justify if it doesn't.]
- **Backward compatibility / migration**: [DB schema, ES/OpenSearch mappings, REST/API
  contracts, serialized state — call out rollback-unsafe changes.]
- **Progressive enhancements**: [Small in-scope cleanups in the code being touched — generics,
  `Logger` over `System.out`, `@Override`/`@Nullable`, `@if` over `*ngIf`. No wholesale rewrites.]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Evaluate the plan against `.specify/memory/constitution.md`. List each relevant principle
and PASS/FAIL. Unjustified violations block the plan — record justified ones in Complexity
Tracking.]

## ADR Alignment (Gate)

*GATE: Must be completed before Phase 1 design. ADRs live ONLY in `dotCMS/platform-adrs`.*

**Step 1 — Consult existing ADRs (mandatory).** Run the lookup with keywords drawn from this
feature (tech, subsystem, data store, integration):

```bash
.specify/scripts/bash/adr-context.sh "<keyword>" "<keyword>" ...
```

(Equivalently, the `/speckit-adr-context` skill runs automatically as a `before_plan` hook.)
Review the full index if the lookup is sparse: `dotCMS/platform-adrs/INDEX.md`.

### Relevant existing ADRs

<!-- List every ADR that informs or constrains this plan. Treat ACCEPTED ADRs as binding. -->

| ADR | Title | Status | How it constrains / informs this plan |
|-----|-------|--------|----------------------------------------|
| [ADR-XXXX](https://github.com/dotCMS/platform-adrs/blob/main/decisions/XXXX-...md) | [title] | accepted/proposed | [impact on approach] |

*If none apply, state "No relevant ADRs found" and note the keywords searched.*

### Conflicts with accepted ADRs

<!-- Any tension between this plan and an ACCEPTED ADR must be resolved here or the plan
     changed to comply. An accepted ADR wins unless explicitly superseded. -->

- [None] — or describe the conflict and its resolution.

### Proposed ADRs (propose only — never created here)

<!--
  If this plan makes a NEW architectural decision worth recording, propose it here.
  DO NOT create an ADR in this repo or in platform-adrs. ADRs are authored only in
  dotCMS/platform-adrs via its own process (new-adr.sh + template.md).
-->

| Proposed title | One-line rationale | Suggested next step |
|----------------|--------------------|--------------------|
| [ADR: ...] | [why this decision deserves an ADR] | Open in `dotCMS/platform-adrs` via `new-adr.sh` |

*If no new decision warrants an ADR, state "None proposed."*

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature using REAL dotCMS paths. Delete unused options. The delivered
  plan must not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Backend feature (dotCMS core)
dotCMS/src/main/java/com/dotcms/<domain>/        # prefer modern package for new code
dotCMS/src/main/java/com/dotmarketing/<area>/    # only if extending existing legacy
dotcms-integration/src/test/java/...             # integration tests

# [REMOVE IF UNUSED] Option 2: Frontend feature (core-web Nx monorepo)
core-web/libs/<lib>/src/lib/...
core-web/apps/dotcms-ui/src/app/...

# [REMOVE IF UNUSED] Option 3: Full-stack (backend + frontend + REST contract)
dotCMS/src/main/java/com/dotcms/rest/...          # JAX-RS endpoint (@Schema must match return type)
core-web/libs/.../data-access/...                 # frontend service consuming the API
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check or ADR Alignment has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., new code in legacy `dotmarketing`] | [current need] | [why modern placement insufficient] |
| [e.g., conflicts with proposed ADR-00XX] | [specific problem] | [why the ADR's approach insufficient] |
