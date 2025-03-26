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
     * Request the bounds for the elements
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
    UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS = 'uve-copy-contentlet-inline-editing-success'
}
