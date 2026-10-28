# Contract: the legacy-screen panel and the legacy editor panel

Both live in `core-web/libs/edit-content/src/lib/components/`:

- `dot-legacy-screen-side-panel/` (new): loads any same-origin admin URL.
- `dot-legacy-editor-side-panel/` (moved from `libs/portlets/dot-content-drive/portlet/src/lib/components/`, rebuilt on the generic panel).

Both are exported lazily: `loadDotLegacyScreenSidePanel()` and `loadDotLegacyEditorSidePanel()`.

## `DotLegacyScreenSidePanelComponent`

```ts
export interface DotLegacyScreenRequest {
    /** Must pass `isSameOriginRelativeUrl`; otherwise nothing renders. */
    url: string;
    title: string;
}

/** The `ng-event` CustomEvent detail the legacy screens dispatch on their own document. */
export interface DotLegacyScreenEvent {
    name: string;
    payload?: Record<string, unknown> | boolean;
    data?: Record<string, unknown>;
}

@Component({ selector: 'dot-legacy-screen-side-panel' })
export class DotLegacyScreenSidePanelComponent implements DotSidePanelContent {
    readonly request = input<DotLegacyScreenRequest | null>(null);

    /** Every `ng-event` the screen dispatches, unchanged. */
    readonly legacyEvent = output<DotLegacyScreenEvent>();

    /** The screen's document loaded (first load or a reload). */
    readonly loaded = output<Window>();

    /** Closes immediately; a guard, if any, belongs to the component built on top. */
    requestClose(): void;
}
```

What it does, all carried over from today's legacy editor panel (`dot-legacy-editor-side-panel.component.ts`): sets `contentStyleClass` to fill the drawer; shows the loading cover until `load` and again on `pagehide`; applies `DotUiColorsService.setColors` to the iframe's `<html>`; re-attaches listeners on every load; forwards an unhandled Escape pressed inside the iframe to the admin document so the panel's own Escape rule decides; detaches on destroy.

## Legacy screens the UVE opens through it

| Screen | URL builder (moved to `edit-content` or kept in the UVE) | Events the UVE handles from `legacyEvent` |
|---|---|---|
| Add content / add widget | `createAddContentletUrl(containerId, acceptTypes, languageId, variantName)` from `dot-ema-dialog.store.ts:354`, moved to the UVE's utils | `select-contentlet` (insert), `create-contentlet-from-edit-page` (open a create request through the opener) |
| Reorder menu | `createReorderMenuURL` (`libs/portlets/edit-ema/portlet/src/lib/utils/index.ts:967`, unchanged) | `save-menu-order`, `error-saving-menu-order`, `cancel-saving-menu-order` |
| Translate page | `createTranslatePageUrl` from `dot-ema-dialog.store.ts:374`, moved to the UVE's utils | `save-page`, `language-is-changed`, `close` |

The form selector is not a legacy screen: `EmaFormSelectorComponent` opens directly as panel content through `DotSidePanelService.open` with its `selected` output.

## `DotLegacyEditorSidePanelComponent`

```ts
@Component({ selector: 'dot-legacy-editor-side-panel' })
export class DotLegacyEditorSidePanelComponent implements DotSidePanelContent {
    readonly request = input<DotEditContentRequest | null>(null);

    /** The server-resolved create screen, supplied by the opener for `mode: 'new'`. */
    readonly createUrl = input<string | null>(null);

    // The five contract events (panel-events.md)
    readonly saved = output<DotCMSContentlet>();
    readonly languageChanged = output<number>();
    readonly pageEditorRequested = output<DotPageEditorRequest>();
    readonly deleted = output<void>();
    readonly closed = output<void>();

    /** A block editor inside the legacy editor asked to open other content (open-content-from-fields.md). */
    readonly openContent = output<DotOpenContentEvent>();

    /** Asks first when the editor reported unsaved changes; closes otherwise. */
    requestClose(): void;
}
```

URL it builds for `mode: 'edit'` (today's `#buildEditUrl` plus the two UVE params):

```
/c/portal/layout?p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view
  &_content_struts_action=/ext/contentlet/edit_contentlet&_content_cmd=edit
  &inode=<inode>&angularCurrentPortlet=<portletId>
  [&variantName=<variantName>][&host_id=<siteId>]
```

For `mode: 'new'`: `createUrl` plus `&folder=<folderInode>` when set.

How it maps the editor's events:

| `ng-event` name | Action |
|---|---|
| `save-page` | clear dirty; fetch `payload.contentletInode`; emit `saved` |
| `close` with `data.redirectUrl` | emit `pageEditorRequested({ url, languageId })` |
| `close` otherwise | close the panel (→ `closed`) |
| `deleted-page` | emit `deleted`; close |
| `edit-contentlet-data-updated` | dirty = payload |
| `workflow-wizard` | `DotWorkflowEventHandlerService.open(data)` (root instance) |
| `compare-contentlet` | `DotEventsService.notify('compare-contentlet', data)` (root compare dialog) |
| `push-publish` | `DotPushPublishDialogService.open(data)` |
| `edit-contentlet` | emit `openContent({ request: { mode: 'edit', inode, contentType } })` |
| `create-contentlet` | emit `openContent({ request: { mode: 'new', contentType, folderPath } })` |
| anything else | ignored |

On each iframe load: read `lang` from the iframe location and emit `languageChanged` when it differs; if a `getVersionBack` was in flight, read `inode` from the iframe location, fetch, emit `saved`.

It keeps the `DotIframeService.ran()` subscription so the workflow wizard's `saveAssignCallBackAngular` and the compare dialog's `getVersionBack` reach the editor; a call for a function the editor does not define is ignored, as today.
