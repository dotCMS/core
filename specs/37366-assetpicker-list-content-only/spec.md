# Issue Resolution Specification: Asset Picker — list panel shows content only, folders navigated through the sidebar tree

**Feature Branch**: `nicobytes/37366-asset-picker-list-panel-should-show-content-only-with-folders-navigated-through-the-sidebar-tree`

**Created**: 2026-09-03

**Status**: Draft — open question settled 2026-09-03 (shape **(a)**, see [Decision](#decision-settled-2026-09-03))

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37366](https://github.com/dotCMS/core/issues/37366) — split from [#37174](https://github.com/dotCMS/core/issues/37174) (finding 9), source [#37207](https://github.com/dotCMS/core/issues/37207) (PR [#37273](https://github.com/dotCMS/core/pull/37273)), epic [#36702](https://github.com/dotCMS/core/issues/36702)

**Input**: User description: "37366-asset-picker-list-panel-should-show-content-only-with-folders-navigated-through-the-sidebar-tree — the field in question is `dotCMS/src/main/webapp/WEB-INF/velocity/static/content/file_browser_field_render_new.vtl`"

---

## ⚠ Premise Corrections

Two statements in the issue body do not survive verification against `main`. Both matter, because
one of them is the whole argument for the alternative shape.

### 1. `forwardTo` is a **Vanity URL** field, not an HTML-page field

The issue says *"the `forwardTo` field on an HTML page is the shipped case"*. `forwardTo` is a
field of the **Vanity URL** content type:

- `VanityUrlContentType.java:42` — `FORWARD_TO_FIELD_VAR = "forwardTo"`
- `VanityUrlContentType.java:80-83` — its custom field is seeded with
  `$velutil.mergeTemplate('/static/content/file_browser_field_render.vtl')`
- `file_browser_field_render.vtl:1-5` — that template is only a dispatcher: it parses
  `file_browser_field_render_new.vtl` when `$structures.isNewEditModeEnabled()` is true, and
  `file_browser_field_render_old.vtl` otherwise

The HTML page's own custom field is `redirecturl`, rendered by `redirect_custom_field_new.vtl`,
which asks for `kinds: ["page", "link"]` (`redirect_custom_field_new.vtl:57`) — **no folder**. It is
therefore unaffected by the `kinds` change and only needs a no-regression check.

**Consequence for the fix**: the Java side changes nothing. `VanityUrlContentType` names the
*dispatcher*, not `_new.vtl`, so editing `_new.vtl` needs no Java edit and no content-type
migration.

### 2. Shape (a) does **not** remove a capability the legacy Browser Selector had

The issue offers shape (a) with the caveat that it *"removes a capability the legacy Browser
Selector had"*. Neither legacy picker ever listed a folder as a selectable row, nor returned one:

| Legacy picker | List content | Returned value |
|---|---|---|
| `DotBrowserSelectorComponent` (Angular, still live for Dojo hosts per #37132) | `content.data: DotCMSContentlet[]` (`browser.store.ts:60-64`) — its `folders: TreeNodeItem[]` field is the **sidebar tree**, not list rows | `selectedContent: DotCMSContentlet \| null` (`browser.store.ts:65`) |
| `dotcms.dijit.FileBrowserDialog` (Dojo, used by `file_browser_field_render_old.vtl:38-39`) | files, via `onFileSelected` | a file; folders live in its `foldersTree` (`FileBrowserDialog.js:164,184-189`) |

A folder became pickable for the first time on **2026-08-31**, when #37273 merged (`81139a83717`),
and only behind the non-default new-Edit-Content flag. So shape (a) **restores** the behavior both
legacy pickers had — which is exactly what QA asked for — rather than withdrawing something
customers have.

There is also no loss of the underlying use case. `forwardTo` keeps its free-text input
(`file_browser_field_render_new.vtl:38-44`), whose `change` handler writes straight to the field
value. A Vanity URL can still forward to a folder path; the path is **typed**, not **picked**. What
goes away is picking one from the browser.

---

## Decision (settled 2026-09-03)

The issue makes one question blocking: *does "pick a folder path" survive as a capability at all?*
It is settled as **shape (a)**:

> **`'folder'` is dropped from the browse contract.** It leaves `DotBrowserItemKind`, leaves the
> `DotBrowserSelection` union, and leaves `file_browser_field_render_new.vtl`'s `kinds`. A folder
> becomes unpickable, and the sidebar tree is the only place folders appear.

And its follow-on: **the picker's folder paging stream is removed**, not kept dormant —
`folderCursor` / `hasMoreFolders` and their contribution to `$totalRecords` go, so the paging model
carries no dead stream. The `/api/v1/drive/search` request keeps sending `showFolders: false`,
because the endpoint takes the flag.

**Shape (b) is rejected, not deferred.** (b) kept the capability and moved it onto an explicit
"Select this folder" affordance in the tree. It is rejected because Premise Correction 2 removes its
justification — there is no legacy capability to preserve — while it costs new UI surface, new UX
design, and a contract that keeps advertising a kind whose selection path is unlike every other
kind's.

**Who settled it**: the issue owner, during refinement on 2026-09-03. The issue's AC asks for
UX/Product sign-off before any code; **this spec PR is that record** — approval of PR 1 is the
sign-off. If UX/Product prefers (b), this spec must be re-approved before `/speckit-plan` runs. See
[Assumptions](#assumptions).

---

## Problem Statement *(mandatory)*

The Asset Picker's right-hand list panel lists the current folder's **subfolders as selectable
rows**, each with a single-select radio, mixed in with files and pages — and a folder can be
returned as the picked value. QA expects the list to carry content only, with folders reached
through the sidebar tree, which is how every dotCMS file browser before this one behaved.

This is **not a defect in #37207's implementation**. That issue's own acceptance criteria asked for
this behavior — *"The picker can list and return folders when the caller asks for them"* — and its
post-merge plan covers folder selection as TC-003, which **passed**. TC-001 failed on QA disagreeing
with the design. What changes here is the decision, not a regression.

**Severity / Impact**: Medium — some functionality impacted.

- **Who**: back-end users editing a content item whose custom field renders
  `file_browser_field_render_new.vtl` — in the shipped product, the Vanity URL `forwardTo` field —
  with the new Edit Content experience enabled (not the default).
- **How badly**: no data loss and no error. A confusing list, and a field value that can be set to a
  folder path by picking a row that QA considers navigational.
- **How often**: every time that picker is opened. Scope is narrow: `DotAssetPickerConfig.browse`
  keeps folder listing opt-in (`store/models.ts:26-32`), and File, Image, video and audio fields
  never set it — so only `openBrowserModal` callers are affected, and today exactly one shipped
  template asks for folders.

## Reproduction *(mandatory)*

**Environment**: latest from `main` (folder listing arrived with `81139a83717`, 2026-08-31); new Edit
Content experience enabled so `$structures.isNewEditModeEnabled()` is true; any site; any modern
browser.

**Steps to Reproduce**:

1. Enable the new Edit Content experience.
2. Edit (or create) a **Vanity URL** content item — its `forwardTo` custom field renders
   `file_browser_field_render_new.vtl` through the `file_browser_field_render.vtl` dispatcher.
3. Click **click-here-to-browse** to open the Asset Picker.
4. Look at the right-hand list panel.
5. Select a folder row and confirm.

**Expected Behavior**: the list panel carries content only — files and pages, no folder rows.
Folders are reached by navigating the sidebar tree, and the list reflects the tree's selected folder.

**Actual Behavior**: step 4 — the current path's subfolders are listed as rows, each with a
selectable radio, interleaved with files and pages. Step 5 — the folder's path is written into the
field. No error message; the behavior is the shipped template's own request honored end to end.

**Reproducibility**: always, for any `openBrowserModal` caller whose `kinds` includes `'folder'`.
Never for any other entry point.

> 🎥 Video on the [TC-001 result](https://github.com/dotCMS/core/issues/37207#issuecomment-5486135180).

## Scope of Investigation *(mandatory)*

- **Affected area**: new Edit Content → the custom-field VTL bridge
  (`DotCustomFieldApi.openBrowserModal`) → the Asset Picker's browse mode and its list panel.

- **Suspected surface**: **modern only**, and almost entirely frontend.
  - `core-web/libs/edit-content-bridge` — the published browse contract and its translation into
    the picker's vocabulary.
  - `core-web/libs/ui/src/lib/components/dot-asset-picker` — the browse options type and the
    request/paging feature.
  - One backend-owned *artifact*, but no backend *code*: the Velocity template
    `dotCMS/src/main/webapp/WEB-INF/velocity/static/content/file_browser_field_render_new.vtl`.
  - No `com.dotmarketing.*` and no `com.dotcms.*` Java change. No REST resource, no `@Schema`, no
    `openapi.yaml` regeneration, no DB schema, no ES mapping.

- **Related known decisions**:
  - **#37132's governing rule stays in force** — *Dojo/old-editor host → the picker already there;
    Angular host → the new AssetPicker*. The legacy `dot-browser-selector` subtree is untouched by
    this fix; it is only cited above as evidence of prior behavior.
  - **#37112's `showLinks` / `linkCursor` paging stays** — only the folder stream is withdrawn.
  - `/speckit-plan` formally consults `dotCMS/platform-adrs`; no ADR is known to bear on this, and
    this spec proposes none.

## Root-Cause Hypothesis

**There is no code defect.** The behavior is the shipped template's own request travelling faithfully
through every layer:

| # | Layer | Location | What it does |
|---|---|---|---|
| 1 | Velocity template | `file_browser_field_render_new.vtl:23` | asks for `kinds: ["file", "page", "folder"]` |
| 2 | Browse-config builder | `angular-form-bridge.ts:665` | `kinds.includes('folder')` → `{ showFolders: true }` |
| 3 | Picker request | `with-asset-browse.feature.ts:129-130, 169` | forwards `showFolders` on every `/api/v1/drive/search` call |
| 4 | Endpoint | `/api/v1/drive/search` | returns folders in `response.list` |
| 5 | Picker store | `with-asset-browse.feature.ts` (`patchState({ items: response.list })`) | folders land in `items` next to contentlets — `items` is typed `DotContentDriveBrowseItem[]` (`store/models.ts:244`) and `selectedAsset` was widened from `DotCMSContentlet` for exactly this (`store/models.ts:297-305`) |
| 6 | List component | `dot-folder-list-view.component.html:106-109` | renders a `p-tableRadioButton` for **every** row it is handed |

Two consequences follow, and they shape the fix:

- **`DotFolderListViewComponent` has no notion of a navigational, non-selectable row.** `items` is
  one flat list and the radio is unconditional. So "list content only" cannot be achieved by styling
  or disabling rows; it has to be enforced **upstream of the list**, in what the picker requests.
- **Turning it off touches the published contract, not just the picker.** `DotBrowserItemKind`
  (`asset-browser.interface.ts:19`) advertises `'folder'`, `DotBrowserFolderSelection`
  (`:132-135`) is a member of the returned union, and the folder paging stream
  (`with-asset-browse.feature.ts:85, 129-130, 151, 211-212`) exists to serve it.

The fix is therefore to **withdraw the capability from the contract**, per the
[Decision](#decision-settled-2026-09-03) — not to patch a layer.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

1. **The published browse contract** — `libs/edit-content-bridge/src/lib/interfaces/asset-browser.interface.ts`
   - drop `'folder'` from `DotBrowserItemKind` (`:19`)
   - remove `DotBrowserFolderSelection` (`:132-135`) and its arm of the `DotBrowserSelection` union
   - update the `kinds` TSDoc so a template author reads what the picker will actually do
2. **The bridge** — `libs/edit-content-bridge/src/lib/bridges/angular-form-bridge.ts`
   - remove the `showFolders` opt-in from `browseOptionsFor` (`:665`)
   - remove `kindOf`'s `item['type'] === 'folder'` branch (`:678-679`)
   - remove the `kind === 'folder'` arm of the selection mapper (`:724`)
   - decide how a stale template that still passes `'folder'` is handled at runtime (see
     [Regression Risk](#regression-risk-mandatory) — a `console.warn`, mirroring the existing
     `link` + `mimeTypes` warning at `:653-660`, is the candidate)
3. **The picker's browse options** — `libs/ui/src/lib/components/dot-asset-picker/store/models.ts`
   - remove `showFolders` from `DotAssetPickerBrowseOptions` (`:26-32`)
4. **The picker's paging model** — `.../store/features/with-asset-browse.feature.ts`
   - drop `hasMoreFolders` from `$totalRecords` (`:85`) — the union becomes content + links
   - remove the `showFolders` computation (`:129-130`) and send a constant `showFolders: false`
   - remove `folderCursor` from the request (`:151`) and `folderCursor` / `hasMoreFolders` from the
     page bookmark (`:211-212`) and from the bookmark's type
   - update the comments that describe folders as a listable, pageable stream (`:52, 74-77,
     148-150, 167-169`)
5. **The shipped template** — `file_browser_field_render_new.vtl:23` → `kinds: ["file", "page"]`
6. **Tests**, per [Acceptance & Verification](#acceptance--verification-mandatory)

**Explicitly out of scope / non-goals**:

- **`DotFolderListViewComponent` is not touched.** No non-selectable row mode, no conditional radio,
  no folder-row filtering inside the list. It stays a dumb renderer of whatever it is handed — this
  is an explicit AC of the issue, not just a preference.
- **The sidebar tree is not touched.** Folder navigation already works and must keep working
  unchanged.
- **Content Drive is not touched.** Its own store legitimately lists folders
  (`dot-content-drive.store.ts:149-172`); it is a different store with a different product intent.
  Its `showFolders` logic and its specs stay exactly as they are.
- **The legacy `dot-browser-selector` subtree is not touched**, and #37132's per-host picker routing
  is not revisited.
- **`showLinks` / `linkCursor` (#37112) is not touched.** The link stream stays, and stays paged.
- **Shape (b) is not built** — no "Select this folder" affordance, in this fix or as follow-up work
  it implies. It is rejected, and the rejection is recorded above so it is not re-litigated.
- **`forwardTo`'s free-text input is not touched.** Typing a folder path stays supported; only
  picking one from the browser goes away.
- **`redirect_custom_field_new.vtl` is not edited.** It asks for `["page", "link"]` already; it is
  only re-verified.
- No Java, no REST contract, no `openapi.yaml`, no DB schema, no ES mapping, no content-type
  migration.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - **Paging / the paginator — the real risk.** `$totalRecords` (`with-asset-browse.feature.ts:79-89`)
    claims one page beyond whenever *any* stream reports more, precisely so a stream that outlives
    the others stays reachable. Narrowing the union from three streams to two must not break the
    surviving link stream: #37207's **TC-007** — *"the paginator stays reachable while any stream
    still has more"* — must be re-verified with the folder stream in its final, removed state.
  - **`DotAssetPickerBrowseOptions.showFolders` removal is a compile-time break** for any caller
    that sets it. Verified callers today: the bridge (`angular-form-bridge.ts:665`) and spec
    fixtures (`asset-picker-config.spec.ts:229`, `dot-asset-picker.store.spec.ts:103` and its
    `showFolders` suites). No production caller outside the bridge.
  - **`DotBrowserItemKind` is the VTL-facing contract, and TypeScript does not police VTL.** A
    template that still passes `kinds: ["folder", …]` hands the bridge a string the type no longer
    admits. The runtime outcome must be benign and *stated*: folders are simply not listed. Whether
    to also warn is a plan decision; the issue's last AC ("a customer template cannot ask for a kind
    the picker silently refuses") argues for warning.
  - The picker's other entry points (File, Image, video, audio) never set `browse`, so their request
    is unchanged apart from `showFolders` going from a computed `false` to a constant `false`.
- **Backward compatibility**:
  - **The browse API is unshipped.** Per `asset-browser.interface.ts:1-11`, the templates that call
    it (`*_new.vtl`) only render when new Edit Content is enabled, which is not the default. Folder
    picking has existed only since 2026-08-31 (`81139a83717`). No shipped customer contract breaks.
  - **Stored field values are unaffected.** A `forwardTo` value that is a folder path — including one
    picked in the days since #37273 — keeps its value and keeps working: Vanity URL forwarding is
    server-side and knows nothing about the picker.
  - **Not rollback-unsafe.** Frontend plus one Velocity template; none of the DB-schema, ES-mapping
    or API-contract categories apply.
- **Data considerations**: none. No migration, no repair of existing data.

## Acceptance & Verification *(mandatory)*

- **AC-001** *(the reproduction)*: with new Edit Content enabled, opening the Asset Picker from a
  Vanity URL's `forwardTo` field lists **no folder rows** — files and pages only. Steps 4 and 5 of
  the reproduction no longer produce the actual behavior.
- **AC-002** *(every caller, including a stale one)*: a caller that passes `kinds: ['folder', …]`
  gets no folder rows and no crash; the resulting `/api/v1/drive/search` request carries
  `showFolders: false`. The behavior for such a caller is documented, not silent.
- **AC-003** *(navigation preserved)*: folders stay fully navigable through the sidebar tree, and
  the list keeps reflecting the tree's selected folder's contents.
- **AC-004** *(enforced upstream)*: the change is enforced where the request is built — the browse
  contract and the picker's browse config. `dot-folder-list-view.component.ts/.html` carry **no
  diff**; no row is hidden or disabled to achieve this.
- **AC-005** *(the decision, recorded before code)*: shape **(a)** is implemented — `'folder'` is
  absent from `DotBrowserItemKind`, from the `DotBrowserSelection` union, and from
  `file_browser_field_render_new.vtl:23`. Shape (b) is recorded as rejected with its rationale
  (this document), so the alternative is not re-opened during implementation.
- **AC-006** *(no dead paging stream)*: the picker's folder paging stream is **removed** —
  `folderCursor` and `hasMoreFolders` appear nowhere in the picker store, and `$totalRecords` unions
  content + links only. The request still sends `showFolders: false`, because the endpoint requires
  the flag.
- **AC-007** *(both shipped templates verified end to end)*:
  - `file_browser_field_render_new.vtl` — `kinds` changed; browse, navigate, pick a file, pick a
    page, save, reopen.
  - `redirect_custom_field_new.vtl` — unchanged `["page", "link"]`; browse, pick a page, pick a
    menu link, save, reopen. No folder rows there either (there never were).
- **AC-008** *(contract docs)*: the `openBrowserModal` TSDoc states the resulting behavior — folders
  are navigation-only, cannot be requested, and cannot be returned — so a template author cannot ask
  for a kind the picker refuses.

### Verification method

Frontend-only change, so the layer-appropriate types are **Jest/Spectator unit specs** plus manual
end-to-end checks of the two templates. Per Constitution Principle V this is stated, not silently
assumed: **integration, Postman and Karate do not apply** — there is no Java, no REST endpoint and
no server behavior change; the one backend artifact is a Velocity template whose only assertion is
the `kinds` literal it passes to the frontend.

**Specs to add / update** (regression coverage the issue asks for):

| Spec | Change |
|---|---|
| `libs/ui/.../dot-asset-picker/store/dot-asset-picker.store.spec.ts` | **new**: no folder row reaches `items` for a `kinds: ['folder', …]` caller. **Update**: the `showFolders` suites (`:103`, `:248-275`, `:319`, `:441`, `:476`, `:514-521`) to the final shape |
| `libs/ui/.../dot-asset-picker/asset-picker-config.spec.ts` | remove the `showFolders: true` fixture (`:229`) |
| `libs/edit-content-bridge/.../angular-form-bridge.spec.ts` | update the `openBrowserModal` suite (`:945`, `:961`); **add**: `kinds: ['folder', 'file']` produces no `showFolders`, and the documented handling for the now-unknown kind |
| paging coverage (picker store spec) | **guards #37207 TC-007**: with the folder stream removed, Next stays reachable while only `hasMoreLinks` is true, and the last page reports an exact total |

**Commands**:

```bash
cd core-web
pnpm nx test ui --testPathPatterns=dot-asset-picker
pnpm nx test edit-content-bridge --testPathPatterns=angular-form-bridge
pnpm nx lint ui && pnpm nx lint edit-content-bridge
```

**Manual**: new Edit Content enabled → Vanity URL `forwardTo` (AC-001, AC-003, AC-007) and HTML page
`redirecturl` (AC-007).

**TDD gates** (Constitution V, non-negotiable): the specs above are written first, approved by the
developer, and confirmed **failing (Red)** before any implementation lands. `/speckit-tasks` orders
each story tests → approval gate → Red gate → implementation.

## Assumptions

- **The blocking decision is settled with the issue owner, not yet with UX/Product.** Shape (a) was
  chosen on 2026-09-03 during refinement, on the strength of
  [Premise Correction 2](#2-shape-a-does-not-remove-a-capability-the-legacy-browser-selector-had).
  This spec PR **is** the record the issue's AC asks for; its approval is the sign-off. If UX/Product
  prefers (b), the spec is re-approved before `/speckit-plan` runs — no code is written against an
  unapproved shape.
- **No customer VTL outside this repo passes `kinds: ['folder']`.** Assumed because the browse API is
  unshipped behind a non-default flag and only three days old. If a customer template does, it
  degrades to "no folder rows" — which is the intended product behavior anyway, so the assumption
  being wrong changes nothing but the warning's audience.
- **`/api/v1/drive/search` keeps requiring `showFolders`**, so the picker keeps sending a constant
  `false` rather than omitting the key. To be confirmed against the endpoint during `/speckit-plan`;
  if the flag is optional, omitting it is preferred.
- **Reproduction environment is "latest from `main`"**, as the issue states, with
  `$structures.isNewEditModeEnabled()` true. Behavior on the default (flag off) path is not in
  question — `file_browser_field_render_old.vtl` never listed folders.
- **QA's TC-003 for #37207 ("the picker can return a folder") is superseded by this decision** and is
  expected to be retired or inverted in that issue's post-merge plan, not treated as a conflicting
  requirement.
