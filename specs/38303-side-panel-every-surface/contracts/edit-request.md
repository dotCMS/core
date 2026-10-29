# Contract: the edit request

**Lives in**: `core-web/libs/edit-content/src/lib/models/dot-edit-content-request.model.ts` (new), exported from `libs/edit-content/src/index.ts`.

**Replaces**: `EditContentDialogData` (`libs/edit-content/src/lib/models/dot-edit-content-dialog.interface.ts`, deleted) and `DotLegacyEditorRequest` (`libs/portlets/dot-content-drive/portlet/src/lib/shared/legacy-editor.models.ts`, deleted).

```ts
import { DotCMSContentlet } from '@dotcms/dotcms-models';

/**
 * What a surface asks to open. It names content, never an editor: the opener picks the editor
 * from the content type (see opener-api.md). It carries no callbacks and no URLs.
 */
export interface DotEditContentRequest {
    /** Edit an existing contentlet, or create one of a content type. */
    mode: 'edit' | 'new';

    /** The working version to edit. Required for `edit`. */
    inode?: string;

    /** The contentlet identifier. Optional; the opener fills it from the fetched contentlet. */
    identifier?: string;

    /**
     * The content type variable. Required for `new`. Optional for `edit`: when absent the opener
     * fetches the contentlet by inode to learn its type (the legacy `edit-host` event sends only an inode).
     */
    contentType?: string;

    /** The language to edit in or create in. Defaults to the surface's current language. */
    languageId?: number;

    /** Pre-selects the folder in the new editor's create form. */
    folderPath?: string;

    /** Pre-selects the folder in the legacy create screen (`folder=<inode>`). */
    folderInode?: string;

    /** The panel heading. Falls back to the content type name (create) or the contentlet title (edit). */
    title?: string;

    /** The experiment variant to edit or create in. Absent means the default variant. */
    variantName?: string;

    /** The site the request belongs to (`host_id` on the legacy edit URL). */
    siteId?: string;

    /**
     * The admin portlet the request comes from, sent to the legacy editor as `angularCurrentPortlet`.
     * Defaults to `'edit-page'` in the UVE and `'content'` elsewhere.
     */
    portletId?: string;

    /** Relationship depth for the new editor's initial load, as today. */
    depth?: number;

    /** Present only when a Relationship field asked for a create; the reply needs it to place the new item. */
    relationship?: {
        parentContentletId: string;
        relationshipName: string;
        isParent: boolean;
    };
}

/** Narrowed shapes the opener produces after validation, so the panels never re-check. */
export type DotEditContentEditRequest = DotEditContentRequest & { mode: 'edit'; inode: string };
export type DotEditContentCreateRequest = DotEditContentRequest & { mode: 'new'; contentType: string };
```

## Validation (in the opener)

| Condition | Outcome |
|---|---|
| `mode === 'edit'` and no `inode` | Standard error message through `DotHttpErrorManagerService`; nothing opens |
| `mode === 'new'` and no `contentType` | Same |
| `mode === 'edit'`, no `contentType` | The opener fetches the contentlet by inode first (`DotContentletService.getContentletByInode`), then resolves the editor from its `contentType` |

## Who builds it

| Surface | Mode | Fields set |
|---|---|---|
| Content Drive | edit, new | `inode`, `identifier`, `contentType`, `languageId`, `title`, `folderPath`, `folderInode`, `portletId: 'content-drive'` |
| UVE: edit, properties, URL-map edit, SDK edit | edit | `inode`, `identifier`, `contentType`, `languageId`, `title`, `variantName`, `siteId`, `portletId: 'edit-page'` |
| UVE: palette drop, container "+", SDK create | new | `contentType`, `languageId`, `title`, `variantName`, `siteId`, `portletId: 'edit-page'` |
| Query Tool | edit | `inode`, `identifier`, `contentType`, `languageId`, `title` |
| Pages portlet | edit | `inode`, `identifier`, `contentType`, `languageId`, `title` |
| Legacy JSP events (`edit-contentlet`, `edit-host`) | edit | `inode`, `contentType` when sent |
| Legacy JSP events (`create-contentlet`, `create-host`) | new | `contentType` (`'Host'` for `create-host`), `folderPath` when sent. The JSP's `url` is ignored |
| Publishing Queue | edit | `inode`, `contentType` (`content_type_name`) |
| Deep links | edit, new | `inode` or `contentType` from the route |
| Relationship field | edit, new | `inode`, `title` or `contentType`, `title`, `relationship` |
| Block editors (via the field host or the legacy bridge) | edit | `inode`, `contentType` |
