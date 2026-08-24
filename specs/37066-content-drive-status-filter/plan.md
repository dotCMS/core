# Implementation Plan: Content Drive Status Filter

**Branch**: `issue-37066-content-drive-status-filter-plan` (spec on `issue-37066-content-drive-status-filter`) | **Date**: 2026-08-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/37066-content-drive-status-filter/spec.md`

**Issue**: [dotCMS/core#37066](https://github.com/dotCMS/core/issues/37066) (absorbed #37067; epic #33999)

## Summary

Add a **Status** filter (Archived, Unpublished, Locked) to Content Drive: a new optional `status`
array on `POST /api/v1/drive/search`, resolved as AND-combined predicates in the database, plus a
multiselect chip in the Content Drive toolbar that drives it and round-trips through the URL.

The technical approach is a flat, additive one. The three statuses are independent boolean facts
about the same `contentlet_version_info` row, so each becomes one independent `and` clause — no
composed OR group, no new join. The plumbing mirrors the workflow filter (`beaf846d51`) end to end.
The only delicate part is that **two existing code paths already have an opinion about
`cvi.deleted`** and both must learn about `ARCHIVED`; that is detailed below and is where the
regression risk lives.

## Technical Context

**Language/Version**: Java 25 (`dotcms.core.compiler.release`); TypeScript 5.x / Angular 22+ (core-web)

**Primary Dependencies**: JAX-RS + Immutables (`@Value.Immutable`) for the request form; `BrowserAPI`/`BrowserQuery` for the query layer; PrimeNG (`p-popover`, `p-listbox`, `p-checkbox`) and NgRx Signal Store on the frontend

**Storage**: PostgreSQL / MS SQL (`contentlet_version_info`) as the system of record; Elasticsearch/OpenSearch for the non-default `PURE_ES` path. **No schema change** — all three columns already exist (`postgres.sql:550-552`)

**Testing**: JUnit integration (`dotcms-integration`, `-Dit.test=`), JUnit unit (`dotCMS/src/test`), Jest/Spectator (core-web)

**Target Platform**: dotCMS server (Docker) + the core-web SPA

**Project Type**: Full-stack — REST form + shared browse query layer + Angular portlet library

**Performance Goals**: No regression against today's drive search. Each status adds one indexed-column predicate to an existing `where`; no new join, no subquery, no extra round-trip. A non-empty selection also drops the folder query entirely (`showFolders(false)`), so filtered requests do strictly less work

**Constraints**: Rollback-safe (no schema, no mapping, no breaking contract change). With no `status` sent, every generated query must be **byte-identical** to today. The legacy Site Browser's inclusive "Show Archived" behavior must not change

**Scale/Scope**: ~4 backend files + 1 new enum; ~6 frontend files + 1 new component; 1 new integration test class, 1 unit test, 3 spec files

## Legacy Impact

- **Touches legacy?** No `com.dotmarketing.*` source is modified. `com.dotcms.browser.BrowserAPIImpl`
  is long-lived and heavily-parameterized but sits in the modern package. The legacy Site Browser JSP
  (`view_browser.jsp`) and the legacy Content Search portlet (`ContentletAjax`) are **read as
  precedent and left untouched**.
- **Modern vs legacy placement**: everything new lands in `com.dotcms.*` — the enum in
  `com.dotcms.browser` (next to `FieldSearchCriteria`, which plays the same query-shaping role), the
  form field in `com.dotcms.rest.api.v1.drive`.
- **Backward compatibility / migration**: none required. No DB schema change, no ES/OpenSearch
  mapping change, no serialized-state change. The REST change is one **optional** field with an empty
  default, so existing callers are unaffected. Not rollback-unsafe under any category in
  `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`.
  - The one real compatibility risk is behavioral, not structural: `BrowserQuery.showArchived` keeps
    its **inclusive** meaning (archived *plus* everything else) because `view_browser.jsp:145`
    depends on it. The new exclusive behavior is added alongside. An integration assertion guards
    this (FR-008).
- **Progressive enhancements** (in-scope, small, only in code already being touched):
  - Javadoc on every new method and on the new enum, matching the density of `appendWorkflowQuery`
    and `withWorkflowSchemeIds` around it.
  - The new frontend component is written to current standards from the start — `@if`, `input()`,
    signals, `#`-private members, `ChangeDetectionStrategy.OnPush` — and strict-mode clean, per the
    in-flight core-web strict migration.
  - No wholesale rewrite of `BrowserAPIImpl`. It is a 2700-line file; this change adds one private
    method and touches three existing lines.

