type DotCMSMessageListenerCallback = (event: MessageEvent) => void;
type DotCMSPointerListenerCallback = (event: PointerEvent) => void;

/**
 * Listener for the dotcms editor
 *
 * @interface DotCMSPageEditorListener
 */
interface DotCMSPageEditorListener {
    type: 'listener';
    event: string;
    callback: DotCMSMessageListenerCallback | DotCMSPointerListenerCallback;
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
