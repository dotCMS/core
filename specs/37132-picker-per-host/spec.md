# Issue Resolution Specification: New AssetPicker must not be used in the legacy Dojo editor — pick the picker per host (WYSIWYG, Block Editor, File/Image)

**Feature Branch**: `nicobytes/37132-new-assetpicker-must-not-be-used-in-the-legacy-dojo-editor-pick-the-picker-per-host-wysiwyg-block-editor-fileimage`

**Created**: 2026-08-20

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37132](https://github.com/dotCMS/core/issues/37132) — regression from [#36944](https://github.com/dotCMS/core/pull/36944) (epic [#36702](https://github.com/dotCMS/core/issues/36702))

**Input**: User description: "Resolver el issue 37132 — el AssetPicker nuevo no debe usarse en el editor Dojo legacy; la elección de picker debe ser por host."

## Problem Statement *(mandatory)*

PR #36944 wired the new `DotAssetPickerComponent` into every asset-selection entry point
**unconditionally**. Three of those entry points are Angular components that the legacy
Dojo/JSP *edit contentlet* page also mounts (two of them as custom elements), so the new
AssetPicker now opens inside the old editor — a full-height, folder-tree-plus-chips browser
that was never designed for that page and matches nothing else on it.

The picker must be chosen by **which host mounted the component**, not globally:

| Host | Expected picker |
|---|---|
| New Edit Content (Angular: full-screen shell, slide-in side panel, or overlay dialog) | `DotAssetPickerComponent` (new) |
| Legacy edit contentlet (Dojo/JSP, fields mounted as custom elements) | The pre-#36944 picker |

**Severity / Impact**: Medium. Front-end only, reproduces in all browsers, no data loss.
Affects **every customer still on the legacy edit contentlet** — i.e. anyone who has not
enabled the new Edit Content, which is the current default for most installs. Regression is
on `main` only; it has not shipped in a released version.

### Legacy hosts involved

| Custom element | Registered in | Mounted by |
|---|---|---|
| `<dotcms-block-editor>` / `<dotcms-old-block-editor>` | `core-web/apps/dotcms-block-editor/src/main.ts` | `dotCMS/src/main/webapp/html/portlet/ext/contentlet/field/edit_field.jsp:248` (tag chosen by `FEATURE_FLAG_NEW_BLOCK_EDITOR`); scripts at `edit_contentlet.jsp:88-91` |
| `<dotcms-binary-field>` | `core-web/apps/dotcms-binary-field-builder/src/app/app.module.ts` | scripts at `edit_contentlet.jsp:114-116` |

### Entry points that open the new picker unconditionally

| Field | File | Entry point |
|---|---|---|
| Block Editor (image / video / audio) | `core-web/libs/new-block-editor/src/lib/editor/services/editor-modal.service.ts` | `openAssetPicker()` → `mountAssetPicker()` |
| WYSIWYG (TinyMCE `dotAddImage`) | `core-web/libs/edit-content/src/lib/fields/dot-edit-content-wysiwyg-field/dot-wysiwyg-plugin/dot-wysiwyg-plugin.service.ts` | `dotImageDialog()` → `openImagePicker()` |
| File / Image | `core-web/libs/edit-content/src/lib/fields/dot-edit-content-file-field/components/dot-file-field/dot-file-field.component.ts` | `showSelectExistingFileDialog()` |

### Picker to restore per field (pre-#36944 behavior)

| Field | Component for the legacy host | Notes |
|---|---|---|
| WYSIWYG | `DotAssetSearchDialogComponent` (`@dotcms/ui`) | header `Insert Image`, `data: { assetType: 'image' }` |
| Block Editor | `DotBrowserSelectorComponent` (`@dotcms/ui`) | via `buildBrowserSelectorConfig({ header, mimeTypes })` — **deleted** by #36944 from `core-web/libs/new-block-editor/src/lib/editor/config.utils.ts`, must be restored |
| File / Image | `DotBrowserSelectorComponent` (`@dotcms/ui`) | headers `dot.file.field.dialog.select.existing.image.header` / `...file.header`; inline dialog config previously built in `showSelectExistingFileDialog()` |

Both legacy components are still exported from `@dotcms/ui`
(`core-web/libs/ui/src/index.ts:70` and `.../dot-asset-search-dialog.component`), so
restoring them is a re-wire, not a resurrection.

## Reproduction *(mandatory)*

**Environment**: Build from `main` containing #36944 (merged 2026-08-19). Any browser. New
Edit Content **disabled**, so the legacy edit contentlet is what opens.

**Steps to Reproduce**:

1. Create a content type with a **WYSIWYG** field, a **Block Editor (Story Block)** field,
   and an **Image** (or File) field.
2. Edit a contentlet of that type in the legacy editor.
3. **Block Editor field** → slash menu (or toolbar) → *Image* / *Video* / *Audio*.
4. **Image / File field** → *Select Existing File*.
5. **WYSIWYG field** → *Add image* toolbar button.
6. Open the **same content type in the new Angular Edit Content** and repeat 3–5.

**Expected Behavior**:

- Steps 3–5 (legacy host): `DotBrowserSelectorComponent`, `DotBrowserSelectorComponent`, and
  `DotAssetSearchDialogComponent` respectively — exactly as before #36944.
- Step 6 (Angular host): `DotAssetPickerComponent` in all three — this path must not regress.

**Actual Behavior**: `DotAssetPickerComponent` opens in all cases, legacy host included.

**Reproducibility**: Always, for any contentlet with those field types.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing — asset selection in the Edit Contentlet UI (WYSIWYG,
  Story Block, File/Image fields). Front-end only.
- **Suspected surface**: Angular front-end under `core-web/` (`libs/ui`,
  `libs/edit-content`, `libs/new-block-editor`, plus the two custom-element apps under
  `core-web/apps/`). **No Java change is expected.** The legacy Dojo/JSP pages
  (`dotCMS/src/main/webapp/html/portlet/ext/contentlet/`) are only *hosts* here — they
  mount the custom elements and are not edited.
- **Related known decisions**: The codebase already solves this exact host-discrimination
  problem for the image editor via the `IMAGE_EDITOR_LAUNCHER` injection token
  (`core-web/libs/edit-content/src/lib/fields/shared/image-editor-launcher/`), injected with
  `{ optional: true }`. This fix follows that precedent. `/speckit-plan` will consult
  `dotCMS/platform-adrs` for any binding ADR on front-end host capability seams.

## Root-Cause Hypothesis

#36944 replaced the picker at each call site instead of behind a host-capability seam. The
three entry points construct `DotAssetPickerComponent` directly, so the choice is baked into
library code that both the Angular Edit Content and the legacy custom elements share. Nothing
at those call sites can tell the two hosts apart, and nothing needs to today — which is
exactly why the switch has to move up to the host, expressed as a DI provider.

Two findings from code inspection refine the issue's own proposal:

1. **The token cannot live in `libs/edit-content`.** `libs/edit-content` already imports
   `@dotcms/new-block-editor`
   (`dot-edit-content-block-editor.component.ts:8`), so `new-block-editor` importing back
   from `edit-content` would be a project cycle. The token (and the launcher that opens
   `DotAssetPickerComponent`) must live in `@dotcms/ui` — the lib both feature libs already
   depend on, and where `DotAssetPickerComponent` itself lives.

2. **There are three Angular hosts, not two.** Besides `EditContentShellComponent` and
   `DotEditContentSidePanelComponent`, `DotEditContentDialogComponent`
   (`libs/edit-content/src/lib/components/dot-create-content-dialog/`) renders the full
   Edit Content layout and is opened live by UVE
   (`libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts:1477`)
   and by the Relationship field
   (`.../dot-relationship-field/components/dot-relationship-field/dot-relationship-field.component.ts:479`).
   It does **not** provide `IMAGE_EDITOR_LAUNCHER`. Providing the new token in only the two
   hosts the issue names would therefore *create* a second regression: the new picker would
   fall back to legacy inside UVE's content dialog and inside nested relationship-field
   editing. **Decision (agreed with the developer): all three Angular hosts provide the
   token.** Only the Dojo custom-element bootstraps leave it absent.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- A host-capability seam in `@dotcms/ui` — an injection token plus a launcher implementation
  that opens `DotAssetPickerComponent` — mirroring `IMAGE_EDITOR_LAUNCHER`, consumed with
  `{ optional: true }`.
- The token provided by the three Angular Edit Content hosts:
  `edit-content.shell.component.ts`, `dot-edit-content-side-panel.component.ts`,
  `dot-create-content-dialog/dot-create-content-dialog.component.ts`.
- Branching the three entry points (Block Editor modal service, WYSIWYG plugin service,
  File/Image field) on token presence: present → new picker; absent → the pre-#36944 picker.
- Restoring `buildBrowserSelectorConfig()` in
  `core-web/libs/new-block-editor/src/lib/editor/config.utils.ts`, including its
  `OVERLAY_ABOVE_FULLSCREEN_Z_INDEX` base z-index.
- Restoring the `DotBrowserSelectorComponent` dialog config in the File/Image field and the
  `DotAssetSearchDialogComponent` dialog in the WYSIWYG plugin service.
- Unit tests covering **both** branches of all three entry points, using the
  token-absent / token-present factory pattern of
  `dot-file-field.component.legacy-availability.spec.ts`.
- **Auditing whether `IMAGE_EDITOR_LAUNCHER` has the same gap** in
  `DotEditContentDialogComponent`. If confirmed, fixing it here is in scope only if it is a
  one-line provider addition consistent with this change; otherwise it is filed as a separate
  issue and recorded in the plan.

**Explicitly out of scope / non-goals**:

- Any change to `DotAssetPickerComponent`'s own behavior, layout, or store.
- Any change to the Dojo/JSP pages, the JSP-side TinyMCE build, or
  `html/js/dotcms/dijit/FileBrowserDialog.js`.
- Any Java / backend change; no new `Config` key or feature flag — the discriminator is the
  host, not a customer setting. (A server-side flag is explicitly rejected: one install can
  render both hosts at the same time.)
- Improving or restyling the legacy pickers. They are restored as they were, warts included.
- Migrating the legacy editor's WYSIWYG off the JSP-side TinyMCE.
- Deleting `DotBrowserSelectorComponent` or `DotAssetSearchDialogComponent` — both stay.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - All three entry points are shared between the Angular and legacy hosts; a token wired in
    the wrong place silently flips the picker for a whole host class. The Angular path is the
    one at risk of a *silent* regression (wrong picker, no error), which is why the
    three-host decision above matters.
  - `libs/ui` gains a token + launcher; `libs/ui` is imported nearly everywhere, so the new
    file must not drag in `Router` or app-shell providers. The legacy binary-field host
    bootstraps with only `provideHttpClient`, `provideAnimations`, `DotMessageService`,
    `DotUploadService`, `DotWorkflowActionsFireService` and the theme — anything reachable
    from the field has to survive that (this is the exact class of failure
    `dot-asset-picker.component.legacy-host.spec.ts` was written to catch).
  - The legacy path must construct with **no `Router`** and **no `DotSite`** — the legacy
    pickers never needed a site, so the site lookup must stay on the new-picker branch only.
  - `DotBrowserSelectorComponent` reuse in the Block Editor must keep
    `OVERLAY_ABOVE_FULLSCREEN_Z_INDEX`, or the dialog paints under the full-screen editor
    shell's `z-[9998]` backdrop and becomes unclickable.
  - The pending/busy double-click guards added by #36944 must keep protecting the new
    branch without leaking a stuck flag into the legacy branch.
- **Backward compatibility**: The legacy binary-field contract consumed by the JSP editor
  (`valueUpdated` → `{ value, fileName }`, preview, title/fileName auto-fill) must behave
  identically on both branches. No API, DB, or ES change; nothing rollback-unsafe.
- **Data considerations**: None. No stored data changes; no migration or repair needed.

## Acceptance & Verification *(mandatory)*

**Host detection**

- **AC-001**: A host-capability injection token exists in `@dotcms/ui`, mirroring
  `IMAGE_EDITOR_LAUNCHER`, that tells an asset-selection entry point whether it runs in the
  new Angular Edit Content or in a legacy host.
- **AC-002**: It is provided by exactly the three Angular Edit Content hosts
  (`edit-content.shell.component.ts`, `dot-edit-content-side-panel.component.ts`,
  `dot-create-content-dialog.component.ts`) and by no other host; both Dojo custom-element
  bootstraps leave it absent.
- **AC-003**: It is injected with `{ optional: true }` — a missing provider resolves to the
  legacy path and never throws.
- **AC-004**: Detection is host-based; no server-side `Config` key or feature flag is added
  or read for this decision.

**Block Editor field**

- **AC-005**: In the new Angular Edit Content, *Image* / *Video* / *Audio* open
  `DotAssetPickerComponent` (unchanged from today).
- **AC-006**: In the legacy Dojo editor (`<dotcms-block-editor>` and
  `<dotcms-old-block-editor>`), they open `DotBrowserSelectorComponent` with the pre-#36944
  mime-type scoping (`['image']`, `['video']`, `['audio']`).
- **AC-007**: `buildBrowserSelectorConfig()` is restored in
  `core-web/libs/new-block-editor/src/lib/editor/config.utils.ts`, including its
  `OVERLAY_ABOVE_FULLSCREEN_Z_INDEX` base z-index, so the picker clears the full-screen
  editor shell's `z-[9998]` backdrop.
- **AC-008**: Inserting an asset from the legacy picker still produces the correct
  `dotImage` / `dotVideo` / `dotAudio` node at the current selection.

**WYSIWYG field**

- **AC-009**: In the new Angular Edit Content, *Add image* opens `DotAssetPickerComponent`
  (unchanged).
- **AC-010**: In a legacy host, it opens `DotAssetSearchDialogComponent` with
  `assetType: 'image'`.
- **AC-011**: Either picker inserts the image via `formatDotImageNode()` honoring the
  `WYSIWYG_IMAGE_URL_PATTERN` property, and returns focus to the editor on close (both
  insert and dismiss).

**File / Image field**

- **AC-012**: In the new Angular Edit Content, *Select Existing File* opens
  `DotAssetPickerComponent` (unchanged).
- **AC-013**: In the legacy Dojo host (`<dotcms-binary-field>`), it opens
  `DotBrowserSelectorComponent` with `mimeTypes: ['image']` for Image fields and `[]` for
  File fields, and the `dot.file.field.dialog.select.existing.image.header` /
  `...file.header` header.
- **AC-014**: The selected asset is written to the store identically on both paths (preview,
  `fileName` / title auto-fill, and the legacy binary-field contract consumed by the JSP
  editor).
- **AC-015**: It is decided and recorded whether
  `core-web/libs/ui/src/lib/components/dot-asset-picker/dot-asset-picker.component.legacy-host.spec.ts`
  stays as a safety net or is removed, since the picker is no longer opened from that host.

**Regression / edge cases**

- **AC-016**: Double-clicking the trigger still opens exactly one dialog on **both** paths;
  the pending-guard behavior added by #36944 is preserved for the new picker and does not
  leak into the legacy path.
- **AC-017**: Dismissing via ✕, Esc, or the overlay mask leaves no orphan dialog ref and no
  stuck pending flag on either path.
- **AC-018**: Destroying the field (navigating away, closing the side panel, removing the
  custom element) closes any open dialog on either path.
- **AC-019**: The legacy picker requires no `Router` and no app-shell providers — it
  constructs in the binary-field host, which bootstraps with only `provideHttpClient`,
  `provideAnimations`, `DotMessageService`, `DotUploadService`,
  `DotWorkflowActionsFireService` and the theme.
- **AC-020**: When the current site cannot be resolved, the new-picker path still opens
  nothing (existing behavior) and the legacy path is unaffected, since it needs no `DotSite`.
- **AC-021**: The new AssetPicker still opens in `DotEditContentDialogComponent` — i.e. from
  UVE's content dialog and from the Relationship field's nested editor — proving the
  three-host provisioning decision holds.
- **AC-022**: Whether `IMAGE_EDITOR_LAUNCHER` shares the missing-provider gap in
  `DotEditContentDialogComponent` is determined and recorded (fixed here or filed as a
  follow-up issue).

**Verification method**:

- **Unit (primary)** — Jest/Spectator, per the `TDD` gate. For each of the three entry
  points, a token-absent (legacy-host) factory and a token-present (Angular-host) factory,
  following `dot-file-field.component.legacy-availability.spec.ts`:
  - `cd core-web && pnpm nx test edit-content`
  - `cd core-web && pnpm nx test new-block-editor`
  - `cd core-web && pnpm nx test ui`
- **E2E (regression, must still pass unchanged)** — the Playwright specs added by #36944,
  which exercise the new Angular Edit Content: `wysiwyg-field-asset-picker.spec.ts`,
  `block-editor-field-asset-picker.spec.ts`, `file-field.spec.ts`, `image-field.spec.ts`.
- **Manual** — the reproduction steps above, both hosts, all three fields; plus opening a
  contentlet from UVE and from a Relationship field to confirm AC-021.
- **Lint / build** — `pnpm nx lint` + `pnpm nx build` for `ui`, `edit-content`,
  `new-block-editor`, `dotcms-block-editor`, `dotcms-binary-field-builder` (the module
  boundary / project-cycle risk in the Root-Cause Hypothesis is caught here).
- No integration or Postman tests: the change is front-end only, with no Java surface.

## Assumptions

- The Angular WYSIWYG field is **not** currently reachable from the legacy editor: the legacy
  page renders WYSIWYG as a plain `textarea` plus JSP-side TinyMCE
  (`edit_field.jsp:465-490`, `edit_field_js.jsp`), whose `dotAddImage` comes from
  `html/js/tinymce/js/tinymce/plugins/dotCustomButtons/plugin.min.js` and opens the Dojo
  `FileBrowserDialog`. No Angular custom element for the WYSIWYG field exists
  (`createCustomElement` appears only in the block-editor and binary-field apps). The
  WYSIWYG change is therefore a **consistency guard**, not a live regression — implemented
  anyway so the three entry points behave identically, and because
  `DotWysiwygPluginService` already documents itself as constructible by non-shell hosts.
- The new Edit Content is reached only through the three Angular hosts named above; any
  future host must provide the token, which the plan will note as a maintenance obligation.
- `DotBrowserSelectorComponent` and `DotAssetSearchDialogComponent` remain functional as
  exported from `@dotcms/ui`. Verified: the only #36944 change under
  `dot-browser-selector/` was `store/browser.store.ts`, and it is a pure refactor — helpers
  (`hasMorePages`, `stripLoadMore`, `withLoadMore`, `findSiteIdByHostname`,
  `findFolderParent`, `SITES_LOAD_MORE_KEY`, `SYSTEM_HOST_ID`) lifted to
  `dot-folder-tree/site-tree.utils.ts` and re-exported. No public contract changed.
- Restoring `buildBrowserSelectorConfig()` means restoring the pre-#36944 source verbatim
  (recoverable from `git show 2275b1bcea^:core-web/libs/new-block-editor/src/lib/editor/config.utils.ts`).
