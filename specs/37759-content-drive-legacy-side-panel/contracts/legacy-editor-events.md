# Contract: legacy editor events the panel consumes

The legacy content editor (JSP, `dotCMS/src/main/webapp/html/portlet/ext/contentlet/`) dispatches
`ng-event` CustomEvents on its own `document`. This work **does not change the JSP**. It reads the
events below and ignores every other one (FR-008). Source lines are from the investigation on
2026-10-01.

| `detail.name` | Payload | Sent when | Source | Panel reaction |
|---|---|---|---|---|
| `save-page` | `detail.payload`: the save callback map. Read: `contentletIdentifier`, `contentletInode`, `allLangContentlets: { inode, identifier, languageId }[]` | a save succeeded (plain save, or a workflow action, including through the wizard) | `edit_contentlet_js_inc.jsp:667-725`; map from `ContentletAjax.java:2015-2033, 2766-2790` | clear dirty; emit `saved` |
| `close` with `detail.data.redirectUrl` | `{ redirectUrl: string, languageId: string }` | first save of a new HTML page whose create form carries a referer | `edit_contentlet_js_inc.jsp:685-699` | emit `pageEditorRequested` |
| `close` (no data) | none | the editor's own close; content deleted/destroyed (non-page) | `edit_contentlet_js_inc.jsp:704-707` and the editor's close action | emit `closed` |
| `deleted-page` | `detail.payload`: the save callback map | an HTML page was deleted | `edit_contentlet_js_inc.jsp:709-714` | emit `closed` |
| `edit-contentlet-data-updated` | `detail.payload: boolean` (dirty) | a field changed | `edit_contentlet_js_inc.jsp:526-533`, `field/edit_field_js.jsp:326` | set dirty |
| `workflow-wizard` | `detail.data: { workflow, callback, inode, selectedInodes? }` | a workflow action needs input (comments, assignment, push publish inputs) | `/html/js/dotcms/dijit/RemotePublisherDialog.js:259-277` | `DotWorkflowEventHandlerService.open(detail.data)` |
| `push-publish` | `detail.data: { assetIdentifier, dateFilter, isBundle, removeOnly, restricted, cats, title, customCode? }` | a push publish action | `RemotePublisherDialog.js:251-256`, `/html/js/dotcms/dojo/push/PushHandler.js:684-692` | `DotPushPublishDialogService.open(detail.data)` |
| `compare-contentlet` | `detail.data: { inode, identifier, language }` | History tab, compare | `edit_contentlet_js_inc.jsp:137-144` (`emmitCompareEvent`) | `DotEventsService.notify('compare-contentlet', detail.data)` |

## Signals that are not events

| Signal | How the panel notices | Panel reaction |
|---|---|---|
| Language switch | the iframe reloads itself with `lang=<id>` in its query string (`edit_contentlet_basic_properties.jsp:267-325`) | on `load`, read `lang`. If it changed, emit `languageChanged`. Clear dirty |
| Bring Back | `DotIframeService.ran()` carries `getVersionBack`; the iframe then reloads with the restored version | on the next `load`, emit `versionRestored` |
| Rejected save (validation) | nothing is dispatched (`saveContentErrors` branch returns early) | none: stays dirty |

## Calls into the iframe window

The panel calls these globals on its iframe's `contentWindow` when `DotIframeService.ran()` names
them (the panel only calls a name that is a function on its own window):

| Function | Called by | Effect |
|---|---|---|
| `saveAssignCallBackAngular(actionId, formData)` | `DotWorkflowEventHandlerService` after the wizard | fills the workflow fields and runs `saveContent`, which ends in `save-page`/`close`/`deleted-page` |
| `getVersionBack(inode)` | the compare dialog's Bring Back | reloads the editor with that version |

## Explicitly ignored

`edit-contentlet-loaded`, `edit-page` (no screen sends it; page-editor links are plain links,
FR-011), `update-workflow-action`, `language-is-changed`, `edit-contentlet`, `create-contentlet`, and
any other name.
