/**
 * Actions received from the dotcms editor
 *
 * @export
 * @enum {number}
 */
export enum NOTIFY_CUSTOMER {
    /**
     * Request to page to reload
     */
    EMA_RELOAD_PAGE = 'ema-reload-page',
    /**
     * Request the bounds for the elements
     */
    EMA_REQUEST_BOUNDS = 'ema-request-bounds',
    /**
     * Received pong from the editor
     */
    EMA_EDITOR_PONG = 'ema-editor-pong',
    /**
     * Received scroll event trigger from the editor
     */
    EMA_SCROLL_INSIDE_IFRAME = 'scroll-inside-iframe'
}

type ListenerCallbackMessage = (event: MessageEvent) => void;
type ListenerCallbackPointer = (event: PointerEvent) => void;

/**
 * Listener for the dotcms editor
 *
 * @interface DotCMSPageEditorListener
 */
interface DotCMSPageEditorListener {
    type: 'listener';
    event: string;
    callback: ListenerCallbackMessage | ListenerCallbackPointer;
}

/**
 * Observer for the dotcms editor
 *
 * @interface DotCMSPageEditorObserver
 */
interface DotCMSPageEditorObserver {
    type: 'observer';
    observer: MutationObserver;
}

export type DotCMSPageEditorSubscription = DotCMSPageEditorListener | DotCMSPageEditorObserver;
