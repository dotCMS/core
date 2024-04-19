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
    NAVIGATION_UPDATE = 'set-url',
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
    /**
     * Ping the editor to see if the page is inside the editor
     */
    PING_EDITOR = 'ping-editor',
    /**
     * Tell the editor to init the inline editing editor.
     */
    INIT_INLINE_EDITING = 'init-inline-editing',
    /**
     * Tell the editor to open the Copy-contentlet dialog
     * To copy a content and then edit it inline.
     */
    COPY_CONTENTLET_INLINE_EDITING = 'copy-contentlet-inline-editing',
    /**
     * Tell the editor to save inline edited contentlet
     */
    UPDATE_CONTENTLET_INLINE_EDITING = 'update-contentlet-inline-editing',

    /**
     * Tell the editor to trigger a menu reorder
     */
    REORDER_MENU = 'reorder-menu',

    NOOP = 'noop'
}

/**
 * Post message props
 *
 * @export
 * @template T
 * @interface PostMessageProps
 */
type PostMessageProps<T> = {
    action: CUSTOMER_ACTIONS;
    payload?: T;
};

/**
 * Post message to dotcms page editor
 *
 * @export
 * @template T
 * @param {PostMessageProps<T>} message
 */
export function postMessageToEditor<T = unknown>(message: PostMessageProps<T>) {
    window.parent.postMessage(message, '*');
}
