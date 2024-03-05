/**
 * Actions received from the dotcms editor
 *
 * @export
 * @enum {number}
 */
export declare enum NOTIFY_CUSTOMER {
    /**
     * Request to page to reload
     */
    EMA_RELOAD_PAGE = "ema-reload-page",
    /**
     * Request the bounds for the elements
     */
    EMA_REQUEST_BOUNDS = "ema-request-bounds",
    /**
     * Received pong from the editor
     */
    EMA_EDITOR_PONG = "ema-editor-pong"
}
