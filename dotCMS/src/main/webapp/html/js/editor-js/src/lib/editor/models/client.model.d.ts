/**
 * Actions send to the dotcms editor
 *
 * @export
 * @enum {number}
 */
export declare enum CUSTOMER_ACTIONS {
    /**
     * Tell the dotcms editor that page change
     */
    NAVIGATION_UPDATE = "set-url",
    /**
     * Send the element position of the rows, columnsm containers and contentlets
     */
    SET_BOUNDS = "set-bounds",
    /**
     * Send the information of the hovered contentlet
     */
    SET_CONTENTLET = "set-contentlet",
    /**
     * Tell the editor that the page is being scrolled
     */
    IFRAME_SCROLL = "scroll",
    /**
     * Ping the editor to see if the page is inside the editor
     */
    PING_EDITOR = "ping-editor",
    CONTENT_CHANGE = "content-change",
    NOOP = "noop"
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
export declare function postMessageToEditor<T = unknown>(message: PostMessageProps<T>): void;
export {};
