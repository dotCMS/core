/**
 * Actions received from the dotcms editor
 *
 * @export
 * @enum {number}
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
