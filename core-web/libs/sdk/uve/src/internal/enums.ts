/**
 * Actions received from the dotCMS UVE
 *
 * @export
 * @enum {string}
 */
export enum NOTIFY_CLIENT {
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
     * Set the page data
     */
    UVE_SET_PAGE_DATA = 'uve-set-page-data',
    /**
     * Copy contentlet inline editing success
     */
    UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS = 'uve-copy-contentlet-inline-editing-success'
}
