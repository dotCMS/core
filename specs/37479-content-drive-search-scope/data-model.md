# Phase 1 Data Model: Content Drive search scope

**Feature**: `37479-content-drive-search-scope` | **Date**: 2026-09-14
**Plan**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

> **No persistent data model changes.** No table, column, index mapping or content-model change.
> Every entity below is request-scoped or client-session-scoped. This is what makes the feature
> rollback-safe by construction (plan → Legacy Impact).

---

## 1. `SearchScope` (new enum)

Which **fields** of a document a Content Drive search term is matched against. Not to be confused
with the **browse** scope of [#37426](https://github.com/dotCMS/core/issues/37426), which says
*where* you are browsing (spec → Review Decision 5).

| Value | Meaning | Default |
|---|---|---|
| `ALL_FIELDS` | The term is read against every indexed field of the document | ✅ yes |
| `TITLE` | The term is read against the contentlet title only | no |

**Rules**

- **R-1** — Absent on a request ⇒ `ALL_FIELDS`, with results byte-identical to today (FR-017, SC-005).
  Realised as Immutables `@Value.Default` so no call site does null handling.
- **R-2** — An unrecognized value is **rejected** with a client error that names the offending value;
  it never silently defaults (FR-018). This differs deliberately from R-4 below.
- **R-3** — Values are the wire contract. Prose says "search scope"; the field is `searchScope`
  (FR-023).
- **R-4** — An unrecognized value **in the browser address** resolves to `ALL_FIELDS` with no error
  surfaced (FR-015). A URL is not a contract and a stale link must still open.

> R-2 and R-4 look contradictory and are not. A request is a contract between programs: a wrong value
> is a bug, and failing loudly is how it gets found. An address is a human artefact that outlives the
> code that wrote it: failing loudly there strands the user for a typo. Same value, different
> provenance, different obligation.

**Placement**: Java enum under `com.dotcms.rest.api.v1.drive` beside the other request types.
Frontend mirror as a `const` object in `shared/constants.ts` with a derived union type
(`TYPESCRIPT_STANDARDS.md` — `as const`, not a TS `enum`).

---

## 2. `QueryFilters` (existing immutable — one new member)

`com.dotcms.rest.api.v1.drive.AbstractQueryFilters`, today `{ text, filterFolders }`.

| Member | Type | Required | Notes |
|---|---|---|---|
| `text` | `String` | yes | Existing. The term the other two members qualify. |
| `filterFolders` | `boolean` | no (default `true`) | Existing. Javadoc: *"when text is provided"*. |
| **`searchScope`** | **`SearchScope`** | **no (default `ALL_FIELDS`)** | **New.** Which fields `text` is read against. |

**Rules**

- **R-5** — `searchScope` is meaningless without `text`. A request carrying a scope with no text is a
  **contract error**, rejected in `ContentDriveHelper` beside the existing `userSearchable`
  cross-field check (FR-025, research R4).
- **R-6** — The member sits **inside** `filters`, not at the request's top level. All three members
  exist to qualify the text search, so they travel together and a nonsense combination is visible
  rather than remembered (spec → Review Decision 6).

**Why not top level**: the browse scope of #37426 *does* sit at the top level, because it qualifies
`assetPath` and because "Clear all" resets `filters` — a browse scope there could navigate you out of
System Host. For the search scope, being cleared by "Clear all" is exactly right (FR-020). Same rule,
opposite placement.

---

## 3. `BrowserQuery` (existing internal query object — one new member)

`com.dotcms.browser.BrowserQuery`, the translation target of the request form.

| Member | Type | Notes |
|---|---|---|
| `filter` | `String` | Existing. Carries `filters.text`. |
| `useElasticsearchFiltering` | `boolean` | Existing; Content Drive forces `true` when text is present (`ContentDriveHelper:181`). |
| **`searchScope`** | **`SearchScope`** | **New.** Defaults to `ALL_FIELDS` so the other five callers of this object are unaffected (FR-024). |

**Rules**

- **R-7** — Only `ContentDriveHelper` sets it. `WebAssetHelper`, `BrowserAjax`, `DotCMSMacroWebAPI`
  and `FileAssetAPIImpl` construct `BrowserQuery` without it and therefore keep today's behaviour
  exactly (FR-024, SC-008).
- **R-8** — The member selects between two clause shapes in `buildBaseESQuery` and **must not** alter
  any other part of the query: sort, paging, permissions, folder/link matching and every non-text
  filter are untouched (FR-011, FR-012, FR-013).

---

## 4. Client filter state (existing — one new key)

Content Drive's filter state in `dot-content-drive.store.ts`, which feeds both the URL and the filter
chip bar.

| Aspect | Behaviour |
|---|---|
| Key | A dedicated key — **must not be `title`**. The store already keys the *search term* as `title` (`getFilterValue('title')`), and a scope whose value is `TITLE` sitting next to a filter key named `title` is a collision waiting to happen (research R5). |
| Written | **Only when not `ALL_FIELDS`** (FR-021). |
| Removed | On return to `ALL_FIELDS`, and by "Clear all" (FR-020) — the key is deleted, mirroring how the search term deletes its own key when emptied (`dot-content-drive.store.ts:228-234`). |
| Restored | From the address on load, reload and Back/Forward (FR-014). Unrecognized ⇒ `ALL_FIELDS`, silently (R-4, FR-015). |
| Persisted | **Never.** No per-user preference; a clean entry starts at `ALL_FIELDS` (FR-016). |

**Rule R-9** — the write-only-when-non-default behaviour is not cosmetic. `hasNonDefaultFilters`
(`utils/functions.ts:334-355`) counts every key except `sharedAssets` and `languageId`, and that
signal shows the bar's "Clear all" (`dot-filter-bar.component.html:7`). Writing the key
unconditionally would offer "Clear all" on a completely unfiltered drive the moment someone selected
the default (spec → Premise Correction 4).

---

## 5. Search outcome — failure vs. emptiness (no new type required)

Not an entity so much as a **distinction that must stop being destroyed**.

| State | Today | Required |
|---|---|---|
| Query matched nothing | empty result set | empty result set, unchanged |
| Query failed to execute | **empty result set** (`BrowserAPIImpl:893-895` logs and returns empty) | distinguishable from the above |

**Rules**

- **R-10** — The browsing service must preserve the distinction; the failure stays logged (FR-029,
  Constitution II — surfacing is added, logging is not removed).
- **R-11** — **Content Drive alone** presents it as an error state. The other four callers keep
  receiving today's empty result, so FR-024 holds (research R3).

> How the signal is carried is an implementation choice for `/speckit-tasks`. The data-model
> obligation is only that the two states stop being the same value. This is the piece that turns
> ticket 39185's "No results found" into something the author can act on.

---

## Entity relationships

```text
POST /v1/drive/search
└── DriveRequestForm
    ├── filters: QueryFilters
    │   ├── text: String            ← the term
    │   ├── filterFolders: boolean  ← qualifies text
    │   └── searchScope: SearchScope ← NEW, qualifies text  (R-5, R-6)
    └── (contentTypes, baseTypes, language, workflow, status, userSearchable, sort, paging …)
                    │
                    ▼   ContentDriveHelper  — validates R-2, R-5; sets R-7
              BrowserQuery
                    ├── filter: String
                    └── searchScope: SearchScope ← NEW, default ALL_FIELDS
                    │
                    ▼   BrowserAPIImpl.buildBaseESQuery — R-8
        ┌───────────┴────────────┐
   ALL_FIELDS                  TITLE
   GlobalSearchAttributeStrategy   sibling clause, prefix-seek only
   (escaping fixed here —          (research R1; no catchall,
    benefits Search portlet         no leading-wildcard gate)
    + Relationships too, FR-031)
```

## Validation summary

| Rule | Requirement | Enforced where | Failure mode |
|---|---|---|---|
| R-1 | FR-017, SC-005 | `@Value.Default` on the immutable | n/a — absence is valid |
| R-2 | FR-018 | Request deserialization | Client error naming the value |
| R-4 | FR-015 | Frontend URL parsing | Silent fallback to `ALL_FIELDS` |
| R-5 | FR-025 | `ContentDriveHelper` | `BadRequestException` |
| R-7 | FR-024, SC-008 | Construction site — other callers never set it | n/a — by omission |
| R-9 | FR-021 | Filter facade / store | n/a — a behaviour, tested not enforced |
| R-10, R-11 | FR-029, SC-011 | Browsing service + Content Drive layer | Error state, not empty list |
