/**
 * What the legacy-editor side panel is asked to open (#37759).
 *
 * `edit` without `inode`, or `new` without `contentTypeVariable`, opens nothing. A built URL that is
 * not same-origin relative is refused (`isSameOriginRelativeUrl`).
 */
export interface DotLegacyEditorRequest {
    mode: 'edit' | 'new';
    /** Working version to edit, in the requested language. Required for `edit`. */
    inode?: string;
    /** Identifier of the content being edited. Reported back with the panel's outputs. */
    identifier?: string;
    /** Content type to create. Required for `new`. */
    contentTypeVariable?: string;
    /**
     * Folder the new content goes in, appended as `folder=<inode>` so the legacy editor pre-selects
     * it. System Host passes its identifier. `new` only.
     */
    folderInode?: string;
    /** `edit`: the version's language. `new`: the list's language filter, else the default. */
    languageId: number;
    /** Drawer header: the content's title, or the type's name for a create. */
    title: string;
    /** Sent to the legacy editor as `angularCurrentPortlet`. Content Drive passes `content-drive`. */
    portletId: string;
}

/** What the panel reports when the legacy editor saved (`save-page`). */
export interface DotLegacyEditorSaved {
    identifier: string;
    inode: string;
    languageId: number;
}

/** What the panel reports when the legacy editor asks to open a page (first save of a new page). */
export interface DotLegacyEditorPageRequest {
    url: string;
    languageId: number;
}
