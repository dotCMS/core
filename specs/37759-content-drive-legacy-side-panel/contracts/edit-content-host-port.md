# Contract: Edit Content changes for Content Drive's side panel (`@dotcms/edit-content`)

Only Content Drive changes how the new editor leaves for the legacy editor or after a load error
(FR-028, FR-029). Every other opener of the new editor (the Edit Content portlet, Query Tool, UVE,
the relationship field, the asset picker) keeps the navigation it has on `main`. The language work
below applies to every overlay, but only Content Drive listens to it.

## Navigation override

File: `core-web/libs/edit-content/src/lib/models/edit-content-navigation-override.ts`, exported from
the lib index. Remove with the legacy editor.

```ts
export interface EditContentNavigationOverride {
    switchToLegacyEditor(
        opened: EditContentIdentity,
        contentlet: DotCMSContentlet | null,
        contentTypeVariable: string
    ): boolean;
    leaveOnLoadError(opened: EditContentIdentity): boolean;
}
export const EDIT_CONTENT_NAVIGATION_OVERRIDE: InjectionToken<EditContentNavigationOverride>;
```

`content.feature.ts` injects it with `{ optional: true }` and keeps `main`'s navigation:

| Path | Override asked first | When there is no override, or it returns `false` (as on `main`) |
|---|---|---|
| `disableNewContentEditor`, after `CONTENT_EDITOR2_ENABLED: false` is saved | `switchToLegacyEditor(host.resolveIdentity(), contentlet ?? null, contentType.variable)` | `router.navigate(['/c/content/', contentlet.inode])` |
| `initializeExistingContent` error, after `dotHttpErrorManagerService.handle(error)` | `leaveOnLoadError(host.resolveIdentity())` | `router.navigate(['/c/content'])` |

`opened` is what the editor was opened with (`EditContentHost.resolveIdentity()`). It stays the
same across the editor's in-place reloads (a language switch), so an opener can tell its own editor
from any other one. The override is provided on an ancestor component, so every editor below it
inherits it, nested ones included: a related content opened from a relationship field (in a side
panel through `ViewContainerRef`, or in a dialog through the side panel's `DialogService`). The
override returns `false` for those, and they navigate as on `main`.

`main`'s crash on a create that was never saved (`contentlet.inode` with no content) is unchanged
outside Content Drive: out of scope.

### Content Drive's implementation

`DotContentDriveNavigationService` implements the interface, and the shell provides the token with
`provideContentDriveNavigationOverride()` (`useExisting` the service). It answers only for the
new-editor panel it has open: the same `contentletInode` for an edit, the same `contentTypeId` with
no inode for a create.

- Switch, edit: the content the editor shows reopens, in its language (`contentlet.languageId`),
  in the legacy panel. The URL already names it, unless the panel moved in place to related
  content: then the URL follows the switched content (`replaceState`). A save follows a new
  language only when it names the content the URL names.
- Switch, create: a legacy create for the same type, in the folder being browsed and the language
  the create started in (`#openLegacyCreate`).
- Load error: `closeEditPanel()`; the folder, filters and page stay.

## Language of an in-place reload

`reloadContent(inode: string, languageId?: number): void`. The locale switch in `locales.feature.ts`
(`switchLocale`) passes the language of the version it loads. `RouterEditContentHost` ignores it:
its route change already names the version. `OverlayEditContentHost` reloads in place, as before,
and carries the language on the `InPlaceNavigationRequest` (`languageId`). The layout reports it
through the optional `reportLanguage?(languageId)` once the reload actually runs, after the
unsaved-changes prompt, so a "keep editing" reports nothing. The overlay emits it on
`languageChanged$`, the side panel emits it as its `languageChanged` output, and Content Drive
routes it to `panelLanguageChanged`, so a refresh reopens that language (FR-020). Full-screen hosts
don't implement `reportLanguage`.

## Identity

`EditContentIdentity` gains `languageId?: number` ([data-model.md §7](../data-model.md)).
`OverlayEditContentHost.resolveIdentity()` fills it from `EditContentDialogData.languageId`.
`RouterEditContentHost` leaves it unset.
