/**
 * What a surface asks to open (#38303). It names content, never an editor: the opener
 * (`DotEditContentOpenerService`) picks the editor from the content type. It carries no callbacks
 * and no URLs; the panels report back through their outputs, and the opener resolves any server
 * URL itself.
 */
export interface DotEditContentRequest {
    /** Edit an existing contentlet, or create one of a content type. */
    mode: 'edit' | 'new';

    /**
     * The working version to edit. An `edit` names it, or names `identifier` instead (a URL names
     * content that way): the opener then finds the working version in `languageId`, or in any
     * language when there is none in that one.
     */
    inode?: string;

    /**
     * The contentlet identifier. Optional when `inode` is given; the opener fills it from the
     * fetched contentlet.
     */
    identifier?: string;

    /**
     * The content type variable. Required for `new`. Optional for `edit`: when absent the opener
     * fetches the contentlet by inode to learn its type (the legacy `edit-host` event sends only an
     * inode).
     */
    contentType?: string;

    /** The language to edit in or create in. Defaults to the surface's current language. */
    languageId?: number;

    /** Pre-selects the folder in the new editor's create form. */
    folderPath?: string;

    /** Pre-selects the folder in the legacy create screen (`folder=<inode>`). */
    folderInode?: string;

    /**
     * The panel heading. Falls back to the content type name (create) or the contentlet title
     * (edit).
     */
    title?: string;

    /** The experiment variant to edit or create in. Absent means the default variant. */
    variantName?: string;

    /** The site the request belongs to (`host_id` on the legacy edit URL). */
    siteId?: string;

    /**
     * The admin portlet the request comes from, sent to the legacy editor as
     * `angularCurrentPortlet`. Defaults to `'edit-page'` in the UVE and `'content'` elsewhere.
     */
    portletId?: string;

    /** Relationship depth for the new editor's initial load. */
    depth?: number;

    /**
     * Present only when a Relationship field asked for a create; the reply needs it to place the
     * new item.
     */
    relationship?: {
        parentContentletId: string;
        relationshipName: string;
        isParent: boolean;
    };
}
