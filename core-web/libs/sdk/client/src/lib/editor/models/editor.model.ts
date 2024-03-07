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
    EMA_EDITOR_PONG = 'ema-editor-pong'
}

export interface DotCMSPageEditorConfig {
    onReload: () => void;
    pathname?: string;
}

interface DotCMSPageEditorListener {
    type: 'listener';
    event: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    callback: (ev: any) => void;
}

interface DotCMSPageEditorObserver {
    type: 'observer';
    observer: MutationObserver;
}

export type DotCMSPageEditorSubscription = DotCMSPageEditorListener | DotCMSPageEditorObserver;
