# Phase 1 Data Model: Content Drive Status Filter

**Feature**: [spec.md](./spec.md) | **Date**: 2026-08-24

> `plan.md`, `research.md` and `quickstart.md` are Spec-Kit process artifacts and are gitignored by
> policy (`.specify/CUSTOMIZATIONS.md`), so the `research.md` references below point at files that
> exist on the author's machine, not in this repo. Each one is summarized inline so this document
> stands on its own.

No database schema changes. Every column this feature reads already exists on
`contentlet_version_info` and is already indexed into the search index. This document describes the
**in-memory and over-the-wire shapes** the feature introduces.

---

## Entity: `ContentStatus` (new)

`dotCMS/src/main/java/com/dotcms/browser/ContentStatus.java`

A closed enum of three independent states a contentlet version can hold.

| Constant | Meaning | Backing column (`contentlet_version_info`) | Index term |
|---|---|---|---|
| `ARCHIVED` | Removed from circulation, recoverable | `deleted = true` | `+deleted:true` |
| `UNPUBLISHED` | No live version exists | `live_inode is null` | `+live:false` |
| `LOCKED` | A lock is held, by anyone | `locked_by is not null` | `+locked:true` |

**Placement**: `com.dotcms.browser` rather than the REST package — `BrowserQuery` is the consumer,
and the browser layer must not depend on the REST layer. Sits alongside `FieldSearchCriteria`, which
plays the same query-shaping role.

**Relationships**: none. The three are orthogonal facts about one row.

**Not a state machine**: an item can hold any subset of the three at once. The filter asks whether
an item is in *any* selected state, not all of them — selected statuses combine with **OR**, like
the Content Type and Language filters. AND was considered and rejected: under AND,
`{ARCHIVED, UNPUBLISHED}` is redundant, `{ARCHIVED, LOCKED}` is almost always empty and all three is
empty in practice, so only one of four combinations says anything — and the chip would be the sole
exception in a toolbar row where every other filter widens on selection (research.md R2).

One overlap is worth knowing even though it no longer produces a degenerate result: every archived
item is also unpublished, because archiving removes the live version
(`ESContentletAPIImpl.java:3833`). Under OR that just means `{ARCHIVED, UNPUBLISHED}` reads as
"everything with no live version" rather than being redundant.

---

## Transport shape: `DriveRequestForm.status`

`dotCMS/src/main/java/com/dotcms/rest/api/v1/drive/AbstractDriveRequestForm.java`

```java
@JsonProperty("status")
@Value.Default
default List<String> status() { return List.of(); }
```

| Property | Value |
|---|---|
| JSON key | `status` |
| Type on the wire | array of strings |
| Accepted values | `ARCHIVED`, `UNPUBLISHED`, `LOCKED` (case-insensitive on input, uppercased before lookup) |
| Default | `[]` — preserves today's behavior exactly (FR-002) |
| Duplicates | Collapsed; the parsed result is a `Set` |
| Unknown value | `400`, message naming the accepted values (FR-010) |

**Declared as `List<String>`, not `List<ContentStatus>`** (research.md R7): a typed field would
route an invalid value through Jackson's `InvalidFormatException`, whose mapping to a useful 400 is
less direct than throwing one ourselves. The helper owns the parse instead, matching the
`userSearchable` precedent already in `ContentDriveHelper`.

### Validation rules

| Rule | Source | Enforced in |
|---|---|---|
| Empty is valid and means "no status filtering" | FR-001, FR-002 | `ContentDriveHelper` (block is skipped) |
| Every element must name a `ContentStatus` | FR-010 | `ContentDriveHelper.parseStatuses` → `BadRequestException` |
| Selection widens (OR), never narrows | FR-006 | `BrowserAPIImpl.appendContentStatusQuery` — one OR-ed group |
| The archived baseline stands unless `ARCHIVED` is selected | FR-007 | `BrowserAPIImpl:2006` — the exclusion is skipped only when the selection contains `ARCHIVED` |
| A non-empty selection excludes folders | FR-015 | `ContentDriveHelper` → `.showFolders(false)` |

### The archived baseline is not a fourth flag, and it lives outside the OR group

Excluding archived content is the drive's **pre-existing default**, not a member of this set:
`appendExcludeArchivedQuery` already emits `cvi.deleted = false` on every request today. The status
group is OR-ed internally and AND-ed against that baseline; `ARCHIVED` is the only status that lifts
it.

| Selection | Baseline | Status group | Net |
|---|---|---|---|
| `[]` | `deleted = false` | — | today's behavior |
| `[UNPUBLISHED]` | `deleted = false` | `(live_inode is null)` | unpublished, not archived |
| `[LOCKED]` | `deleted = false` | `(locked_by is not null)` | locked, not archived |
| `[UNPUBLISHED, LOCKED]` | `deleted = false` | `(live_inode is null or locked_by is not null)` | either, still not archived |
| `[ARCHIVED]` | *lifted* | `(deleted = true)` | archived only |
| `[ARCHIVED, UNPUBLISHED]` | *lifted* | `(deleted = true or live_inode is null)` | everything with no live version |
| all three | *lifted* | `(deleted = true or live_inode is null or locked_by is not null)` | anything not cleanly published |

