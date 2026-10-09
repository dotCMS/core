# Contract: `DotLegacyEditorSidePanelComponent` (Content Drive lib)

The side panel that hosts the legacy content editor in an iframe. Location:
`core-web/libs/portlets/dot-content-drive/portlet/src/lib/components/dot-legacy-editor-side-panel/`.
Internal to the lib, not exported from its `index.ts`: the Content Drive shell is its only
consumer. Selector: `dot-legacy-editor-side-panel`. It leaves routing and URL decisions to the shell,
through the outputs below, so it can be deleted with the legacy editor without touching anything else.

## Inputs

| Input | Type | Notes |
|---|---|---|
| `request` | `DotLegacyEditorRequest \| null` | See [data-model.md §1](../data-model.md). `null` = closed. A new request object reloads the iframe |

## Outputs

| Output | Payload | Emitted when |
|---|---|---|
| `saved` | `{ identifier: string; inode: string; languageId: number }` | `save-page`. `languageId` is the language the panel is showing, else the matching `allLangContentlets` entry |
| `closed` | `void` | the editor's own `close`, `deleted-page`, or a user close (X, ESC, mask click, `requestClose()`) that passed the unsaved-changes prompt |
| `pageEditorRequested` | `{ url: string; languageId: number }` | `close` naming a page (first save of a new HTML page) |
| `languageChanged` | `number` | the iframe reloaded in another language |
| `versionRestored` | `void` | the iframe reloaded after a Bring Back |

The panel never navigates the router. What each output means is the caller's decision.

## Public methods

| Method | Notes |
|---|---|
| `requestClose(): void` | Runs the unsaved-changes prompt when dirty, then emits `closed`. Used by the caller for browser Back |

## Behavior it owns

- Drawer: right side, 80% width with an expand toggle to 100% (shared localStorage key
  `dot-edit-content-side-panel-expanded`), transparent mask, header with `request.title`, the expand
  toggle and the close button. It registers with `DotSidePanelNavController` (`@dotcms/edit-content`, as is) for
  stacking and nav collapse.
- ESC, from the parent document through `DotKeyboardShortcutService` or from inside the iframe
  (forwarded keydown), and a click on its own mask go through `requestClose()`, only when the panel
  is on top.
- Builds the iframe URL (research R3) and refuses a URL that is not same-origin relative.
- Handles `workflow-wizard`, `push-publish` and `compare-contentlet` through root services, and
  answers `DotIframeService.ran()` calls on its own window ([legacy-editor-events.md](legacy-editor-events.md)).
- Unsaved-changes prompt: component-level `ConfirmationService` with its own `<p-confirmDialog />`,
  keys `edit.content.unsaved.changes.{title,message,keep,discard}`.
- Applies the admin UI colors to the iframe document on load, as UVE's dialog does.

## Dependencies (all root-provided in the admin app)

`DotIframeService`, `DotWorkflowEventHandlerService`, `DotEventsService`, `DotUiColorsService`,
`DotActionUrlService` (moved to `@dotcms/data-access`), `DotMessageService` (`@dotcms/data-access`);
`DotPushPublishDialogService` (`@dotcms/dotcms-js`); `DotSidePanelNavController`
(`@dotcms/edit-content`); `DotKeyboardShortcutService` (`@dotcms/ui`). The Content Drive lib already
imports all of these libs. It needs these mounted dialogs: workflow wizard, push
publish and content compare, which `MainComponentLegacyComponent` mounts for every admin route.

## Usage (Content Drive)

```html
@if ($legacyPanelRequest(); as request) {
  <dot-legacy-editor-side-panel #legacyPanelRef
      [request]="request"
      (saved)="onLegacyPanelSaved($event)"
      (closed)="onLegacyPanelClosed()"
      (pageEditorRequested)="onLegacyPanelPageEditor($event)"
      (languageChanged)="onLegacyPanelLanguage($event)"
      (versionRestored)="onLegacyPanelRestored()" />
}
```
