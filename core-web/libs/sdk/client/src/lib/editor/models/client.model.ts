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
