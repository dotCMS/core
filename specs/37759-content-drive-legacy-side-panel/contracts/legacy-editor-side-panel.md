# Contract: `DotLegacyEditorSidePanelComponent` (Content Drive lib)

The side panel content that hosts the legacy content editor in an iframe. Location: `core-web/libs/portlets/dot-content-drive/portlet/src/lib/components/dot-legacy-editor-side-panel/`. Internal to the lib, not exported from its `index.ts`: the Content Drive shell is its only consumer. Selector: `dot-legacy-editor-side-panel`. It opens in the admin's single side-panel outlet through `DotSidePanelService` (`@dotcms/ui`), which draws the drawer around it. It leaves routing and URL decisions to the shell, through the outputs below, so it can be deleted with the legacy editor without touching anything else.

## Inputs

| Input | Type | Notes |
|---|---|---|
| `request` | `DotLegacyEditorRequest \| null` | See [data-model.md §1](../data-model.md). Set for as long as the panel is open. A new request object reloads the iframe |

## Outputs

| Output | Payload | Emitted when |
|---|---|---|
| `saved` | `{ identifier: string; inode: string; languageId: number }` | `save-page`. `languageId` is the language the panel is showing, else the matching `allLangContentlets` entry |
| `pageEditorRequested` | `{ url: string; languageId: number }` | `close` naming a page (first save of a new HTML page) |
| `languageChanged` | `number` | the iframe reloaded in another language |
| `versionRestored` | `void` | the iframe reloaded after a Bring Back |

The panel never navigates the router. What each output means is the caller's decision. The panel closes itself through its `DotSidePanelRef` on the editor's own `close`, on `deleted-page`, and on a user close that passed the unsaved-changes prompt; the shell hears it through `injectSidePanel`'s `closed`.

## Public methods

| Method | Notes |
|---|---|
| `requestClose(): void` | From `DotSidePanelContent` (`@dotcms/ui`). Runs the unsaved-changes prompt when dirty, then closes the panel through its `DotSidePanelRef`. Every user close goes through it: the X, ESC, a click on the mask, and browser Back, which the shell routes through its handle on the panel |

## Behavior it owns

- The drawer is not its own: `DotSidePanelComponent` (`@dotcms/ui`) draws it, with the expand toggle to 100% (shared localStorage key `dot-edit-content-side-panel-expanded`), the transparent mask, the header with `request.title`, the expand and close buttons, ESC and the click on its mask; `DotSidePanelService` collapses the main navigation while any panel is open. The panel only makes its content area a flex row with no padding, so the iframe fills it.
- ESC pressed inside the iframe never reaches the admin page, so the panel presses it again on the admin page, where the drawer's ESC rules decide. Not one the editor already handled (`defaultPrevented`).
- Builds the iframe URL (research R3) and refuses a URL that is not same-origin relative.
- Handles `workflow-wizard`, `push-publish` and `compare-contentlet` through root services, and answers `DotIframeService.ran()` calls on its own window ([legacy-editor-events.md](legacy-editor-events.md)).
- Unsaved-changes prompt: component-level `ConfirmationService` with its own `<p-confirmDialog />`, keys `edit.content.unsaved.changes.{title,message,keep,discard}`.
- Applies the admin UI colors to the iframe document on load, as UVE's dialog does.

## Dependencies

Root-provided in the admin app: `DotIframeService`, `DotWorkflowEventHandlerService`, `DotEventsService`, `DotUiColorsService`, `DotMessageService` (`@dotcms/data-access`); `DotPushPublishDialogService` (`@dotcms/dotcms-js`). From its side panel: `DotSidePanelRef` (`@dotcms/ui`). The Content Drive lib already imports all of these libs. It needs these mounted dialogs: workflow wizard, push publish and content compare, which `MainComponentLegacyComponent` mounts for every admin route.

## Usage (Content Drive)

```ts
readonly #legacyPanel = injectSidePanel(this.$legacyPanelRequest, {
    component: () => DotLegacyEditorSidePanelComponent,
    heading: (request) => request.title,
    inputs: (request) => ({ request }),
    outputs: {
        saved: (saved: DotLegacyEditorSaved) => this.onLegacyPanelSaved(saved),
        pageEditorRequested: (page: DotLegacyEditorPageRequest) =>
            this.onLegacyPanelPageEditor(page),
        versionRestored: () => this.onLegacyPanelRestored(),
        languageChanged: (languageId: number) => this.onLegacyPanelLanguage(languageId)
    },
    closed: () => this.onLegacyPanelClosed()
});
```