## Test Strategy (TDD — mandatory)

Constitution Principle V: no implementation code before tests are written, developer-approved, and
confirmed **failing** for the right reason.

| Component / behavior | Test type(s) | Where | Notes |
|---|---|---|---|
| Status value parsing; unknown value → 400 | Unit (JUnit) | `dotCMS/src/test/java/com/dotcms/rest/api/v1/drive/ContentDriveHelperStatusTest.java` | Mirrors `ContentDriveFieldFilterResolverTest`. No container needed |
| Each status alone; each pair; all three; empty default | Integration | `dotcms-integration/src/test/java/com/dotcms/rest/api/v1/drive/ContentDriveStatusFilterTest.java` | Follows `ContentDriveWorkflowFilterTest`: dedicated site + folder + a purpose-built content type + unique id, `@AfterClass` cleanup. Never asserts against shared default content types |
| `ARCHIVED + UNPUBLISHED` == `ARCHIVED`; `ARCHIVED + LOCKED` reachable | Integration | same class | FR-007 and the second Edge Case — asserted as documented behavior |
| `UNPUBLISHED` alone excludes an archived item; `ARCHIVED + UNPUBLISHED` admits it | Integration | same class | FR-004's baseline carve-out. Fixture needs an item that is **both** archived and unpublished, or the assertion passes vacuously. Raised by the automated spec review on #37170 |
| `LOCKED` composes with working-version scoping | Integration | same class | FR-005. Assert a locked item is returned under the drive's default (`live: false`), matching the legacy `+locked:true` / `+working:true` pairing (`ContentletAjax.java:1018`/`:1035`). Raised by the same review |
| Parity with and without free text (default hybrid heuristic) | Integration | same class | FR-009 / SC-005 |
| Parity under `BROWSE_API_HEURISTIC_TYPE=PURE_ES` | Integration | same class | Config override + restore in the test; the only coverage `buildPureESQuery` gets |
| Status + archive-target workflow step | Integration | same class | FR-011. The regression this change is most likely to cause |
| Legacy inclusive `showArchived` unchanged | Integration | same class | FR-008 — a direct `BrowserQuery` assertion, not through the drive form |
| `ContentDriveWorkflowArchiveStepTest` still green | Integration (existing) | unchanged file | Byte-identical SQL when no status is sent |
| Status multiselect: single, multiple, clearing, testids | Jest/Spectator | `…/dot-content-drive-status-filter/dot-content-drive-status-filter.component.spec.ts` | Driven through the rendered checkbox's `(onChange)`, never protected members. No `if` in test bodies. No CSS-class assertions |
| Request carries `status`; `showFolders` false; `archived` pin gone | Jest | `…/store/dot-content-drive.store.spec.ts` | Asserts the built `$request` payload |
| `status` URL decode round-trip | Jest | `…/utils/functions.spec.ts` | Alongside the existing `workflow` decode cases |

**Registration**: `ContentDriveStatusFilterTest` joins `MainSuite3a`, where the other drive
integration tests live.

- **Tests that cannot be implemented**: **none**. Every layer is reachable. Postman is deliberately
  omitted rather than "not possible" — the endpoint-level behavior is covered more precisely by the
  integration tests, which can seed the exact archived/locked fixtures a Postman collection cannot
  construct against a shared environment. If the reviewer wants a Postman smoke case for the 400,
  that is a cheap addition.

## Constitution Check

