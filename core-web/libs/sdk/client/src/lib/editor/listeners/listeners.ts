import { CUSTOMER_ACTIONS, postMessageToEditor } from '../models/client.model';
import { DotCMSPageEditorConfig } from '../models/editor.model';
import { DotCMSPageEditorSubscription, NOTIFY_CUSTOMER } from '../models/listeners.model';
import {
    findVTLData,
    findDotElement,
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

        if (event.data.name === 'scroll-inside-iframe') {
            const scrollY = event.data.direction === 'up' ? -120 : 120;
            window.scrollBy({ left: 0, top: scrollY, behavior: 'smooth' });
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
        const foundElement = findDotElement(event.target as HTMLElement);

        if (!foundElement) return;

        const { x, y, width, height } = foundElement.getBoundingClientRect();

        const isContainer = foundElement.dataset?.['dotObject'] === 'container';

        const contentletForEmptyContainer = {
            identifier: 'TEMP_EMPTY_CONTENTLET',
            title: 'TEMP_EMPTY_CONTENTLET',
            contentType: 'TEMP_EMPTY_CONTENTLET_TYPE',
            inode: 'TEMPY_EMPTY_CONTENTLET_INODE',
            widgetTitle: 'TEMP_EMPTY_CONTENTLET',
            baseType: 'TEMP_EMPTY_CONTENTLET',
            onNumberOfPages: 1
        };

        const contentlet = {
            identifier: foundElement.dataset?.['dotIdentifier'],
            title: foundElement.dataset?.['dotTitle'],
            inode: foundElement.dataset?.['dotInode'],
            contentType: foundElement.dataset?.['dotType'],
            baseType: foundElement.dataset?.['dotBasetype'],
            widgetTitle: foundElement.dataset?.['dotWidgetTitle'],
            onNumberOfPages: foundElement.dataset?.['dotOnNumberOfPages']
        };

        const vtlFiles = findVTLData(foundElement);
        const contentletPayload = {
            container:
                // Here extract dot-container from contentlet if is Headless
                // or search in parent container if is VTL
                foundElement.dataset?.['dotContainer']
                    ? JSON.parse(foundElement.dataset?.['dotContainer'])
                    : getClosestContainerData(foundElement),
            contentlet: isContainer ? contentletForEmptyContainer : contentlet,
            vtlFiles
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

    const scrollEndCallback = () => {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL_END
        });
    };

    window.addEventListener('scroll', scrollCallback);
    window.addEventListener('scrollend', scrollEndCallback);

    subscriptions.push({
        type: 'listener',
        event: 'scroll',
        callback: scrollEndCallback
    });

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
 * Sends a ping message to the editor.
 *
 */
export function pingEditor() {
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.PING_EDITOR
    });
}
