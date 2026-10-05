# Contract: `EditContentHost` port additions (`@dotcms/edit-content`)

File: `core-web/libs/edit-content/src/lib/services/host/edit-content-host.model.ts`. These keep the
two new-editor paths that leave for Content Search (FR-028, FR-029) inside the side panel, without
changing the full-page new editor. They follow the existing `leaveDeletedContent` pattern.

## New methods

| Method | Called from | `RouterEditContentHost` (full page) | `OverlayEditContentHost` (panel, dialog) |
|---|---|---|---|
| `switchToLegacyEditor(contentlet: DotCMSContentlet \| null, contentTypeVariable: string): void` | `content.feature.ts`, `disableNewContentEditor`, after `CONTENT_EDITOR2_ENABLED: false` is saved (replaces `router.navigate(['/c/content/', inode])`, `:438`). `contentlet` is `null` for a create that was never saved | `router.navigate(['/c/content/', contentlet.inode])`, as today, or `['/c/content/new/', contentTypeVariable]` for a create | reloads the page (`DOCUMENT.defaultView.location.reload()`); no event |
| `leaveOnLoadError(): void` | `content.feature.ts`, `initializeExistingContent` error branch, after `dotHttpErrorManagerService.handle(error)` (replaces `router.navigate(['/c/content'])`, `:401`) | `router.navigate(['/c/content'])`, as today | emits on `loadFailed$` |

## Overlay consumers

| Consumer | `loadFailed$` | `languageChanged$` |
|---|---|---|
| `DotEditContentSidePanelComponent` | emits `closed` (no unsaved prompt: nothing loaded) | new output `languageChanged: number`; Content Drive routes it to `panelLanguageChanged` |
| `DotCreateContentDialogComponent` | keeps today's navigation to `/c/content` | not used |

## Changed method

`reloadContent(inode: string, languageId?: number): void`. The locale switch in `locales.feature.ts`
(`switchLocale`) passes the language of the version it loads. `RouterEditContentHost` ignores it:
its route change already names the version. `OverlayEditContentHost` reloads in place, as before,
and also emits the language on `languageChanged$`, so the opener's URL names the language now
open. That keeps a refresh, and the page reload of "switch to the old editor", in that language
(FR-020, FR-028).

"Switch to the old editor" reports nothing to the overlay's consumers (decided with the developer,
2026-10-05). The page reloads and whatever opened the overlay reopens from its own URL. Content
Drive's URL names the open content (`editContent` + `editContentLang`) or create (`createContent`
with `path`), so the reload reopens it in the legacy panel, now that the type uses the legacy
editor (FR-028). The other side-panel hosts (Query Tool, UVE, Experiments, relationship field)
reload their own page. Unsaved edits are protected by the `beforeunload` guards of the edit-content
layout and the Content Drive shell.

## Identity

`EditContentIdentity` gains `languageId?: number` ([data-model.md §7](../data-model.md)).
`OverlayEditContentHost.resolveIdentity()` fills it from `EditContentDialogData.languageId`.
`RouterEditContentHost` leaves it unset.