*GATE: evaluated before Phase 0, re-evaluated after Phase 1 design. Result: **PASS**, no violations.*

| Principle | Verdict | Evidence |
|---|---|---|
| **I. Legacy-Aware Development** | PASS | No `com.dotmarketing.*` changes. New code in `com.dotcms.browser` / `com.dotcms.rest.api.v1.drive`. Legacy Site Browser behavior explicitly preserved and asserted. Progressive enhancements scoped to touched code only — see Legacy Impact |
| **II. Config & Logging Discipline** | PASS | No new `System.*` calls. Reads `BROWSE_API_HEURISTIC_TYPE` only through the existing `Config`-backed `HEURISTIC_TYPE` lazy. No new dependency, so no `bom/application/pom.xml` change |
| **III. Security by Default** | PASS | No secrets. The only user input is a closed enum, validated against it and rejected with a 400 — it never reaches SQL as text, so there is no injection surface. Permission filtering is untouched: the status clauses narrow a candidate set that `permissionAPI` still filters downstream, so the filter grants no new visibility |
| **IV. Contract Correctness** | PASS | One optional field with an empty default; no `@Schema` return type changes. `openapi.yaml` regenerated from the annotations and committed alongside the Java change. Not rollback-unsafe: no schema, no mapping, no breaking contract change |
| **V. Test-First / TDD** | PASS | Test Strategy above covers every layer with no exceptions claimed. `/speckit-tasks` will order each user story as tests → approval GATE → Red GATE → implementation, and `/speckit-implement` must halt at each gate |
| **ADR consultation** | PASS | `/speckit-adr-context` ran as the mandatory `before_plan` hook; results in ADR Alignment below |

## ADR Alignment (Gate)

**Step 1 — Consult existing ADRs**: run automatically as the `before_plan` hook.

```bash
.specify/scripts/bash/adr-context.sh content-drive search elasticsearch browser query rest angular filter permissions legacy
```

### Relevant existing ADRs

| ADR | Title | Status | How it constrains / informs this plan |
|---|---|---|---|
| [ADR-0018](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0018-database-first-content-drive-search-with-index-deferred-text-filtering.md) | Database-First Search for Content Drive, with Text Filtering Deferred to the Search Index | proposed | **Directly governing.** Its routing table lists *"Archived / deleted, show-on-menu → **DB** (version-info flags)"*. All three status predicates are version-info flags, so all three are resolved in SQL. It also states the index must be used *only* for free-text and searchable-field matching, and that structural criteria "must **never** be silently re-routed to the index for speed" — which this plan honors. It further notes `PURE_ES` "remains available behind configuration… but is **not** the default": that is precisely why `buildPureESQuery` is patched, so a supported configuration cannot silently drop the filter |
| [ADR-0009](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0009-opensearch-migration-plan.md) | Migrate OpenSearch from 1.x to 3.x Using Environment-Based Migration Strategy | accepted | Constrains only the `PURE_ES` clauses. The three terms used (`deleted`, `live`, `locked`) are core version-info fields mapped identically under both backends, so no migration-phase divergence is introduced. ADR-0018's own rationale — "shrink the blast radius of the ES→OS migration" by hanging fewer correctness guarantees off the index — is served by keeping the DB as the authority here |
| [ADR-0020](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0020-deprecate-folder-bypath-endpoint.md) | Deprecate `POST /api/v1/folder/byPath` in favor of `GET /api/v1/folder/search` | accepted | Surfaced by the keyword search but **not applicable** — this feature adds no folder endpoint and calls neither |

### Conflicts with accepted ADRs

**None.** The one ADR that governs this work (ADR-0018) is `proposed` rather than `accepted`, so it
is directional rather than binding — but this plan complies with it fully anyway, so the distinction
does not need resolving. Its central rule is that structural and metadata predicates belong in the
database, and all three status predicates are resolved there.

### Proposed ADRs

