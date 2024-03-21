import { CUSTOMER_ACTIONS, postMessageToEditor } from '../models/client.model';
import { DotCMSPageEditorConfig } from '../models/editor.model';
import { DotCMSPageEditorSubscription, NOTIFY_CUSTOMER } from '../models/listeners.model';
import {
    findContentletElement,
    getClosestContainerData,
    getPageElementBound
} from '../utils/editor.utils';

declare global {
    interface Window {
        lastScrollYPosition: number;
    }
}

/**
 * Default reload function that reloads the current window.
 */
const defaultReloadFn = () => window.location.reload();

/**
 * Configuration object for the DotCMSPageEditor.
 */
let pageEditorConfig: DotCMSPageEditorConfig = {
    onReload: defaultReloadFn
};

export function setPageEditorConfig(config: DotCMSPageEditorConfig) {
    pageEditorConfig = config;
}

/**
 * Represents an array of DotCMSPageEditorSubscription objects.
 * Used to store the subscriptions for the editor and unsubscribe later.
 */
export const subscriptions: DotCMSPageEditorSubscription[] = [];

/**
 * Sets the bounds of the containers in the editor.
 * Retrieves the containers from the DOM and sends their position data to the editor.
 * @private
 * @memberof DotCMSPageEditor
 */
function setBounds() {
    const containers = Array.from(
        document.querySelectorAll('[data-dot-object="container"]')
    ) as HTMLDivElement[];
    const positionData = getPageElementBound(containers);

    postMessageToEditor({
        action: CUSTOMER_ACTIONS.SET_BOUNDS,
        payload: positionData
    });
}

/**
 * Reloads the page and triggers the onReload callback if it exists in the config object.
 */
function reloadPage() {
    pageEditorConfig?.onReload();
}

/**
 * Listens for editor messages and performs corresponding actions based on the received message.
 *
 * @private
 * @memberof DotCMSPageEditor
 */
export function listenEditorMessages() {
    const messageCallback = (event: MessageEvent) => {
        switch (event.data) {
            case NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS: {
                setBounds();
                break;
            }

            case NOTIFY_CUSTOMER.EMA_RELOAD_PAGE: {
                reloadPage();
                break;
            }
        }
    };

    window.addEventListener('message', messageCallback);

    subscriptions.push({
        type: 'listener',
        event: 'message',
        callback: messageCallback
    });
}

/**
 * Listens for pointer move events and extracts information about the hovered contentlet.
 *
 * @private
 * @memberof DotCMSPageEditor
 */
export function listenHoveredContentlet() {
    const pointerMoveCallback = (event: PointerEvent) => {
        const target = findContentletElement(event.target as HTMLElement);
        if (!target) return;
        const { x, y, width, height } = target.getBoundingClientRect();

        const contentletPayload = {
            container:
                // Here extract dot-container from contentlet if is Headless
                // or search in parent container if is VTL
                target.dataset?.['dotContainer']
                    ? JSON.parse(target.dataset?.['dotContainer'])
                    : getClosestContainerData(target),
            contentlet: {
                identifier: target.dataset?.['dotIdentifier'],
                title: target.dataset?.['dotTitle'],
                inode: target.dataset?.['dotInode'],
                contentType: target.dataset?.['dotType'],
                onNumberOfPages: target.dataset?.['dotOnNumberOfPages']
            }
        };

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
            payload: {
                x,
                y,
                width,
                height,
                payload: contentletPayload
            }
        });
    };

    document.addEventListener('pointermove', pointerMoveCallback);

    subscriptions.push({
        type: 'listener',
        event: 'pointermove',
        callback: pointerMoveCallback
    });
}

/**
 * Attaches a scroll event listener to the window
 * and sends a message to the editor when the window is scrolled.
 *
 * @private
 * @memberof DotCMSPageEditor
 */
export function scrollHandler() {
    const scrollCallback = () => {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL
        });
        window.lastScrollYPosition = window.scrollY;
    };

    window.addEventListener('scroll', scrollCallback);

    subscriptions.push({
        type: 'listener',
        event: 'scroll',
        callback: scrollCallback
    });
}

/**
 * Restores the scroll position of the window when an iframe is loaded.
 * Only used in VTL Pages.
 * @export
 */
export function preserveScrollOnIframe() {
    const preserveScrollCallback = () => {
        window.scrollTo(0, window.lastScrollYPosition);
    };

    window.addEventListener('load', preserveScrollCallback);
    subscriptions.push({
        type: 'listener',
        event: 'scroll',
        callback: preserveScrollCallback
    });
}

/**
 * Listens for changes in the content and triggers a customer action when the content changes.
 *
 * @private
 * @memberof DotCMSPageEditor
 */
export function listenContentChange() {
    const observer = new MutationObserver((mutationsList) => {
        for (const { addedNodes, removedNodes, type } of mutationsList) {
            if (type === 'childList') {
                const didNodesChanged = [
                    ...Array.from(addedNodes),
                    ...Array.from(removedNodes)
                ].filter(
                    (node) => (node as HTMLDivElement).dataset?.['dotObject'] === 'contentlet'
                ).length;

                if (didNodesChanged) {
                    postMessageToEditor({
                        action: CUSTOMER_ACTIONS.CONTENT_CHANGE
                    });
                }
            }
        }
    });

    observer.observe(document, { childList: true, subtree: true });

    subscriptions.push({
        type: 'observer',
        observer
    });
}

/**
 * Sends a ping message to the editor.
 *
 */
export function pingEditor() {
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.PING_EDITOR
    });
}