**The bug to avoid** is folding the baseline into the group. `[UNPUBLISHED, LOCKED]` would then read
`(deleted = false or live_inode is null or locked_by is not null)`, which matches essentially every
row in the folder — a filter that silently stops filtering.

Note that a single-status selection produces a one-disjunct group, so `[ARCHIVED]` is still exactly
`cvi.deleted = true`. OR and AND only diverge from two statuses upward.

*(The baseline-vs-flag distinction was raised by the automated spec review on
[#37170](https://github.com/dotCMS/core/pull/37170); the OR semantics were settled separately during
planning, see the OR rationale above.)*

### `LOCKED` and version scoping compose

`LOCKED` does not constrain which version is joined, so it stacks on whatever `showWorking` already
selected. That is the same pairing the legacy portlet uses: `ContentletAjax.java:1018` appends
`+locked:true` and `:1035` unconditionally appends `+working:true`.

One deliberate difference: legacy **always** scopes to the working version, whereas here the drive
scopes to working because `AbstractDriveRequestForm.live()` defaults to `false`. A caller that sets
`live: true` with `status: ["LOCKED"]` therefore gets "live content that is locked" — a coherent,
strictly more expressive query, not a bug. Only `ARCHIVED` and `UNPUBLISHED` force working-version
scoping, because neither state can have a live version at all.

---

## Query shape: `BrowserQuery.contentStatuses`

`dotCMS/src/main/java/com/dotcms/browser/BrowserQuery.java`

```java
final Set<ContentStatus> contentStatuses;          // never null; empty means no filtering
public Set<ContentStatus> getContentStatuses()     // accessor, mirrors getFieldCriteria()
Builder withContentStatuses(@Nonnull Set<ContentStatus>)
```

Plumbed exactly like `workflowSchemeIds`: builder field (`LinkedHashSet`, insertion-ordered for
stable generated SQL), `Set.copyOf` in the constructor, a line in the copy-constructor, and a line
in `toString()`.

**One derived field changes.** The constructor's

```java
this.showWorking = builder.showWorking || builder.showArchived;
```

must also be true when the selection contains `ARCHIVED` or `UNPUBLISHED`. Both states imply no live
version, so without this `selectQuery` joins on `live_inode` — which those rows never have — and the
filter silently returns nothing (research.md R4).

---

## Frontend shape

### Filter-bag entry

`core-web/libs/portlets/dot-content-drive/portlet/src/lib/shared/models.ts`

```ts
export type DotKnownContentDriveFilters = {
    // …
    status: string[];   // 'ARCHIVED' | 'UNPUBLISHED' | 'LOCKED'
};
```

| Aspect | Behavior | Why |
|---|---|---|
| URL encoding | `status:ARCHIVED,LOCKED` | `encodeFilters` already comma-joins arrays — no change |
| URL decoding | `status: multiSelector` in `decodeByFilterKey` | one line; splits on comma |
| Seeded default | **No** | Empty genuinely means "off", unlike `languageId`/`sharedAssets` |
| "Clear all" | Cleared automatically | `clearFilters()` re-seeds only defaults, so `status` drops |
| Chip visibility | Automatic | `hasNonDefaultFilters` returns `true` for any non-default key |

### Request field

`core-web/libs/dotcms-models/src/lib/dot-content-drive.model.ts`

```ts
export interface DotContentDriveSearchRequest {
    // …
    status?: string[];
}
```

Sent only when non-empty. The `archived: false` pin is **removed** — the form's own `archived()`
already defaults to `false`, so omitting it produces an identical query while letting the status
selection own the archived decision (FR-019).

### Option list

`core-web/libs/portlets/dot-content-drive/portlet/src/lib/shared/constants.ts`

```ts
export const STATUS_FILTER_KEY = 'status';

export const CONTENT_STATUS = {
    ARCHIVED: 'ARCHIVED',
    UNPUBLISHED: 'UNPUBLISHED',
    LOCKED: 'LOCKED'
} as const;

export const STATUS_FILTER_OPTIONS: { value: string; labelKey: string }[] = [
    { value: CONTENT_STATUS.ARCHIVED,    labelKey: 'content-drive.status-filter.archived' },
    { value: CONTENT_STATUS.UNPUBLISHED, labelKey: 'content-drive.status-filter.unpublished' },
    { value: CONTENT_STATUS.LOCKED,      labelKey: 'content-drive.status-filter.locked' }
];
```

Shape follows `FOLDER_UPLOAD_BEHAVIOR_OPTIONS` in the same file. Order is display order: Archived
first because it is the capability people currently leave Content Drive to get (US1).

---

## What this feature does **not** change

- **No schema migration.** `deleted`, `live_inode` and `locked_by` all predate this work.
- **No index mapping change.** `deleted`, `live` and `locked` are already mapped
  (`ESMappingAPIImpl.java:527` for `locked`).
- **No change to `BrowserQuery.showArchived`.** Its inclusive meaning and the legacy Site Browser
  checkbox that depends on it are untouched (FR-008).
- **No new API surface.** One optional field on an existing request body; `openapi.yaml` is
  regenerated, not hand-edited.