**None proposed.** This feature adds a filter *within* an already-decided routing contract; it makes
no new architectural decision. The AND-vs-OR choice is a domain fact about independent boolean flags
(and matches long-standing behavior in the legacy Content Search portlet), not an architectural
decision worth recording.

## Project Structure

### Documentation (this feature)

```text
specs/37066-content-drive-status-filter/
├── spec.md                             # /speckit-specify output
├── plan.md                             # This file
├── research.md                         # Phase 0 — R1..R10, all questions closed
├── data-model.md                       # Phase 1 — enum, transport and filter-bag shapes
├── quickstart.md                       # Phase 1 — how to build, test and see it work
├── contracts/
│   └── drive-search-status.md          # Phase 1 — the `status` field contract
├── checklists/requirements.md          # gitignored; local spec-quality record
└── tasks.md                            # Phase 2 — /speckit-tasks, NOT created here
```

### Source Code (repository root)

```text
# Backend — query layer
dotCMS/src/main/java/com/dotcms/browser/
├── ContentStatus.java                      # NEW — ARCHIVED | UNPUBLISHED | LOCKED
├── BrowserQuery.java                       # + field, builder method, copy-ctor, toString;
│                                           #   showWorking derivation extended
└── BrowserAPIImpl.java                     # + appendContentStatusQuery; 3 touched lines
                                            #   (:1980 archiveStepIds, :2006 exclusion, :612 ES)

# Backend — REST
dotCMS/src/main/java/com/dotcms/rest/api/v1/drive/
├── AbstractDriveRequestForm.java           # + status() : List<String>, default List.of()
├── ContentDriveHelper.java                 # + parseStatuses + builder wiring + showFolders(false)
└── ContentDriveResource.java               # @Operation description only
dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml   # regenerated, committed

# Backend — tests
dotCMS/src/test/java/com/dotcms/rest/api/v1/drive/ContentDriveHelperStatusTest.java        # NEW
dotcms-integration/src/test/java/com/dotcms/rest/api/v1/drive/ContentDriveStatusFilterTest.java  # NEW
dotcms-integration/src/test/java/com/dotcms/MainSuite3a.java                                # + registration

# Frontend — portlet
core-web/libs/portlets/dot-content-drive/portlet/src/lib/
├── shared/constants.ts                     # + STATUS_FILTER_KEY, CONTENT_STATUS, STATUS_FILTER_OPTIONS
├── shared/models.ts                        # + status: string[] on DotKnownContentDriveFilters
├── utils/functions.ts                      # + status: multiSelector in decodeByFilterKey
├── store/dot-content-drive.store.ts        # - archived: false; + status; + showFolders term
└── components/dot-content-drive-toolbar/
    ├── dot-content-drive-toolbar.component.{ts,html}          # render the new chip
    └── components/dot-content-drive-status-filter/            # NEW component + template + spec

# Frontend — shared model + i18n
core-web/libs/dotcms-models/src/lib/dot-content-drive.model.ts      # + status?: string[]
dotCMS/src/main/webapp/WEB-INF/messages/Language.properties          # + 4 keys (near :7113)
```

**Structure Decision**: Full-stack, following the exact shape the workflow filter established in
`beaf846d51` — request form → `BrowserQuery` → `BrowserAPIImpl` on the backend, and filter bag →
store `$request` → toolbar chip on the frontend. Nothing new is introduced structurally; this
feature is a second instance of an already-proven pattern, which is why it should land well under
the archive-step work's footprint (`f92f939296`: 184 impl lines).

---

## Implementation approach

Full detail and verification for each decision is in [research.md](./research.md); this is the
summary a reviewer needs.

### Backend

**1. `ContentStatus` enum** — `ARCHIVED`, `UNPUBLISHED`, `LOCKED`, in `com.dotcms.browser`.

**2. `AbstractDriveRequestForm.status()`** — `List<String>`, defaulting to `List.of()`. Deliberately
strings rather than the enum, so `ContentDriveHelper` owns the parse and throws an explicit
`BadRequestException` naming the accepted values — the deterministic 400 FR-010 asks for, matching
the `userSearchable` precedent already in that class (R7).

