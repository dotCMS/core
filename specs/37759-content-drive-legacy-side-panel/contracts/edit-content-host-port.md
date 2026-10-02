# Contract: `EditContentHost` port additions (`@dotcms/edit-content`)

File: `core-web/libs/edit-content/src/lib/services/host/edit-content-host.model.ts`. These keep the
two new-editor paths that leave for Content Search (FR-028, FR-029) inside the side panel, without
changing the full-page new editor. They follow the existing `leaveDeletedContent` pattern.

## New methods

| Method | Called from | `RouterEditContentHost` (full page) | `OverlayEditContentHost` (panel, dialog) |
|---|---|---|---|
| `switchToLegacyEditor(contentlet: DotCMSContentlet): void` | `content.feature.ts`, `disableNewContentEditor`, after `CONTENT_EDITOR2_ENABLED: false` is saved (replaces `router.navigate(['/c/content/', inode])`, `:438`) | `router.navigate(['/c/content/', contentlet.inode])`, as today | emits on `switchedToLegacy$` |
| `leaveOnLoadError(): void` | `content.feature.ts`, `initializeExistingContent` error branch, after `dotHttpErrorManagerService.handle(error)` (replaces `router.navigate(['/c/content'])`, `:401`) | `router.navigate(['/c/content'])`, as today | emits on `loadFailed$` |

## Overlay consumers

| Consumer | `switchedToLegacy$` | `loadFailed$` |
|---|---|---|
| `DotEditContentSidePanelComponent` | new output `switchedToLegacyEditor: DotCMSContentlet` | emits `closed` (no unsaved prompt: nothing loaded) |
| `DotCreateContentDialogComponent` | keeps today's navigation to `/c/content/<inode>` | keeps today's navigation to `/c/content` |

## Identity

`EditContentIdentity` gains `languageId?: number` ([data-model.md §7](../data-model.md)).
`OverlayEditContentHost.resolveIdentity()` fills it from `EditContentDialogData.languageId`.
`RouterEditContentHost` leaves it unset.
