# Data Model: Content Drive — open legacy-editor content in the side panel

**Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md)

This is a frontend-only feature. There is no database, index or REST change. The "entities" are
the client-side state that decides which panel or dialog is open and what the URL shows. Names are
proposals for the implementation; tasks may refine them.

## 1. `DotLegacyEditorRequest` (new, Content Drive lib)

What the legacy panel is asked to open. It is the input of `DotLegacyEditorSidePanelComponent`
([contracts/legacy-editor-side-panel.md](contracts/legacy-editor-side-panel.md)).

| Field | Type | Required | Notes |
|---|---|---|---|
| `mode` | `'edit' \| 'new'` | yes | |
| `inode` | `string` | `edit` | The working version in the requested language |
| `identifier` | `string` | `edit` | Reported back with outputs. Not used to build the URL |
| `contentTypeVariable` | `string` | `new` | Passed to `/api/v1/portlet/_actionurl/<variable>` |
| `folderInode` | `string` | no | `new` only. Appended as `folder=<inode>`. System Host passes its identifier, as today |
| `languageId` | `number` | yes | `edit`: the version's language. `new`: the list's language filter or the default (R13) |
| `title` | `string` | yes | Drawer header: the content's title, or the type's name for a create |
| `portletId` | `string` | yes | Sent as `angularCurrentPortlet`. Content Drive passes `content-drive` |

Validation: `edit` without `inode`, or `new` without `contentTypeVariable`, opens nothing. A built
URL that is not same-origin relative is refused (`isSameOriginRelativeUrl`).

## 2. `DotContentDrivePanelRequest` (new, Content Drive)

The single source signal in `DotContentDriveNavigationService` (R9).

```text
DotContentDrivePanelRequest =
  | { editor: 'new';    data: EditContentDialogData }    // today's request, unchanged shape
  | { editor: 'legacy'; data: DotLegacyEditorRequest }
  | null                                                  // nothing open
```

Derived, read-only:
- `$editPanelRequest`: `data` when `editor === 'new'`, else `null`. The same signal and shape as today.
- `$legacyPanelRequest`: `data` when `editor === 'legacy'`, else `null`.

`EditContentDialogData` (`libs/edit-content/src/lib/models/dot-edit-content-dialog.interface.ts`)
does not change shape. Its existing `languageId` starts being read for `mode: 'new'` (R13).

## 3. `DotContentDrivePanelLocation` (new, Content Drive)

What the URL must say about the open panel (R9, R10). Starts from the request, then follows the
panel without remounting it.

```text
DotContentDrivePanelLocation =
  | { kind: 'edit';   editContent: string /* identifier */; editContentLang: number }
  | { kind: 'create'; createContent: string /* content type variable */ }
  | null
```

State transitions:

```text
null ──open edit──────────────▶ edit(id, lang)                               [location.go]
null ──open create────────────▶ create(type)                                 [location.go]
create(type) ──first save─────▶ edit(savedId, savedLang)                     [replaceState]
edit(id, lang) ──legacy language switch──▶ edit(id, newLang)                 [replaceState]
edit(id, lang) ──switch to old editor (new panel)──▶ edit(id, lang)          [no URL change; the request becomes legacy]
create(type) ──switch to old editor (new panel)──▶ create(type)               [no URL change; a legacy create opens for the same type]
any ──close (any reason)──────▶ null                                         [replaceState]
```

A save rejected by validation sends no `save-page`, so `create` stays `create`.

## 4. Folder-dialog state (extended, Content Drive store)

The store's dialog feature (`store/features/dialog/withDialog.ts`) gains one dialog type.

| `DIALOG_TYPE` | Payload | URL param while open |
|---|---|---|
| `FOLDER` (no payload) | none | `createFolder=true` (its parent is `path`) |
| `FOLDER` (with payload) | `DotContentDriveActionableFolder` | `editFolder=<payload.identifier>` |
| `FOLDER_PERMISSIONS` (new) | `{ identifier: string }` | `folderPermissions=<identifier>` |
| `CONTENT_TYPE_SELECTOR`, `ACTION_CENTER` | unchanged | none |

`DotContentDriveActionableFolder` (`libs/dotcms-models/src/lib/dot-content-drive.model.ts:31-66`)
is unchanged. A folder resolved from the URL (R17) fills it from the `GET /api/v1/folder/{id}` bean:
`identifier`, `inode`, `name`, `path`, `title`, `sortOrder`, `showOnMenu`, `filesMasks`,
`defaultFileType`, `defaultBaseType`. `permissions` is built from the user's `canEdit` (`EDIT`)
and `canEditPermissions` (`EDIT_PERMISSIONS`) flags.

## 5. URL intent (new, Content Drive, pure function)

The result of reading the query params once on load (R11).

| Intent | When | Action |
|---|---|---|
| `none` | no panel or folder-dialog param (`editContent=new` counts as absent) | nothing |
| `edit { identifier, languageId? }` | only `editContent` (+ optional `editContentLang`) | `openEditByIdentifier` |
| `create { contentType }` | only `createContent` | `createContent` once `path` resolves |
| `createFolder` | only `createFolder=true` | open New Folder |
| `editFolder { identifier }` | only `editFolder` | resolve, check `canEdit`, open Folder Settings |
| `folderPermissions { identifier }` | only `folderPermissions` | check `canEditPermissions`, open Edit Permissions |
| `conflict` | `editContent` + `createContent`; a panel param + a folder-dialog param; two folder-dialog params | open nothing, remove all six params (`replaceState`) |

## 6. Legacy panel internal state (Content Drive lib)

| State | Set by | Cleared by |
|---|---|---|
| `dirty: boolean` | `edit-contentlet-data-updated` (`payload`) | `save-page`, every iframe reload, confirmed close |
| `languageId: number` | the request; `lang` on iframe reload (R7) | n/a |
| `restoreInFlight: boolean` | `DotIframeService.ran()` with `getVersionBack` | the next iframe `load` (then emits `versionRestored`) |
| `expanded: boolean` | expand toggle; localStorage `dot-edit-content-side-panel-expanded` | n/a |

## 7. `EditContentIdentity` (extended, `libs/edit-content`)

`services/host/edit-content-host.model.ts:26-33` gains an optional field:

| Field | Type | Notes |
|---|---|---|
| `languageId` | `number \| undefined` | Overlay host fills it from `EditContentDialogData.languageId`. Router host leaves it unset. `loadSystemLocales` picks that locale, else the system default |

## 8. `EditContentHost` port (extended, `libs/edit-content`)

See [contracts/edit-content-host-port.md](contracts/edit-content-host-port.md).
