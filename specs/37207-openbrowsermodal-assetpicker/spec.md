# Feature Specification: Migrate `openBrowserModal` to the new AssetPicker (Angular host)

**Feature Branch**: `nicobytes/37207-migrate-openbrowsermodal-to-the-new-assetpicker-and-delete-the-legacy-browser-selector`

**Created**: 2026-08-28

**Status**: Draft — planned; `DotCustomFieldApi` redesign adopted 2026-08-28

**Related GitHub Issue**: [#37207](https://github.com/dotCMS/core/issues/37207) — epic [#36702](https://github.com/dotCMS/core/issues/36702)

**Input**: User description: "Migrate `DotCustomFieldApi.openBrowserModal()` (AngularFormBridge) from the legacy `DotBrowserSelectorComponent` to the new `DotAssetPickerComponent`, extend the AssetPicker to cover the browse contract (folders, menu links via the #37112 `showLinks`/`linkCursor` API, pages/HTMLPAGE, plus `showWorking`/`showArchived`/`showDotAssets`/`sortByDesc`/`extensions`), keep `BrowserSelectorOptions`/`BrowserSelectorResult`/`BrowserSelectorController` unchanged as a published VTL public API, then delete the entire `core-web/libs/ui/src/lib/components/dot-browser-selector/` subtree and its export from `libs/ui/src/index.ts`. Also resolve the conflicting issue #37132 by reorienting or closing it."

---

## ⚠ Premise Correction and Scope Decision

### The issue's "Current state" is stale

Issue #37207 states that `openBrowserModal` is the **only remaining consumer** of the legacy
picker, "confirmed by grep across `libs` + `apps`". That was true when the issue was written. It
is **no longer true on `main`**.

PR [#37196](https://github.com/dotCMS/core/pull/37196) — *"fix(edit-content): pick the asset picker
per host"*, the fix for [#37132](https://github.com/dotCMS/core/issues/37132) — **merged on
2026-08-25** (commit `aaed80b639`). It introduced the `ASSET_PICKER_LAUNCHER` injection token and
deliberately **restored the legacy pickers as the legacy-Dojo-host pickers**. Issue #37132 itself
is still `OPEN` (not yet closed by QA), which is presumably why #37207 describes it as something to
"reorient or close" rather than something already delivered.

Verified consumer state on `main` today:

| Legacy component | Production consumers today |
|---|---|
| `DotBrowserSelectorComponent` | `angular-form-bridge.ts:533` (`openBrowserModal`), `new-block-editor/.../editor-modal.service.ts:204` (legacy host), `edit-content/.../dot-file-field.component.ts:942` (legacy host) |
| `DotAssetSearchDialogComponent` | `edit-content/.../dot-wysiwyg-plugin.service.ts:227` (legacy host) |

### Governing rule (decided 2026-08-28)

> **If the component is launched by Dojo / the old editor, it shows the picker that is already
> there. If it is launched by Angular — the new components — it uses the new AssetPicker.**

This **supersedes** #37207's stated decision that "the new AssetPicker becomes the single picker in
every host … and the legacy picker is deleted in this work". #37132 is therefore **confirmed as
correct and remains in force**, not reoriented or closed as a conflict.

Applied to `openBrowserModal`, the rule resolves cleanly, because the host seam it needs
**already exists** — it is the form bridge itself, not `ASSET_PICKER_LAUNCHER`:

| Host | Bridge (`createFormBridge`) | `openBrowserModal` behavior |
|---|---|---|
| New Angular Edit Content | `type: 'angular'` → `AngularFormBridge` | **Changes**: opens the new AssetPicker |
| Legacy Dojo edit contentlet | `type: 'dojo'` → `DojoFormBridge` | **Unchanged**: a documented no-op (`// TODO: Implement browser selector modal for Dojo`) |

`AngularFormBridge` is, by construction, only ever the Angular host. So migrating it *is* the rule,
and needs no new token.

### What this changes in this specification

1. **Deleting the legacy picker is out of scope.** It survives as the legacy-Dojo-host picker for
   the Story Block and File/Image entry points. Same for `DotAssetSearchDialogComponent` (WYSIWYG).
   Requirement Group C is reduced to *not regressing* those consumers.
2. **`ASSET_PICKER_LAUNCHER` stays.** #37207's criterion "no `ASSET_PICKER_LAUNCHER`-style
   host-discriminating token is introduced" is inverted: the existing token is the mechanism that
   implements the governing rule and must be preserved.
3. **#37207's title over-promises.** After this work the legacy Browser Selector still exists, with
   two legacy-host consumers instead of three. The issue should be retitled or split — see FR-023.
4. **Groups A, B and E are unaffected** and carry all the editor-visible value.

---

## Context

`DotCustomFieldApi.openBrowserModal()` is the browse API for **custom-field VTL templates** — it
lets editors browse the site and pick a file, page, folder or menu link, and the template reads the
selection's `url` back into its field. It is **new and unshipped** (see *API Redesign*), so its only
consumers today are two templates that ship with the product:

| Template | What it asks for | What it reads |
|---|---|---|
| `file_browser_field_render_new.vtl` | files + pages + folders, no dotAssets, live only, not archived, newest first | `result.url` |
| `redirect_custom_field_new.vtl` | pages + **menu links**, no files, no folders, no dotAssets, live only, not archived, newest first | `result.url` |

Today this opens the legacy Browser Selector. Every other asset-selection entry point moved to the
new AssetPicker in [#36944](https://github.com/dotCMS/core/pull/36944) (in the Angular host; #37196
then restored the legacy picker for the Dojo host).

**This is not a component swap.** `openBrowserModal` is a *browser* — it returns pages, folders and
menu links. The AssetPicker is an *asset picker* — it deliberately excludes all three:

| `openBrowserModal` capability | AssetPicker today |
|---|---|
| `showFolders` | Hardcoded `showFolders: false` as a **documented invariant**; the paging model pins `folderCursor: 0` because "with `showFolders: false` the folder cursor never advances" |
| `showLinks` | No support. Newly possible only because [#37112](https://github.com/dotCMS/core/pull/37112) added `showLinks` / `linkCursor` / `hasMoreLinks` / `nextLinkCursor` to the backend browse API. The **frontend Drive request/response models do not yet carry any of those four fields** |
| `showPages` | Restricted to `DOTASSET` + `FILEASSET`; there is no `HTMLPAGE` path |
| `showWorking`, `showArchived`, `showDotAssets`, `sortByDesc`, `extensions` | No equivalents on the picker's configuration; the picker hardcodes `archived: false` |
| Result must carry a usable `url` for every kind | The picker returns contentlets; `url` is the only field either shipped template reads |

So the picker has to *grow the browse contract* before it can stand in — and it has to grow it
**opt-in**, so the File / Image / video / audio entry points keep returning assets only.

---

## Clarifications

### Session 2026-08-28

- Q: Should this work define how folder rows behave in the picker's list (single click selects vs. double click navigates)? → A: No. This work opens the AssetPicker and stops there; how the picker behaves once open is the picker's own responsibility and must not be changed or dictated from here.
- Q: Does the picker still gain folder / menu-link / page capability, or is it opened strictly as-is? → A: It gains the capability — the point is to open *and select*, so the shipped templates must keep working. But this PR must not change the picker's existing behaviors: it already navigates and selects, and those stay untouched. Additions are purely additive, opt-in configuration.
- Q: What happens to `ContentByFolderParams` flags the browse endpoint cannot express? → A: Verified against `/api/v1/drive/search` — #37112 already covers everything the two shipped templates need. `extensions` is the single genuine gap, so the spec grows to close it in the endpoint rather than dropping it. See *Capability Mapping* below.
- Q: Is `DotCustomFieldApi` a shipped contract that must stay backward-compatible? → A: **No.** It is new and no customer uses it yet — the `_new.vtl` templates only render when the new Edit Content is enabled, which is not the default. This is the moment to fix the API's design; backward compatibility is not required. **Scope of this freedom is `DotCustomFieldApi` only** — `/api/v1/drive/search` has other consumers and MUST keep working (additive changes only).
- Q: What shape should the selection result take? → A: A union discriminated by `kind` (`file` / `dotasset` / `page` / `folder` / `link`), with `identifier`, `title` and a non-empty `url` on every variant and contentlet-only fields confined to the variants that have them.
- Q: Callback or Promise? → A: A handle exposing a `result` Promise, keeping `close()` for programmatic dismissal. One way to do it, not two.
- Q: Redesign the request parameters? → A: Yes, fully — `kinds[]` replaces five booleans, `status` replaces two, `sort` replaces `sortByDesc`, and `path` (optional, path-based) replaces the mandatory id-based `hostFolderId`.

---

## API Redesign — `DotCustomFieldApi.openBrowserModal` is new and unshipped

`DotCustomFieldApi` has **no customer consumers**. Verified: `file_browser_field_render.vtl` and
`redirect_custom_field.vtl` are dispatchers —
`#if($structures.isNewEditModeEnabled())` selects `_new.vtl`, otherwise `_old.vtl` — so the two
`_new` templates only render when the new Edit Content is enabled, which is not the default.
`browser-selector.interface.ts` has never been versioned (single commit, #34126).

So this is the moment to fix the API rather than preserve it. **Backward compatibility is not
required** for `DotCustomFieldApi`.

**This freedom does not extend to `/api/v1/drive/search`**, which serves the Content Drive portlet
and the AssetPicker itself. Every change there stays additive and backward-compatible (FR-033).

### Defects being fixed

| # | Today | Problem | Fix |
|---|---|---|---|
| 1 | `BrowserSelectorResult` is contentlet-shaped: `inode`, `mimeType`, `baseType`, `contentType` | Meaningless for a folder or a menu link; forced FR-011 into an impossible "all fields populated" rule and is the root of the selection-pipeline break | Union discriminated by `kind` |
| 2 | `onClose(result \| null)` callback + returned controller | Callback nesting in template code for a one-shot dialog | Handle with a `result` Promise, keeping `close()` |
| 3 | Five booleans for content kind (`showFiles`, `showPages`, `showFolders`, `showLinks`, `showDotAssets`) | Verbose; no way to express "these kinds"; invalid combinations representable | `kinds: DotBrowserItemKind[]` |
| 4 | `showWorking` + `showArchived` | Three states in two booleans, with a meaningless combination | `status: 'live' \| 'working' \| 'archived'` |
| 5 | `sortByDesc: boolean` | Direction with no field; diverges from the Drive API's `field:direction` | `sort?: { field, direction }` |
| 6 | `hostFolderId: string`, **required**, id-based | Required yet no template passes it; id-based while the browse endpoint is path-based, and id→path resolution would need the `byPath` endpoint ADR-0020 deprecates | `path?: string`, optional, path-based |

### Target shape

```ts
type DotBrowserItemKind = 'file' | 'dotasset' | 'page' | 'folder' | 'link';

interface DotBrowserOptions {
    title?: string;
    kinds?: DotBrowserItemKind[];              // default: ['file', 'dotasset']
    status?: 'live' | 'working' | 'archived';  // default: 'working'
    path?: string;                             // `//site/folder/`; omit for the picker's default
    mimeTypes?: string[];
    extensions?: string[];
    sort?: { field: string; direction: 'asc' | 'desc' };
}

interface DotBrowserSelectionBase {
    kind: DotBrowserItemKind;
    identifier: string;
    title: string;
    url: string;        // always non-empty — the value a field stores
}

type DotBrowserSelection =
    | (DotBrowserSelectionBase & { kind: 'file' | 'dotasset'; inode: string; name: string;
                                   mimeType: string; baseType: string; contentType: string })
    | (DotBrowserSelectionBase & { kind: 'page'; inode: string; baseType: 'HTMLPAGE';
                                   contentType: string })
    | (DotBrowserSelectionBase & { kind: 'folder'; inode: string })
    | (DotBrowserSelectionBase & { kind: 'link'; inode: string });

interface DotBrowserHandle {
    readonly result: Promise<DotBrowserSelection | null>;  // null = cancelled
    close(): void;                                          // resolves `result` with null
}

openBrowserModal(options?: DotBrowserOptions): DotBrowserHandle;
```

Consumer code becomes:

```js
const { result } = DotCustomFieldApi.openBrowserModal({
    title: "Select a Page", kinds: ["page", "link"], status: "live",
    sort: { field: "modDate", direction: "desc" }
});
const selection = await result;
if (selection) field.setValue(selection.url);
```

---

## Scope Boundary — this work opens the picker, it does not redesign it

This work is *open the picker and get a selection back*. The AssetPicker already knows how to
navigate and how to select, and those behaviors stay exactly as they are. This specification
describes **what a caller can ask for and what it gets back**, never how the picker looks or how
the editor interacts with it once it is open. In particular, the following are explicitly **out of
scope** and are the picker's own responsibility:

- How a folder row responds to a click, a double click, or the keyboard, and how it relates to the
  sidebar tree's navigation.
- The layout, the toolbar, the filter chips, the language filter, the upload affordances, the
  splitter, and the footer.
- Sorting, searching and selection interaction models.

Where a requirement below says the picker "lists" or "returns" something, it constrains the
**result set and the returned value only**. Any acceptance scenario that appears to describe
on-screen interaction is describing the observable outcome, not prescribing a UX design.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A custom-field editor browses for a file or page and gets a usable path (Priority: P1)

A content editor is working in the **new Angular Edit Content** on a contentlet whose content type
has a custom field built on `openBrowserModal` — the shipped **file browser** field. They click
*Browse*, a picker opens, they navigate the site tree, pick a file or a page, and the field's text
input is filled with that asset's path. They save; the value persists.

**Why this priority**: This is the whole point of the API and it is the path every existing
customer template already depends on. If only this ships, the migration has delivered its core
value and no customer field is broken.

**Independent Test**: Open a contentlet with the shipped `file_browser_field_render_new.vtl` field
in the new Edit Content, browse, select a file, confirm the path lands in the input and in the
saved field value. Repeat for a page. No other story needs to be built first.

**Acceptance Scenarios**:

1. **Given** a custom field using `openBrowserModal` with files, pages and folders requested,
   **When** the editor clicks *Browse*, **Then** the new AssetPicker opens showing files, pages and
   folders for the current site, with the dialog title the caller supplied.
2. **Given** the picker is open, **When** the editor selects a **file**, **Then** the picker closes
   and the caller's callback receives a result whose `url` is that file's path, and the field's
   input and value are set to it.
3. **Given** the picker is open, **When** the editor selects a **page**, **Then** the callback
   receives a result whose `url` is that page's URL.
4. **Given** the picker is open, **When** the editor dismisses it by the close control, by `Esc`,
   or by clicking the mask, **Then** the callback is invoked exactly once with `null` and the field
   value is untouched.
5. **Given** a caller holds the returned controller, **When** it calls `close()`, **Then** the
   dialog closes and the callback receives `null` exactly once.

---

### User Story 2 - A custom-field editor picks a folder or a menu link (Priority: P1)

The same editor, in the shipped **redirect** field, needs to point a page redirect at a **menu
link**; or, in the file browser field, needs to select a **folder** rather than a file. Both must
return a usable path.

**Why this priority**: P1 alongside Story 1, not below it — the shipped `redirect_custom_field_new.vtl`
requests `showLinks: true`, and `file_browser_field_render_new.vtl` requests `showFolders: true`.
Ship Story 1 without this and one of the two shipped templates silently loses a content kind it
could previously select. This is the capability gap that makes the migration non-trivial.

**Independent Test**: Open the shipped redirect field, browse, select a menu link, confirm the
redirect input receives the link's URL. Separately, open the file browser field, select a folder,
confirm the folder path lands in the input.

**Acceptance Scenarios**:

1. **Given** a caller requests menu links, **When** the picker loads a folder containing menu
   links, **Then** those links appear in the list alongside whatever else was requested.
2. **Given** a folder contains more menu links than fit on one page, **When** the editor pages
   forward and back, **Then** every link is reachable exactly once and no link or asset is skipped
   or duplicated.
3. **Given** the editor selects a **menu link**, **Then** the callback receives a result whose
   `url` is that link's URL.
4. **Given** a caller requests folders, **When** the picker lists a folder's contents, **Then**
   child folders are among the listed results and can be chosen, and paging across folders behaves
   as in scenario 2. *(How a folder row responds to input is the picker's own concern — see Scope
   Boundary.)*
5. **Given** the editor chooses a **folder**, **Then** the callback receives a result whose `url`
   is that folder's path.
6. **Given** a caller requests neither folders nor links, **Then** neither appears in the list —
   the additions are opt-in.

---

### User Story 3 - Existing asset entry points behave exactly as they do today (Priority: P1)

An editor uses the File field, the Image field, the WYSIWYG *Add image* button, or a Story Block
image / video / audio node — **in either host**. Nothing they see or can select changes.

**Why this priority**: P1 because it is a **non-regression guarantee**, not an enhancement. The
picker is shared; widening it for `openBrowserModal` is exactly how these entry points would
accidentally start offering folders, pages or menu links to fields that cannot store them. It also
guards the governing rule: the Dojo host must keep the legacy pickers it was given by #37196.

**Independent Test**: Exercise each of the four entry points in the Angular host and assert the
result set contains no folders, no menu links and no pages, and that the media modes still silently
restrict to their mimetype. Then exercise the same four in the legacy Dojo host and assert each
still opens the legacy picker it opens today.

**Acceptance Scenarios**:

1. **Given** the Image field's picker in the Angular host, **When** it lists any folder, **Then**
   it shows only image assets — no folders, no links, no pages, no non-image files.
2. **Given** the File field's picker in the Angular host, **When** it lists any folder, **Then** it
   shows assets of any mimetype but still no folders, links or pages.
3. **Given** a Story Block video or audio node in the Angular host, **Then** its picker is narrowed
   to that mimetype as it is today.
4. **Given** the legacy Dojo edit contentlet, **When** the editor opens the Story Block image /
   video / audio picker or the File/Image field's *Select Existing File*, **Then** the legacy
   Browser Selector opens, exactly as it does today.
5. **Given** the legacy Dojo edit contentlet, **When** the editor clicks WYSIWYG *Add image*,
   **Then** the legacy asset-search dialog opens, exactly as it does today.
6. **Given** any of these entry points, **When** its picker pages through a large folder, **Then**
   paging behaves as it does today.

---

### User Story 4 - The legacy Browser Selector loses its last Angular-host consumer (Priority: P2)

A dotCMS engineer inspecting the codebase finds the legacy Browser Selector reachable only from the
legacy Dojo host. Nothing in the Angular path opens it any more.

**Why this priority**: P2 — this is the structural payoff of the migration and the precondition for
a future deletion, but it delivers no editor-visible behavior on its own and must not gate
Stories 1–3.

**Independent Test**: Search the frontend workspace for the legacy picker's exported name; confirm
every remaining reference is on a legacy-Dojo-host path guarded by the host seam, and that the
workspace lints, tests and builds clean.

**Acceptance Scenarios**:

1. **Given** the migration is complete, **When** the workspace is searched for the legacy picker,
   **Then** its only production references are the two legacy-host entry points, and the form
   bridge no longer imports it.
2. **Given** the migration is complete, **Then** the host-discriminating token and its legacy-host
   regression specs are intact and still passing.
3. **Given** the migration is complete, **Then** the frontend lint, unit test and build targets for
   the affected libraries all pass.

---

### Edge Cases

- **Cancel paths fire once.** Close control, `Esc`, mask click and programmatic `close()` must each
  produce exactly one `null` callback — never zero, never two. The legacy dialog was configured
  with `closeOnEscape: false`; the new one must still honor `Esc` as a cancel per Story 1
  scenario 4.
- **A folder or menu link has no `url`.** A folder's path and a link's URL come from different
  places than a contentlet's `url`. Every `DotBrowserSelection` variant must still carry a
  non-empty `url`, because it is the only field either shipped template reads. An empty `url` is a
  defect, not an acceptable outcome.
- **Mixed paging.** A folder holding folders *and* assets *and* menu links pages over three
  independent cursors. Reaching the end of one stream must not truncate the others, and no item may
  repeat when paging backwards.
- **`showFolders: false` is currently load-bearing.** The picker pins the folder cursor to zero
  *because* folders are never returned. Making folders opt-in must not leave the cursor pinned for
  callers who do want them.
- **Two pickers at once.** The dialog service refuses to open a second dialog of a type it already
  has open. A custom field that calls `openBrowserModal` while another field's picker is open must
  not silently get no dialog.
- **Links plus mimetypes.** The browse endpoint drops menu links whenever `mimeTypes` is set, since
  a link has no MIME type. A caller asking for both gets links silently omitted — a documented
  upstream rule this work surfaces rather than works around (FR-036).
- **`showFolders` defaults differ.** The endpoint defaults `showFolders` to `true`; the picker pins
  it `false`. The picker's default wins for callers that do not opt in (FR-007).
- **The Dojo host still does nothing.** `DojoFormBridge.openBrowserModal()` is a no-op today. Under
  the governing rule it keeps "what is already there" — which is nothing. A VTL template calling it
  in the legacy editor gets no dialog, before and after this work. This is preserved deliberately,
  not fixed here (see FR-022).
- **Caller-supplied title.** The picker renders its own header rather than the dialog's, so
  `options.title` must still reach the visible title.
- **Site scope.** `openBrowserModal` callers may pass a starting folder or none at all; with none,
  the picker must start somewhere sensible for the contentlet being edited. Neither shipped
  template passes one.

---

## Requirements *(mandatory)*

### Group A — Extend the picker's browse contract *(opt-in)*

- **FR-001**: The picker MUST be able to include folders in its results, and return a chosen
  folder, when the caller asks for them — and MUST NOT include them when the caller does not. How a
  folder is presented and interacted with is the picker's own concern (see Scope Boundary).
- **FR-002**: The picker's paging MUST remain correct when folders are included — it MUST NOT
  assume the folder cursor never advances.
- **FR-003**: The picker MUST be able to list menu links when the caller asks for them, consuming
  the browse API's link paging contract added in #37112 (`showLinks`, `linkCursor`, `hasMoreLinks`,
  `nextLinkCursor`).
- **FR-004**: The frontend browse request and response models MUST carry the four link-paging
  fields, which they do not today.
- **FR-005**: The picker MUST be able to list pages when the caller asks for them, without pages
  becoming reachable from the File, Image, video or audio entry points.
- **FR-006**: The picker MUST honor `showWorking`, `showArchived`, `showDotAssets`, `sortByDesc`
  and `extensions`. All five are expressible on the browse endpoint once Group E lands — see
  *Capability Mapping* — so none may be silently dropped.
- **FR-007**: Every capability added by Group A MUST be off by default, so a caller that asks for
  nothing new gets today's asset-only behavior.
- **FR-007a**: Group A MUST be **purely additive**. The picker already navigates and selects; this
  work MUST NOT change those existing behaviors, nor the picker's layout, toolbar, filters or
  upload affordances. A change is acceptable only where it is strictly required to *make a new
  capability expressible* — e.g. unpinning the folder cursor (FR-002) — and never to alter how the
  picker behaves for a caller that did not opt in.
- **FR-008**: The File, Image, video and audio entry points MUST keep their exact current
  narrowing: no folders, no menu links, no pages, and the same silent mimetype restriction.

### Group B — Migrate `openBrowserModal` in the Angular host

- **FR-009**: `AngularFormBridge.openBrowserModal()` MUST open the new AssetPicker.
- **FR-010**: The `openBrowserModal` contract MUST be replaced with the *Target shape* above:
  `DotBrowserOptions`, `DotBrowserSelection` (a union discriminated by `kind`) and
  `DotBrowserHandle`. `BrowserSelectorOptions`, `BrowserSelectorResult` and
  `BrowserSelectorController` are removed. This is safe because the API is unshipped — see *API
  Redesign*. **No equivalent freedom applies to `/api/v1/drive/search`** (FR-033).
- **FR-010a**: Both shipped VTL templates MUST be updated to the new shape in the same change, so
  the repository never contains a template written against the removed contract.
- **FR-011**: Every `DotBrowserSelection` variant MUST carry `kind`, `identifier`, `title` and a
  non-empty `url`. Contentlet-only fields (`inode`, `name`, `mimeType`, `baseType`, `contentType`)
  MUST appear only on the variants that genuinely have them, so a consumer can never read a
  mimetype off a folder.
- **FR-011a**: `kind` MUST reflect what was actually selected, so a consumer can branch on it
  without inspecting other fields.
- **FR-012**: Selecting a folder and selecting a menu link MUST each yield a non-empty, usable
  `url`.
- **FR-013**: Cancelling by close control, `Esc` or mask click MUST resolve the handle's `result`
  with `null` exactly once. The Promise MUST never reject on cancellation and MUST never resolve
  twice.
- **FR-014**: `handle.close()` MUST close the dialog programmatically and resolve `result` with
  `null`.
- **FR-015**: `options.title` MUST be shown as the dialog's visible title.
- **FR-016**: The dialog MUST continue to open inside the Angular zone so callers invoked from
  outside Angular (VTL script) still trigger change detection.
- **FR-017**: Every `DotBrowserOptions` field a caller passes MUST reach the browse request as
  described in *Capability Mapping*. No narrowing option may be silently ignored: a caller MUST
  never be offered a content kind it did not list in `kinds`.
- **FR-036**: Where a caller's combination is not satisfiable upstream — specifically `kinds`
  containing `'link'` together with `mimeTypes`, which the endpoint resolves by dropping links —
  the behavior MUST be surfaced to the developer rather than silently absorbed. This work MUST NOT
  work around the upstream rule.
- **FR-037**: `DojoFormBridge.openBrowserModal()` MUST return a well-formed handle whose `result`
  resolves `null`, accompanied by a developer-facing warning that the legacy editor does not
  support it — rather than today's silent no-op that is indistinguishable from a cancelled dialog.
- **FR-018**: Both shipped templates MUST work end to end **in the new Angular Edit Content**: the
  file-browser field (browse → path lands in the input and the field value) and the redirect field
  (browse → link/page URL lands in the redirect input).

### Group C — Preserve the legacy host *(scope reduced by the governing rule)*

- **FR-019**: The legacy Browser Selector subtree and its public export MUST be **retained** — it
  remains the picker for the Story Block and File/Image entry points in the legacy Dojo host.
- **FR-020**: The legacy asset-search dialog MUST be **retained** — it remains the WYSIWYG picker
  in the legacy Dojo host.
- **FR-021**: The `ASSET_PICKER_LAUNCHER` host-discriminating token, its Angular-host providers and
  its three legacy-host regression specs MUST be preserved and MUST keep passing. No replacement or
  competing host discriminator MUST be introduced.
- **FR-022**: `DojoFormBridge.openBrowserModal()` MUST NOT open any picker. Giving the legacy
  editor a working browser modal is **out of scope**; if it is wanted, it is a separate issue. Its
  only change is the diagnosability improvement in FR-037.
- **FR-023**: Issue #37207's scope MUST be corrected in writing before this work merges — its
  "delete the legacy Browser Selector" goal and its "reorient or close #37132" goal are both
  superseded by the governing rule. #37132 MUST be recorded as confirmed and in force, not closed
  as a conflict.
- **FR-024**: The migration MUST NOT leave dead translation keys, stylesheets or model types behind
  from the code paths it does remove.

### Group D — Tests *(constitution Principle V: tests first, approved, and Red before implementation)*

- **FR-025**: The `openBrowserModal` test suite MUST be updated to the new picker and MUST still
  cover option pass-through, result mapping, all cancellation paths, `close()`, and the zone
  wrapping.
- **FR-026**: New coverage MUST exist for folder, menu-link and page selection each returning a
  correct `url`.
- **FR-027**: New coverage MUST exist proving the asset-only entry points gain no folders, links or
  pages.
- **FR-028**: The three legacy-host regression specs (Story Block, File/Image, WYSIWYG) MUST still
  pass unchanged, proving the Dojo host kept its legacy pickers.
- **FR-029**: The picker's legacy-host construction spec MUST be kept, since it documents that the
  picker constructs without router or app-shell providers.
- **FR-030**: The four end-to-end asset-picker specs from #36944 (WYSIWYG, Block Editor, File,
  Image) MUST still pass.
- **FR-031**: Lint and unit tests MUST pass for the affected frontend libraries.

### Group E — Close the one endpoint gap: `extensions`

- **FR-032**: `/api/v1/drive/search` MUST accept an `extensions` parameter and pass it through to
  the browse query. The underlying query object already supports it (`BrowserQuery.extensions`, set
  via `showExtensions(...)`); the REST request form does not expose it and the helper never sets
  it. This is the **only** `ContentByFolderParams` capability with no path through the endpoint.
- **FR-033**: The addition MUST be additive and default to "no extension filtering", so every
  existing caller of the endpoint is unaffected.
- **FR-034**: The endpoint's generated API description MUST be regenerated and committed alongside
  the change, with the description authored in the Java annotations rather than edited into the
  generated file (constitution IV).
- **FR-035**: Backend coverage MUST prove that an `extensions` request narrows results to those
  extensions, and that omitting it changes nothing.

### Capability Mapping

*Verified against `/api/v1/drive/search` (`AbstractDriveRequestForm`, `ContentDriveHelper`) on
2026-08-28. Every `DotBrowserOptions` field appears in exactly one row, with the legacy flag it
replaces for traceability.*

| New option | Replaces | Status | How it is expressed on the browse endpoint |
|---|---|---|---|
| `kinds: ['file']` | `showFiles` | Honored | `FILEASSET` in `baseTypes` |
| `kinds: ['dotasset']` | `showDotAssets` | Honored | `DOTASSET` in `baseTypes` |
| `kinds: ['page']` | `showPages` | Honored | `HTMLPAGE` in `baseTypes` |
| `kinds: ['folder']` | `showFolders` | Honored | `showFolders` (endpoint default `true`; the picker pins `false`) |
| `kinds: ['link']` | `showLinks` | Honored | `showLinks` — added by #37112 |
| `status: 'live'` | `showWorking: false` | Honored | `live: true` — the helper maps `showWorking(!live)` |
| `status: 'working'` | `showWorking: true` | Honored | `live: false` |
| `status: 'archived'` | `showArchived` | Honored | `archived: true` |
| `sort` | `sortByDesc` | Honored | `sortBy` as `field:direction`, e.g. `modDate:desc` |
| `mimeTypes` | `mimeTypes` | Honored | `mimeTypes` |
| `path` | `hostFolderId` | Honored, **improved** | `assetPath` directly. The redesign drops the id→path resolution the old id-based field would have needed — which also avoids the `byPath` endpoint ADR-0020 deprecates. |
| `extensions` | `extensions` | **Gap — closed by Group E** | No endpoint parameter today. `BrowserQuery` supports it; the request form and helper do not. |

**Hard constraint discovered:** the helper computes
`showLinks = requestForm.showLinks() && !isSet(requestForm.mimeTypes())` — **menu links are
silently dropped whenever `mimeTypes` is set**, because a link carries no MIME type. A caller
therefore cannot combine link browsing with mimetype narrowing. This is by design upstream, and
this work MUST NOT paper over it; see FR-036.

**Confirmation for the redirect template:** `showLinks: true` + `baseTypes: ["HTMLPAGE"]` +
`showFolders: false` returns links plus pages — exactly what `redirect_custom_field_new.vtl` asks
for. #37112 is sufficient for it.

### Key Entities

- **Browse selection result** (`DotBrowserSelection`): what a caller receives when the editor picks
  something. A union discriminated by `kind` — every variant carries identity, title and a non-empty
  URL; contentlet-only attributes live on the variants that have them (FR-010, FR-011).
- **Browse request parameters**: what a caller asks the browser to show — starting folder, which
  content kinds (files, pages, folders, menu links, dotAssets), which versions (working, archived),
  sort direction, and extension / mimetype narrowing.
- **Browsable item**: anything the picker can list and return. Today: assets. After this work:
  assets, folders, pages and menu links — each with its own notion of "the URL".
- **Link paging cursor**: menu links page independently of assets and folders, so a page of results
  is described by three cursors, not one.
- **Host**: which editor mounted the field — the new Angular Edit Content or the legacy Dojo edit
  contentlet. It is the discriminator for which picker opens, and it is already expressed twice in
  the codebase: by the form bridge for `openBrowserModal`, and by the picker-launcher token for the
  other three entry points.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In the new Angular Edit Content, both shipped custom-field templates complete
  browse → select → value-populated without error, for every content kind they request: file, page,
  folder (file-browser field) and page, menu link (redirect field).
- **SC-002**: 100% of selections made through `openBrowserModal` return a non-empty `url` —
  including folders and menu links, which have none today.
- **SC-003**: Selecting each of the five kinds yields a result whose `kind` is correct, and a
  consumer branching on `kind` can reach every field it needs without a type assertion or an
  existence check. Both shipped templates are updated to the new shape in the same change, and no
  reference to the removed contract remains.
- **SC-004**: Across the File, Image, video and audio entry points in the Angular host, the number
  of folders, menu links and pages a user can see or select is **zero**, unchanged from today.
- **SC-005**: Every menu link and every folder in a directory is reachable exactly once by paging
  forward and back, with no duplicates and no omissions, in a directory large enough to span at
  least three pages.
- **SC-006**: Cancelling produces exactly one cancellation callback, across all four dismissal
  paths.
- **SC-007**: In the legacy Dojo editor, all four asset-selection entry points open the same picker
  they open today — zero behavioral change in that host.
- **SC-008**: The legacy Browser Selector has zero Angular-host consumers; its only remaining
  production references are the two legacy-host entry points.
- **SC-009**: Every flag in the browse parameters appears in exactly one row of the *Capability
  Mapping* table, and every one of them is honored end to end — zero flags are silently ignored.
- **SC-010**: The four pre-existing end-to-end asset-selection journeys and the three legacy-host
  regression specs all still pass, and the affected libraries' lint, unit-test and build targets
  all pass.
- **SC-011**: A written scope correction for #37207 and a confirmation of #37132 exist and are
  linked from this spec before merge.
- **SC-012**: A browse request that names file extensions returns only assets with those
  extensions, and an identical request without them returns exactly what it returns today — proving
  the endpoint addition is both effective and backward-compatible.

---

## Assumptions

- **Mostly frontend, with one small backend addition.** #37112 already shipped the browse
  capability the shipped templates need. The one exception is `extensions` (Group E), which
  requires a Java change to the drive search request form and helper plus a regenerated API
  description. Everything else is frontend: the request/response models need to carry the
  link-paging fields.
- **Legacy Impact (constitution I)**: Group E touches `com.dotcms.rest.api.v1.drive` (modern
  package) and reads a builder in `com.dotcms.browser`. No `com.dotmarketing.*` change is expected.
  The addition is backward-compatible — an absent `extensions` behaves exactly as today.
- **`AngularFormBridge` is only ever the Angular host.** `createFormBridge` selects it for
  `type: 'angular'` and `DojoFormBridge` for `type: 'dojo'`, so migrating the former needs no new
  host discriminator.
- **`kinds: ['file']` and `path` are already covered** by the picker's existing behavior and need
  no new capability work.
- **`status` maps to version-state filtering** that the browse API already supports (`live` /
  `archived`); the picker currently hardcodes non-archived and will need to stop doing so
  unconditionally.
- **`extensions` is closed at the endpoint, not worked around client-side.** Filtering a
  cursor-paged result set in the browser would silently shrink pages and break paging, so the
  narrowing belongs where the query is built (Group E).
- **A folder's "URL" is its path**, and a menu link's "URL" is its target URL. Planning confirms
  the exact field each comes from on the browse response.
- **The default starting location** stays the picker's existing behavior (explicit `path`, else the
  remembered last-used location, else the contentlet's site). Making `path` optional in the redesign
  matches what both shipped templates already do — neither passes a starting folder.
- **No new user-facing translation keys** are needed beyond a title for the generic browse mode, if
  the caller supplies none.
- **Rollback safety.** `DotCustomFieldApi` is unshipped, so redesigning it carries no
  customer-facing rollback risk (see *API Redesign*). The rollback-sensitive surface is
  `/api/v1/drive/search`, which other consumers depend on — its only change is additive and
  defaults to today's behavior (FR-033).
- **`/speckit-plan` will consult `dotCMS/platform-adrs`** for binding decisions on frontend host
  capability seams — directly relevant to the governing rule, which this work now reinforces rather
  than removes.
