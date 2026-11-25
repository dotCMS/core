# Data Model: Open every content edit and create in the side panel

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-28

This feature stores nothing. The "data" is the set of in-memory shapes that travel between a surface, the opener and the two panels. Each entity below maps to a spec Key Entity; the exact TypeScript is in [contracts/](contracts/).

## Edit request (`DotEditContentRequest`)

What a surface asks to open. Replaces `EditContentDialogData` and `DotLegacyEditorRequest`.

| Field | Type | Required | Rules |
|---|---|---|---|
| `mode` | `'edit' \| 'new'` | yes | Decides which of the two groups below is required |
| `inode` | string | edit | The working version to edit. `edit` without an inode is invalid and opens nothing |
| `identifier` | string | no | Reported back with events; the opener fills it from the fetched contentlet when absent |
| `contentType` | string | new | The content type **variable**. For `edit` it is optional: the opener fetches the contentlet by inode when it is missing (the `edit-host` legacy event sends only an inode) |
| `languageId` | number | no | Defaults to the surface's current language; the legacy create URL needs it and the new editor's create uses it |
| `folderPath` | string | no | Pre-selects the folder in the new editor's create |
| `folderInode` | string | no | Pre-selects the folder in the legacy create screen (`folder=<inode>`) |
| `title` | string | no | The panel heading. Falls back to the content type name for a create and the contentlet title for an edit |
| `variantName` | string | no | The experiment variant to edit or create in. Absent means the default variant |
| `siteId` | string | no | The site the request belongs to (`host_id` on the legacy edit URL) |
| `portletId` | string | no | The admin portlet the request comes from, sent to the legacy editor as `angularCurrentPortlet`. Defaults to `'edit-page'` in the UVE and `'content'` elsewhere |
| `depth` | number | no | Relationship depth for the new editor's load, as today |
| `relationship` | `{ parentContentletId; relationshipName; isParent }` | no | Present only when a Relationship field asked for a create; the reply needs it |

Validation: the opener rejects a request that has neither an `inode` (edit) nor a `contentType` (new) and reports the standard error; nothing opens.

What the request never carries: which editor to use, callbacks, a server-resolved create URL, or an iframe URL. Those belong to the opener and the panels.

## Editor choice (`DotContentEditorChoice`)

Owned by `DotContentletEditUrlService`. Derived from the content type's `metadata.CONTENT_EDITOR2_ENABLED`.

| Value | When |
|---|---|
| `'new'` | The content type's metadata has `CONTENT_EDITOR2_ENABLED: true` |
| `'legacy'` | Otherwise, or when the content type lookup fails |

Cached per content type variable for the life of the application, as the URL resolver's cache is today. The resolved `DotCMSContentType` travels with the choice so the opener can pass the id, the variable and the name to the panels without a second lookup.

## Panel events (`DotEditContentPanelEvents`)

What a panel tells its opener. Both panel components expose all five as Angular outputs; the opener re-exposes them to the surface as a handler map.

| Event | Payload | New editor source | Legacy editor source |
|---|---|---|---|
| `saved` | `DotCMSContentlet` | each successful save or restore | `save-page`, after fetching by inode; a restore after `getVersionBack`, after fetching the restored inode |
| `languageChanged` | `number` | the overlay host's `languageChanged$` | the `lang` query param on the iframe reload |
| `pageEditorRequested` | `{ url: string; languageId: number }` | never | `close` carrying `data.redirectUrl` |
| `deleted` | void | the overlay host's `left$` | the editor's `ngEditContentletEvents` window hook, supplied by the panel, called with `deleted-contenlet`; or `deleted-page` for a page |
| `closed` | void | the panel ref's `closed` | the panel ref's `closed` |

Ordering rules: `deleted` is always followed by `closed`; `saved` is never emitted for a delete; `closed` is emitted exactly once per panel.

## Open-content request from a field (`DotOpenContentEvent`)

What a field hands its host when it wants other content opened.

| Field | Type | Required | Rules |
|---|---|---|---|
| `request` | `DotEditContentRequest` | yes | An edit request for an existing item or a create request for "create new" |
| `onSaved` | `(contentlet: DotCMSContentlet) => void` | no | Called on every `saved` of the panel the host opens. The Relationship field treats it as an upsert by identifier: add the row once, refresh it on later saves |
| `source` | `'relationship' \| 'block-editor'` | no | Who asked. Only a Relationship-field edit replaces in place inside a panel; anything else, or no source, stacks |
| `from` | `{ inode; title }` | no | The content the field is showing, for the in-place breadcrumb. The field computes it (an unsaved translation names its source version). Absent → fresh trail |

Host decision:

| Host | Existing item (`mode: 'edit'`) | New item (`mode: 'new'`) |
|---|---|---|
| Full-screen route (`RouterEditContentHost`) | opens a new panel | opens a new panel |
| A panel or dialog (`OverlayEditContentHost`) | replaces in place with a breadcrumb (`goToRelatedContent`), as today, when the request comes from a Relationship field; stacks a panel when it comes from a block editor | stacks a panel |

## Legacy screen request (`DotLegacyScreenRequest`)

What the generic legacy-screen panel loads.

| Field | Type | Required | Rules |
|---|---|---|---|
| `url` | string | yes | Must pass `isSameOriginRelativeUrl`; otherwise the panel renders nothing |
| `title` | string | yes | The panel heading |

Its single output, `legacyEvent`, carries the `ng-event` detail `{ name; payload?; data? }` unchanged.

## UVE insertion context (internal to the UVE)

Not a contract; recorded because the duplicate-insert rule depends on it.

| Field | Type | Rules |
|---|---|---|
| `actionPayload` | `ActionPayload` | Captured when a create panel opens from a palette drop or a container "+" |
| `inserted` | boolean | `false` at open. Set to `true` after the first `saved` inserts the contentlet. A later `saved` from the same panel only reloads the page |

## State transitions of one panel

```
requested ──(opener resolves editor, and create URL for a legacy create)──▶ opening (outlet spinner)
opening ──(component resolved)──▶ open
opening ──(lookup refused)──▶ error toast, closed
open ──(save)──▶ open, emits saved            (repeatable)
open ──(restore)──▶ open, emits saved
open ──(language switch)──▶ open, emits languageChanged
open ──(delete)──▶ emits deleted, then closed
open ──(first save of a new page, legacy)──▶ emits pageEditorRequested; the opener closes the panel and opens the page editor
open ──(X, Escape, mask, routed Back; form clean)──▶ closed
open ──(X, Escape, mask, routed Back; form dirty)──▶ prompt ──keep──▶ open
                                                     prompt ──discard──▶ closed
open ──(field or block editor asks for content)──▶ open, plus a second panel stacked in front
open ──(opener's injector destroyed)──▶ closed, unguarded (existing outlet behaviour)
```
