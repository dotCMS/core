import { CUSTOMER_ACTIONS, postMessageToEditor } from './models/client.model';
import {
    DotCMSPageEditorConfig,
    DotCMSPageEditorSubscription,
    NOTIFY_CUSTOMER
} from './models/editor.model';
import {
    findContentletElement,
    getClosestContainerData,
    getPageElementBound
} from './utils/editor.utils';

/**
 * Default reload function that reloads the current window.
 */
const defaultReloadFn = () => window.location.reload();

/**
 * Represents an array of DotCMSPageEditorSubscription objects.
 * Used to store the subscriptions for the editor and unsubscribe later.
 */
const subscriptions: DotCMSPageEditorSubscription[] = [];

/**
 * Configuration object for the DotCMSPageEditor.
 */
let pageEditorConfig: DotCMSPageEditorConfig = {
    onReload: defaultReloadFn
};

/**
 *
 * Updates the navigation in the editor.
 * @param {string} pathname - The pathname to update the navigation with.
 * @memberof DotCMSPageEditor
 */
export function updateNavigation(pathname: string) {
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
        payload: {
            url: pathname === '/' ? 'index' : pathname?.replace('/', '')
        }
    });
}

/**
 * Sets the bounds of the containers in the editor.
 * Retrieves the containers from the DOM and sends their position data to the editor.
 * @private
 * @memberof DotCMSPageEditor
 */
function setBounds() {
    const containers = Array.from(
        document.querySelectorAll('[data-dot-object="container"]')
    ) as unknown as HTMLDivElement[];
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
function listenEditorMessages() {
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
function listenHoveredContentlet() {
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
                contentType: target.dataset?.['dotType']
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
 *
 *
 * @private
 * @memberof DotCMSPageEditor
 */
function scrollHandler() {
    const scrollCallback = () => {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL
        });
    };

    window.addEventListener('scroll', scrollCallback);

    subscriptions.push({
        type: 'listener',
        event: 'scroll',
        callback: scrollCallback
    });
}

/**
 * Listens for changes in the content and triggers a custom action when the content changes.
 *
 * @private
 * @memberof DotCMSPageEditor
 */
function listenContentChange() {
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

/**
 * Checks if the code is running inside an editor.
 * @returns {boolean} Returns true if the code is running inside an editor, otherwise false.
 */
export function isInsideEditor() {
    if (window?.parent === window) {
        return false;
    }

    return true;
}

/**
 * Initializes the DotCMS page editor.
 *
 * @param conf - Optional configuration for the editor.
 */
export function initEditor(config?: DotCMSPageEditorConfig) {
    if (config) {
        pageEditorConfig = config;
    }

    pingEditor();
    listenEditorMessages();
    listenHoveredContentlet();
    scrollHandler();
    listenContentChange();
}

/**
 * Destroys the editor by removing event listeners and disconnecting observers.
 */
export function destroyEditor() {
    subscriptions.forEach((subscription) => {
        if (subscription.type === 'listener') {
            window?.removeEventListener(subscription.event, subscription.callback);
        }

        if (subscription.type === 'observer') {
            subscription.observer.disconnect();
        }
    });
}
