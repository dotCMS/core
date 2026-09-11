# Issue Resolution Specification: New image editor opens blank inside UVE — NG0201: No provider found for MessageService

**Feature Branch**: `37398-image-editor-message-service`

**Created**: 2026-09-04

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37398](https://github.com/dotCMS/core/issues/37398) (epic: [#36702](https://github.com/dotCMS/core/issues/36702))

**Input**: User description: "https://github.com/dotCMS/core/issues/37398"

## Problem Statement *(mandatory)*

Opening **Edit image** on an Image/File field from inside **UVE** renders an empty white dialog.
The new image editor never paints, and the browser console throws:

```
ERROR ɵNotFound: NG0201: No provider found for `MessageService`.
Source: Standalone[_DotImageEditorComponent].
```

The same field works from the full-screen Edit Content route, so the failure is specific to the
new Edit Content experience when it is hosted by UVE. Because it is a dependency-injection
failure and not a rendering one, it is not browser- or OS-specific (reported on Chrome / macOS).

A second, quieter symptom shares the same root: with the side-panel feature flag **off**, UVE
opens the centered `DotEditContentDialogComponent`, which provides neither `DialogService` nor
`IMAGE_EDITOR_LAUNCHER`. There the Image/File field's optional launcher injection resolves to
`undefined` and the field silently falls back to the **legacy Dojo image editor** — no error, just
the wrong editor.

**Severity / Impact**: **High — major functionality broken.** It hits anyone editing an Image or
File field without leaving the page editor, which is the primary flow the new Edit Content
experience was built for. Inside UVE the image editor is completely unusable; the only workaround
is to abandon UVE and open the content full-screen.

## Reproduction *(mandatory)*

**Environment**: Latest `main`; dotCMS admin UI (Angular `core-web`); new Edit Content experience
enabled with the side-panel flag **on**; Chrome on macOS (not browser-specific).

**Steps to Reproduce**:

1. Open a page in **UVE** with the new Edit Content experience enabled and the side-panel flag on.
2. Click a contentlet that has an **Image** field, so the Edit Content side panel opens.
3. Hover the image and click the **pencil / Edit image** action.
4. Observe the dialog and the browser console.

**Expected Behavior**: The "Edit image" modal renders completely — address bar, canvas, and the
Adjust / Transform / File info panels, plus the footer — exactly as it does from the full-screen
Edit Content route.

**Actual Behavior**: The dialog opens **completely blank** — no toolbar, no canvas, no panels — and
the console logs `NG0201: No provider found for MessageService. Source:
Standalone[_DotImageEditorComponent]`.

**Reproducibility**: **Always**, in every host that lacks an ambient `MessageService`. Confirmed
deterministic (a construction-time injection failure, not a race).

Secondary path, also always reproducible: turn the side-panel flag **off**, open the same field
from UVE, and the **legacy Dojo** editor opens instead of the new one, with a clean console.

## Scope of Investigation *(mandatory)*

- **Affected area**: Frontend only — the new Angular Edit Content experience: the
  `@dotcms/image-editor` library and the three Angular Edit Content hosts that can open it
  (full-screen route, side panel, centered dialog). No backend or REST surface is involved.
- **Suspected surface**: Modern Angular/TypeScript under `core-web/libs/` — no `com.dotcms.*` or
  `com.dotmarketing.*` Java code is touched. The legacy Dojo image editor is only relevant as the
  fallback the secondary path incorrectly reaches; it is not modified.
- **Related known decisions**: The library already establishes the intended pattern next door —
  `DotAssetPickerComponent` self-provides `MessageService` and renders its own `<dot-toast />`
  outlet, which is why it works in every host. The provider-scoping comments in
  `edit-content.shell.component.ts` and `dot-edit-content-side-panel.component.ts` document a
  deliberate decision to scope the new editor/picker launchers per host rather than globally; the
  fix must preserve that scoping. The plan formally consults `dotCMS/platform-adrs`.

Confirmed in the codebase during scoping:

| Host | Where | `DialogService` | `IMAGE_EDITOR_LAUNCHER` | `MessageService` | Result |
|---|---|---|---|---|---|
| Full-screen route | `libs/edit-content/src/lib/edit-content.shell.component.ts` `providers` | yes | yes | yes | Editor works |
| Side panel (UVE, Content Drive, Query Tool) | `libs/edit-content/src/lib/components/dot-edit-content-side-panel/dot-edit-content-side-panel.component.ts:81-103` | yes | yes | **no** | **Opens blank — NG0201** |
| Centered dialog (UVE, side-panel flag off) | `libs/edit-content/src/lib/components/dot-create-content-dialog/dot-create-content-dialog.component.ts` `providers` | **no** | **no** | **no** | Editor never opens; silently falls back to legacy Dojo |

## Root-Cause Hypothesis

`DotImageEditorAddressBarComponent` injects PrimeNG's `MessageService` non-optionally, to surface
the copy-URL toast:

```ts
// libs/image-editor/src/lib/components/dot-image-editor-address-bar/dot-image-editor-address-bar.component.ts:37
readonly #messageService = inject(MessageService);
```

Nothing in `@dotcms/image-editor` provides that token — the library implicitly relies on its host
having one. `AngularImageEditorLauncher` opens the editor through the `DialogService` provided on
the host component, so the dialog's injector chain is the host's chain. In the side panel that
chain has no `MessageService`, so the address bar's `inject()` throws while the component tree is
being constructed, `DotImageEditorComponent` fails to instantiate, and the dialog paints as an
empty white box.

Two supporting findings, both verified:

- An audit of every `inject()` in `libs/image-editor/src` (excluding specs) shows `MessageService`
  is the **only** host-dependent token the library does not either provide itself or obtain from a
  root-provided service. `DotImageEditorService`, `DotPropertiesService` and `DotMessageService`
  are `providedIn: 'root'`; `ImageEditorStore` and `ConfirmationService` are declared in
  `DotImageEditorComponent`'s own `providers`; `Dialog` is injected `{ optional: true }`.
- The existing Jest specs do not catch this because they replace the child components with
  `MockComponent(...)` via `overrideComponents` — `dot-image-editor.component.spec.ts` mocks the
  canvas, and `dot-image-editor-canvas.component.spec.ts` mocks the address bar — so the real
  `inject(MessageService)` never executes in any test.

For the secondary path, the cause is simply that `DotEditContentDialogComponent` was never given
the two providers the shell and the side panel have, so the field's
`inject(IMAGE_EDITOR_LAUNCHER, { optional: true })` legitimately resolves to `undefined`.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Make `@dotcms/image-editor` self-sufficient for messaging: `DotImageEditorComponent` declares
  `MessageService` in its own `providers` and renders its own toast outlet, following the
  `DotAssetPickerComponent` precedent, so no host is required to supply either.
- Ensure the copy-URL success and error toasts render inside the editor dialog and are visible
  above it, in every host.
- Provide `DialogService` and `IMAGE_EDITOR_LAUNCHER` on `DotEditContentDialogComponent` the way
  the shell and the side panel provide them, so the centered-dialog host opens the new editor
  instead of silently downgrading to legacy Dojo.
- Audit the library's remaining `inject()` sites once, so a future host cannot hit the same class
  of failure.
- Add regression coverage that exercises the real child component tree (not mocks) with no ambient
  `MessageService`, plus coverage asserting `IMAGE_EDITOR_LAUNCHER` resolves in all three hosts.

**Explicitly out of scope / non-goals**:

- No changes to the legacy Dojo image editor, and no removal of the legacy fallback path itself.
- No change to the full-screen shell's own `MessageService` / `<p-toast />`, which stays for the
  shell's own messages.
- No move of `MessageService` (or the launcher tokens) to a root/application-level provider — the
  per-host scoping of `IMAGE_EDITOR_LAUNCHER` / `ASSET_PICKER_LAUNCHER` is deliberate and is
  preserved.
- No redesign of the image editor's toast/messaging UX, no new toast positions or styles beyond
  what is needed to render inside the dialog.
- No change to the side-panel feature flag, its default, or the choice of which host UVE opens.
- No refactor of `ImageEditorStore`, the editor's features, or the asset picker.
- No broader dependency-injection refactor of other `@dotcms/*` libraries, even if the same
  implicit-host-provider pattern exists elsewhere.

## Regression Risk *(mandatory)*

- **Blast radius**: `@dotcms/image-editor` is consumed by all three Angular Edit Content hosts, so
  the change is exercised by UVE, Content Drive, the Query Tool, the Relationship field's dialog,
  and the full-screen route. Adding a component-scoped `MessageService` shadows any ambient one
  for the editor's subtree: in the full-screen shell, editor toasts will now render through the
  editor's own outlet rather than the shell's `<p-toast />`. That is the intended behavior, but it
  is a visible change in where those specific toasts appear. A second toast outlet inside a
  PrimeNG `DynamicDialog` also carries a stacking/`z-index` risk — the toast must be visible above
  the dialog, not trapped behind its mask. Giving `DotEditContentDialogComponent` a `DialogService`
  and the launcher token changes which editor that host opens for Image/File/Binary fields; that
  path currently runs the legacy Dojo editor, so it moves from one working editor to another.
- **Backward compatibility**: Frontend-only. No REST contract, `@Schema`, `openapi.yaml`, DB
  schema, or Elasticsearch/OpenSearch mapping is touched, so nothing here falls into a
  rollback-unsafe category. Saved image edits continue to flow through the existing temp-file
  mechanism unchanged. The public surface of `@dotcms/image-editor` gains no new required input and
  loses no existing one; hosts that already provide `MessageService` keep working without edits.
- **Data considerations**: None. No stored content, field value, or serialized state is read or
  written differently, so there is no existing bad data to migrate or repair.

## Acceptance & Verification *(mandatory)*

### The editor is self-sufficient

- **AC-001**: `DotImageEditorComponent` declares `MessageService` in its own `providers` and
  renders its own toast outlet (`<dot-toast />`), so the editor no longer depends on the host
  providing either.
- **AC-002**: The copy-URL success and error toasts raised by
  `DotImageEditorAddressBarComponent` render **inside** the editor dialog and are visible above
  it, in every host.
- **AC-003**: No host is required to add `MessageService` for the editor to work. The full-screen
  shell keeps its own `MessageService` / `<p-toast />` for its own messages, unchanged.
- **AC-004**: No other `inject()` in `@dotcms/image-editor` resolves against a token the library
  neither provides itself nor obtains from a root-provided service, so a second host cannot hit
  the same class of failure.

### Works in every Angular Edit Content host

- **AC-005**: **Side panel** (UVE, Content Drive, Query Tool) — the reproduction above no longer
  produces a blank dialog: the "Edit image" modal opens and renders fully (address bar, canvas,
  Adjust / Transform / File info panels, footer).
- **AC-006**: **Centered dialog** (`DotEditContentDialogComponent`, side-panel flag off) — the
  modal opens the **new** editor, not the legacy Dojo one, because `DialogService` and
  `IMAGE_EDITOR_LAUNCHER` are provided there the way the shell and the side panel provide them.
- **AC-007**: **Full-screen route** — unchanged; no regression in opening, rendering, or closing
  the editor.
- **AC-008**: Save, Cancel, Download, and the unsaved-changes confirm dialog all work in each of
  the three hosts.
- **AC-009**: The browser console is free of `NG0201` in all three hosts.

### Regression coverage

- **AC-010**: `dot-image-editor.component.spec.ts` instantiates the component with **no ambient
  `MessageService`** and asserts it renders. The test must exercise the real child tree along the
  path that injects `MessageService` (today both `dot-image-editor.component.spec.ts` and
  `dot-image-editor-canvas.component.spec.ts` replace those children with `MockComponent(...)`,
  which is precisely why the defect is invisible in CI). The test fails on the current code and
  passes after the fix.
- **AC-011**: `dot-image-editor-address-bar.component.spec.ts` covers the copy-URL toast resolving
  through the component-scoped `MessageService`.
- **AC-012**: A spec asserts `IMAGE_EDITOR_LAUNCHER` resolves (is not `undefined`) in each of the
  three hosts, so a host losing the provider fails a test instead of silently downgrading to the
  legacy editor.
- **AC-013**: The existing binary-field E2E that drives `image-editor-root`
  (`apps/dotcms-ui-e2e/src/tests/edit-content/fields/file-upload-fields/binary-field/`) still
  passes.

**Verification method**:

- Jest/Spectator (Red first, per Principle V):
  `cd core-web && pnpm nx test image-editor` for AC-001, AC-002, AC-004, AC-010, AC-011;
  `pnpm nx test edit-content` for AC-012.
- Playwright E2E: the existing binary-field image-editor spec under
  `apps/dotcms-ui-e2e/src/tests/edit-content/fields/file-upload-fields/binary-field/` for AC-013.
- Manual, once per host, for AC-005 through AC-009: reproduce the steps above in the side panel,
  then with the side-panel flag off, then from the full-screen route — each time exercising Save,
  Cancel, Download and the unsaved-changes confirm, and checking the console for `NG0201`.

## Assumptions

- `<dot-toast />` (`libs/ui/src/lib/components/dot-toast/dot-toast.component.ts`) is the correct
  outlet to reuse, as `DotAssetPickerComponent` does; the plan confirms whether it needs a
  specific `position` or `z-index` treatment to sit above a PrimeNG `DynamicDialog`.
- "Every Angular Edit Content host" means exactly the three hosts enumerated in the table above.
  Any additional consumer of `@dotcms/image-editor` found during planning inherits AC-001 through
  AC-004 for free, since the fix makes the library self-sufficient rather than patching hosts.
- The issue's report is treated as accurate and was re-verified against `main` during scoping: the
  `inject(MessageService)` site, the three hosts' `providers`, the `DotAssetPickerComponent`
  precedent, and the mocked-child specs were all confirmed in the codebase.
- The centered-dialog fix (AC-006) is in scope because it shares the same root — hosts not
  declaring what the editor needs — even though its user-visible symptom differs.
