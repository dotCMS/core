# Phase 1 Data Model: Content Drive Status Filter

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-08-24

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

**Relationships**: none. The three are orthogonal facts about one row, which is exactly why they
combine with AND rather than OR (see [research.md R2](./research.md)).

**Not a state machine**: an item can hold any subset of the three at once. Two subsets are worth
naming because they are user-visible oddities, not defects:

- `{ARCHIVED, UNPUBLISHED}` is always equivalent to `{ARCHIVED}` — archiving removes the live
  version (`ESContentletAPIImpl.java:3833`), so every archived item is already unpublished.
- `{ARCHIVED, LOCKED}` is reachable but rare: it needs a self-lock or a CMS-Admin archive
  (`canLock` at `:10380`/`:10406`), and `internalArchive` never clears `locked_by`.

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

**Declared as `List<String>`, not `List<ContentStatus>`** — see [research.md R7](./research.md). The
helper owns the parse so the 400 is thrown explicitly, matching the `userSearchable` precedent
already in `ContentDriveHelper`.

### Validation rules

| Rule | Source | Enforced in |
|---|---|---|
| Empty is valid and means "no status filtering" | FR-001, FR-002 | `ContentDriveHelper` (block is skipped) |
| Every element must name a `ContentStatus` | FR-010 | `ContentDriveHelper.parseStatuses` → `BadRequestException` |
| Selection narrows (AND), never widens | FR-006 | `BrowserAPIImpl.appendContentStatusQuery` — independent `and` clauses |
| The archived baseline stands unless `ARCHIVED` is selected | FR-004 | `BrowserAPIImpl:2006` — the exclusion is skipped only when the selection contains `ARCHIVED` |
| A non-empty selection excludes folders | FR-015 | `ContentDriveHelper` → `.showFolders(false)` |

### The archived baseline is not a fourth flag

FR-006 says the statuses combine with AND, and FR-004 says `UNPUBLISHED` excludes archived content
unless `ARCHIVED` is also selected. Read as "a pure AND of three independent flags", those look like
they disagree. They don't, and an implementer who misses the distinction will get `UNPUBLISHED`
wrong.

**Excluding archived content is the drive's pre-existing default, not a member of this set.**
`appendExcludeArchivedQuery` already emits `cvi.deleted = false` on every request today. The three
statuses are AND-ed *on top of* that baseline; `ARCHIVED` is the only one that lifts it.

So the generated predicate is:

| Selection | Baseline | Status clauses | Net |
|---|---|---|---|
| `[]` | `deleted = false` | — | today's behavior |
| `[UNPUBLISHED]` | `deleted = false` | `live_inode is null` | unpublished **and not archived** |
| `[LOCKED]` | `deleted = false` | `locked_by is not null` | locked **and not archived** |
| `[ARCHIVED]` | *lifted* | `deleted = true` | archived only |
| `[ARCHIVED, LOCKED]` | *lifted* | `deleted = true` + `locked_by is not null` | archived **and** locked |

This falls out of the code shape rather than needing special handling: the baseline is skipped only
when the selection contains `ARCHIVED`, so `UNPUBLISHED`/`LOCKED` alone keep it automatically.

*(Raised by the automated spec review on [#37170](https://github.com/dotCMS/core/pull/37170).)*

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
version, so without this the query joins on `live_inode` and can never match. See
[research.md R4](./research.md).

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
