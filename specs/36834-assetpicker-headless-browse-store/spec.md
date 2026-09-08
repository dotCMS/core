# Feature Specification: Headless Browse Store for AssetPicker

**Feature Branch**: `issue-36702-asset-picker`

**Created**: 2026-08-07

**Status**: Draft

**Type**: Task (enabling refactor — AssetPicker 4/7)

**GitHub Issue**: [dotCMS/core#36834](https://github.com/dotCMS/core/issues/36834) (parent epic: [#36702](https://github.com/dotCMS/core/issues/36702))

**Input**: User description: "Create a headless browse SignalStore for AssetPicker that builds the same style of `DotContentDriveSearchRequest` as Content Drive, but **without** `ActivatedRoute`, URL sync, or editor navigation. Store responsibilities: site/asset path; sidebar folder tree state (expand / load-more via shared helpers); items, pagination/sort, loading/error; filters (text, contentTypes, baseTypes, language, silent `mimeTypes`); always `showFolders: false` for picker browse requests; selection (single asset). Must NOT include: query-param / `Location.go` sync; `DotContentDriveNavigationService`; context menu / dragging / Add New dialogs."

---

## Scope Note *(read this first)*

This issue delivers **state management only — no UI**. The `DotAssetPicker` shell that renders this
store lands in [#36835](https://github.com/dotCMS/core/issues/36835) (5/7), and the File/Image entry
points that configure it land in [#36836](https://github.com/dotCMS/core/issues/36836) (6/7).

The user stories below are therefore written as **capabilities the store must provide**, and every
"Independent Test" is a store-level unit test, not a click-through. This is deliberate: the store is
what makes the picker's behavior correct, and it is testable in isolation *before* any UI exists. A
user story that could only be verified through a shell that does not yet exist would be untestable
in this issue and belongs in 5/7.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A File field browses assets, never folders (Priority: P1)

An editor opens the picker from a **File** field. The store requests assets for the current site and
folder with no content-type restriction, pre-selecting only the contentlet's locale. Folders never
appear in the result list — the list is for picking an asset, and the folder tree on the left is how
you navigate.

**Why this priority**: This is the baseline entry point. Without it there is no picker. The
"no folders in the list" rule is a hard invariant of the epic's design and the single clearest
behavioral difference from Content Drive, where folders and content share the list.

**Independent Test**: Configure the store for a File field (site + locale, no base types) and assert
the generated search request: `showFolders: false`, `language` set to the given locale, no
`contentTypes`, no `baseTypes`, no `mimeTypes`.

**Acceptance Scenarios**:

1. **Given** the store is configured with a site and a locale, **When** the search request is built,
   **Then** it carries that locale and no content-type or base-type restriction.
2. **Given** any combination of filters is applied afterwards, **When** the search request is rebuilt,
   **Then** `showFolders` is still `false`.
3. **Given** the host supplies a starting folder path, **When** the request is built, **Then**
   `assetPath` addresses that folder on that site.

---

### User Story 2 - An Image field silently narrows to images (Priority: P1)

An editor opens the picker from an **Image** field. The store additionally restricts the search to
the `dotAsset` and `File Asset` base types and applies a mimetype filter. The mimetype filter is
**invisible to the editor** — it produces no chip and cannot be cleared, because an Image field that
could return a PDF would be broken.

**Why this priority**: Equal to User Story 1 — it is the second of the two entry points, and the
silent-filter behavior is the requirement most likely to be implemented wrong (as a normal, visible,
clearable filter).

**Independent Test**: Configure the store for an Image field (site + locale + base types +
`mimeTypes: ['image/*']`) and assert the generated request carries all four, then assert that the
user-facing filter state exposed for chip rendering contains no mimetype entry.

**Acceptance Scenarios**:

1. **Given** an Image-field configuration, **When** the search request is built, **Then** it carries
   `baseTypes: ['DOTASSET', 'FILEASSET']` and `mimeTypes: ['image/*']`.
2. **Given** an Image-field configuration, **When** the user-facing filter state is read, **Then**
   the mimetype restriction is absent from it — nothing downstream can render it as a chip.
3. **Given** the user clears every filter they can see, **When** the search request is rebuilt,
   **Then** the mimetype restriction is still applied.

---

### User Story 3 - Browsing the picker never disturbs the page behind it (Priority: P1)

An editor is editing a contentlet, opens the picker, navigates folders, searches, filters and
paginates, then closes it. The browser URL never changed, the browser history gained no entries, and
pressing Back afterwards behaves exactly as it would have if the picker had never been opened.

**Why this priority**: P1 rather than a nice-to-have because it is a **correctness** requirement, not
a polish one. The picker opens *on top of* the Edit Contentlet screen, whose own URL state is owned
by the editor. A store that wrote query params — as Content Drive's does — would corrupt the host
page's history and could navigate the editor away mid-edit, losing unsaved work.

**Independent Test**: Instantiate the store in a test with **no router providers at all**. If any
part of the store injects `ActivatedRoute` or `Location`, instantiation throws and the test fails.
Then exercise every state-mutating method and assert no URL API is touched.

**Acceptance Scenarios**:

1. **Given** a test environment with no router configured, **When** the store is instantiated,
   **Then** it constructs successfully.
2. **Given** the store is running, **When** path, filters, sort, pagination or selection change,
   **Then** no browser URL or history API is invoked.
3. **Given** an asset is double-clicked or selected, **When** the store handles it, **Then** no
   navigation to the content or page editor occurs.

---

### User Story 4 - Navigating folders and paging results (Priority: P2)

An editor expands folders in the tree, loads more folders when a level is truncated, selects a
folder to scope the list, and pages through results — all using the same folder and search APIs
Content Drive uses, so the two views can never disagree about what exists.

**Why this priority**: P2 because Stories 1–3 define whether the picker is *correct*; this defines
whether it is *usable at scale*. A picker that works only on small sites still demonstrably works.

**Independent Test**: Drive the store's folder-tree methods against mocked folder APIs and assert the
resulting tree shape (including load-more sentinels); drive pagination and assert the cursor advances
in the outgoing request.

**Acceptance Scenarios**:

1. **Given** a starting path several levels deep, **When** the tree loads, **Then** it is expanded
   down to that path with the target folder selected.
2. **Given** a folder level with more children than one page, **When** it loads, **Then** a load-more
   affordance is present carrying the next page cursor.
3. **Given** the user selects a folder, **When** the search request is rebuilt, **Then** it addresses
   that folder and pagination has reset to the first page.
4. **Given** the user advances a page, **When** the request is rebuilt, **Then** it carries the next
   content cursor.

---

### User Story 5 - A failed search leaves the picker usable and says so (Priority: P3)

If the search or folder API fails, the editor sees the failure reported through the platform's normal
error surface and the picker stays open and interactive — they can adjust a filter and retry rather
than staring at a spinner or an empty list that looks like "no results".

**Why this priority**: P3 — it does not block the happy path, but silently swallowing an error is
actively misleading here: an empty list is indistinguishable from a failed request, so the editor
concludes the asset does not exist and goes off to re-upload it.

**Independent Test**: Make the mocked search API error and assert the store reports through
`DotHttpErrorManagerService`, leaves status in a terminal (non-loading) state, and still accepts
subsequent filter changes and retries.

**Acceptance Scenarios**:

1. **Given** the search API fails, **When** the store handles the response, **Then** the error is
   reported through the shared HTTP error manager, not swallowed or `console.error`-ed.
2. **Given** a failed search, **When** the state is read, **Then** status is not left as "loading".
3. **Given** a failed search, **When** the user changes a filter, **Then** a new search is issued.

---

### Edge Cases

- **The store is created before it is configured.** A dialog host constructs the store, then calls
  init with the field's configuration. Between those two moments the store must not issue a search
  against an undefined site. (Content Drive gets away with an equivalent gap because its URL-driven
  init effect runs at construction; the picker has no such trigger.)
- **No starting path.** When the host supplies no folder (first ever use, before 6/7's remembered
  path exists), the store browses the site root.
- **A folder path that no longer exists** — e.g. a remembered path from 6/7 pointing at a since-deleted
  folder. The picker must fall back to a browsable location rather than dead-ending.
- **An empty result set** is a legitimate outcome and must be distinguishable from a failure
  (User Story 5).
- **Locale with no assets**: the editor's contentlet language may simply have no assets. Empty list,
  not an error.
- **Rapid filter changes** (typing in search while a request is in flight) must not let a stale
  response overwrite a newer one.
- **Reopening the picker** in the same session must start from the configured state, not from
  whatever the previous session left behind.
- **`SYSTEM_HOST`** is not a browsable site; a configuration naming it must not fire a search.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The store MUST build a drive search request from its own state, addressing a site and
  folder path supplied by its host.
- **FR-002**: The store MUST set `showFolders: false` on every browse request it issues, regardless
  of filter state. This is an invariant, not a default.
- **FR-003**: The store MUST support these user-facing filters: free-text search, content types, base
  types, and language.
- **FR-004**: The store MUST support a mimetype restriction supplied by the host that is applied to
  every request and is NOT exposed as user-facing filter state (so no UI can render it as a chip or
  offer to clear it).
- **FR-005**: The store MUST accept its full configuration — site, starting path, pre-selected locale,
  allowed base types, mimetypes — explicitly from its host, and MUST NOT read any of it from the URL
  or from global navigation state.
- **FR-006**: The store MUST NOT depend on `ActivatedRoute` or `Location`, and MUST NOT write to the
  browser URL or history.
- **FR-007**: The store MUST NOT navigate to the content or page editor.
- **FR-008**: The store MUST maintain sidebar folder-tree state — the tree, the selected node, per-level
  expansion and load-more paging — using the shared folder helpers, not a private implementation.
- **FR-009**: The store MUST maintain list state: items, pagination, sort, and a status distinguishing
  loading, loaded, and error.
- **FR-010**: The store MUST maintain a **single** asset selection.
- **FR-011**: The store MUST use the shared drive search and folder APIs. It MUST NOT call
  `/api/v1/browser`.
- **FR-012**: The store MUST report API failures through the platform's shared HTTP error handling and
  MUST leave itself in a state that accepts a retry.
- **FR-013**: The store MUST NOT issue a search before it has been configured with a browsable site.
- **FR-014**: The store MUST NOT include context-menu, drag-and-drop, workflow-action, or content-creation
  state.
- **FR-015**: The store MUST live in a shared location reachable by the AssetPicker shell, and MUST NOT
  import from any portlet library.

### Key Entities

- **Picker configuration**: What the host hands the store when the picker opens — the site to browse,
  an optional starting folder, an optional pre-selected locale, optional allowed base types, and
  optional mimetypes. Two shapes matter: the File-field shape (locale only) and the Image-field shape
  (locale + base types + mimetypes).
- **Browse request**: The search payload sent for each browse. Derived entirely from configuration +
  filter/pagination/sort state; never assembled by callers.
- **Filter state**: The subset of the request the editor can see and change. Deliberately excludes the
  mimetype restriction (FR-004).
- **Folder tree node**: A node in the sidebar, either a real folder or a synthetic "load more" sentinel
  carrying a paging cursor.
- **Selected asset**: The single asset the editor has chosen; what the picker ultimately returns.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of browse requests carry `showFolders: false`, verified across every filter
  permutation exercised in tests.
- **SC-002**: A File-field configuration produces a request with the locale set and no content-type,
  base-type, or mimetype restriction — asserted exactly, field by field.
- **SC-003**: An Image-field configuration produces a request with the locale, base types, and
  `mimeTypes: ['image/*']` — asserted exactly, field by field.
- **SC-004**: The store instantiates and operates in a test with zero router providers configured.
- **SC-005**: Zero references to `ActivatedRoute`, `Location`, `Router`, or `/api/v1/browser` in the
  store's source, verified by inspection of the shipped files.
- **SC-006**: Zero imports from any portlet library in the new code.
- **SC-007**: Existing Content Drive behavior is unchanged: the portlet's full test suite passes at its
  current count with no test modified to accommodate this work, except where a symbol was relocated.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Drive's own store is **not modified**. The one shared-code
  change is relocating the folder-tree constants (`ALL_FOLDER`, the load-more label key, and the
  duplicated `SYSTEM_HOST_ID`) out of the Content Drive UI library into the shared UI library, because
  the shared library cannot depend on a portlet. Content Drive keeps consuming the same values from
  their new home. This follows the relocation pattern already used three times in this epic
  (`DotFolderListView` in 1/7, the folder-tree helpers in 2/7, the chip-filter kit in 3/7).
- **Backward-compatibility expectations**: Content Drive's URL behavior, deep links, and browser
  Back/Forward restore must all keep working exactly as today — none of that code is touched. The
  drive search endpoint contract is unchanged: `mimeTypes` and `showFolders` already exist on it and
  are simply being used by a second caller.
- **Deliberate divergence to record**: this store duplicates the *shape* of Content Drive's request
  builder and sidebar logic rather than sharing an implementation. Unifying them would mean refactoring
  a 507-line store whose initialization is welded to the router, mid-epic. The duplication is accepted
  as known debt, revisitable once the AssetPicker series is complete and both call sites are stable.
- **Known related decisions**: the numeric base-type encoding (`1`=CONTENT … `9`=DOTASSET) exists only
  to make Content Drive's filters URL-safe. The picker has no URL, so it uses base-type **names**
  throughout. This is a divergence on purpose, not an oversight.

## Assumptions

1. The picker **has** a folder-tree sidebar. The epic's "the right section lists no folders" refers to
   the result list, which is exactly `showFolders: false`; this issue's own description asks for
   "sidebar folder tree state (expand / load-more via shared helpers)".
2. **Upload is out of scope here.** The epic requires upload parity with Content Drive, but this issue
   lists no upload responsibility — the shell wires the upload trigger later in the series.
3. **User Searchable and workflow filters are out of scope.** The epic marks User Searchable explicitly
   out of scope; workflow filtering is not listed among this store's filters.
4. The site to browse arrives through configuration rather than the global site switcher, so a host can
   pin the picker to a specific site.
5. Because `showFolders` is always false, folder paging never advances — only the content cursor does.
   The store is not obliged to reproduce Content Drive's dual-cursor page reconciliation.
6. The store is provided per picker instance (component-scoped), not application-wide, so each opening
   starts clean.
7. The relocated `ALL_FOLDER` label keeps its existing `content-drive.*` i18n key. Renaming keys in
   `Language.properties` is separate work; the same trade-off was accepted in 3/7 for the upload labels.

## Open Questions

- None blocking. Assumptions 1–7 were reviewed with the developer on 2026-08-07 and accepted; any of
  them turning out false changes scope and should reopen this section.
