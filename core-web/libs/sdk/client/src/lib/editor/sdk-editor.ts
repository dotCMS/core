import {
    listenEditorMessages,
    listenHoveredContentlet,
    pingEditor,
    scrollHandler,
    setPageEditorConfig,
    subscriptions
} from './listeners/listeners';
import { CUSTOMER_ACTIONS, postMessageToEditor } from './models/client.model';
import { DotCMSPageEditorConfig } from './models/editor.model';

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
 * Checks if the code is running inside an editor.
 * @returns {boolean} Returns true if the code is running inside an editor, otherwise false.
 */
export function isInsideEditor() {
    if (typeof window === 'undefined') {
        return false;
    }

    return window.parent !== window;
}

/**
 * Initializes the DotCMS page editor.
 *
 * @param conf - Optional configuration for the editor.
 */
export function initEditor(config?: DotCMSPageEditorConfig) {
    if (config) {
        setPageEditorConfig(config);
    }

    pingEditor();
    listenEditorMessages();
    listenHoveredContentlet();
    scrollHandler();
}

/**
 * Destroys the editor by removing event listeners and disconnecting observers.
 */
export function destroyEditor() {
    subscriptions.forEach((subscription) => {
        if (subscription.type === 'listener') {
            window.removeEventListener(subscription.event, subscription.callback as EventListener);
        }

        if (subscription.type === 'observer') {
            subscription.observer.disconnect();
        }
    });
}
