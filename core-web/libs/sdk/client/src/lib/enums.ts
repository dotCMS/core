/**
 * Actions send to the dotcms editor
 *
 * @export
 * @enum {number}
 */
export enum CUSTOMER_ACTIONS {
    /**
     * Tell the dotcms editor that page change
     */
    SET_URL = 'set-url',
    /**
     * Send the element position of the rows, columnsm containers and contentlets
     */
    SET_BOUNDS = 'set-bounds',
    /**
     * Send the information of the hovered contentlet
     */
    SET_CONTENTLET = 'set-contentlet',
    /**
     * Tell the editor that the page is being scrolled
     */
    IFRAME_SCROLL = 'scroll',
    NOOP = 'noop'
}