**3. `BrowserQuery`** — plumbed exactly like `workflowSchemeIds`. One derived line changes:

```java
this.showWorking = builder.showWorking || builder.showArchived;   // :151
```

must also be true when `ARCHIVED` or `UNPUBLISHED` is selected. Both states mean *no live version*,
so without this the query joins `c.inode = cvi.live_inode` and can never match — the filter would
silently return nothing. The drive path is safe today only by coincidence (`live()` defaults false);
the flag has to be right for any caller (R4).

**4. `BrowserAPIImpl` — the three clauses**, in a new private `appendContentStatusQuery`:

| Status | SQL |
|---|---|
| `ARCHIVED` | `and cvi.deleted = <DBTrue>` |
| `UNPUBLISHED` | `and cvi.live_inode is null` |
| `LOCKED` | `and cvi.locked_by is not null` |

Independent `and` clauses — that *is* the AND semantics, no combinator needed.

**5. `BrowserAPIImpl` — the two `cvi.deleted` interactions.** This is the risk surface:

- **The global exclusion** (`:2006`) gains a third term:
  `if (!showArchived && archiveStepIds.isEmpty() && !statuses.contains(ARCHIVED))`.
  Without it, `cvi.deleted = false` and `cvi.deleted = true` are both emitted and `ARCHIVED` always
  returns nothing. With it, `UNPUBLISHED`/`LOCKED` alone still keep the exclusion — which is exactly
  FR-004, for free.
- **Archive-target workflow steps** (`:1980`). `archiveStepIds` is already emptied when
  `showArchived`, because `appendWorkflowQuery` otherwise owns `cvi.deleted` per branch and would
  force `false` on the live branch. `ARCHIVED` needs identical treatment:
  ```java
  final boolean admitsArchived = browserQuery.showArchived || statuses.contains(ARCHIVED);
  ```
  This reuses the mechanism the archive-step work already built rather than inventing a second
  reconciliation. With no status sent, every generated query stays byte-identical (R5).

**6. `buildPureESQuery`** — the hardcoded `+deleted:false` (`:612`) becomes conditional, plus
`+live:false` / `+locked:true`. Only runs under `BROWSE_API_HEURISTIC_TYPE=PURE_ES`, which is a
supported configuration where the filter would otherwise silently no-op (R6).

**7. `ContentDriveHelper`** — a block mirroring the workflow block directly above it: parse, set the
statuses, and `showFolders(false)` because folders carry no status.

### Frontend

Reuse over invention — every piece already exists (R9):

- **Constants / models / decode**: three small additions (`STATUS_FILTER_OPTIONS`, `status: string[]`
  on the filter bag and on `DotContentDriveSearchRequest`, `status: multiSelector` in
  `decodeByFilterKey`). Encoding needs **no** change: `encodeFilters` already comma-joins arrays.
- **Not seeded in `withFilterDefaults`.** Unlike `languageId` and `sharedAssets`, where "absent" is
  not a neutral state, an empty status set genuinely means no filtering. Leaving it unseeded makes
  "Clear all" appear and clear correctly with no new code.
- **Store `$request`**: drop `archived: false` (the server default already supplies it) and send
  `status` instead; add `!filters()?.status?.length` to the `showFolders` conjunction.
- **New component**: `dot-chip-filter` (`mode="dropdown"`) + `p-popover` + `p-listbox` with a
  `p-checkbox` per row and `dot-filter-list-item` for labels, over a static three-option list.
  Modeled on the workflow filter but **without** its service, caches, request-id guard and reconcile
  pass — those exist because workflow options are fetched and can vanish between loads. Three fixed
  options need none of it. `data-testid` on the chip, panel and each option; `[attr.aria-label]` on
  the chip.
- **i18n**: four `content-drive.status-filter.*` keys in `Language.properties`.

## Complexity Tracking

No Constitution Check or ADR Alignment violations, so this section is intentionally empty.
