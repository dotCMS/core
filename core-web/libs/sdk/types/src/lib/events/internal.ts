/**
 * Actions received from the dotcms editor
 *
 * @export
 * @enum {number}
 */
export enum __DOTCMS_UVE_EVENT__ {
    /**
     * Request to page to reload
     */
    UVE_RELOAD_PAGE = 'uve-reload-page',
    /**
     * Tell the SDK to emit page bounds immediately, bypassing the
     * auto-bounds debounce. Used when the editor needs a synchronous
     * snapshot of bounds for an interaction (drag/drop dropzone) and
     * cannot wait the ~100ms trailing edge.
     */
    UVE_FLUSH_BOUNDS = 'uve-flush-bounds',
    /**
     * @deprecated Use UVE_FLUSH_BOUNDS instead. Kept for one release so the
     * editor can dual-emit and SDKs in the wild that still listen for the
     * legacy `uve-request-bounds` event continue to receive flushes. Drop
     * after the next minor release of @dotcms/uve.
     */
    UVE_REQUEST_BOUNDS = 'uve-request-bounds',
    /**
     * Received pong from the editor
     */
    UVE_EDITOR_PONG = 'uve-editor-pong',
    /**
     * Received scroll event trigger from the editor
     */
    UVE_SCROLL_INSIDE_IFRAME = 'uve-scroll-inside-iframe',
    /**
     * TODO:
     * Set the page data - This is used to catch the "changes" event.
     * We must to re-check the name late.
     */
    UVE_SET_PAGE_DATA = 'uve-set-page-data',
    /**
     * Copy contentlet inline editing success
     */
    UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS = 'uve-copy-contentlet-inline-editing-success',
    /**
     * Request the iframe to scroll to a section by 1-based row index
     */
    UVE_SCROLL_TO_SECTION = 'uve-scroll-to-section',
    /**
     * The editor cleared its contentlet selection (e.g. after a canvas resize
     * or scroll). The SDK uses this to reset its "last selected" tracker so a
     * subsequent click on the same contentlet re-emits CONTENTLET_CLICKED
     * instead of being treated as a passthrough.
     */
    UVE_SELECTION_CLEARED = 'uve-selection-cleared'
}
