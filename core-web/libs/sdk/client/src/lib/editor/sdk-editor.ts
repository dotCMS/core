import {
    fetchPageDataFromInsideUVE,
    listenEditorMessages,
    listenHoveredContentlet,
    scrollHandler,
    subscriptions
} from './listeners/listeners';
import { CUSTOMER_ACTIONS, postMessageToEditor } from './models/client.model';
import { DotCMSPageEditorConfig } from './models/editor.model';

/**
 * Updates the navigation in the editor.
 *
 * @param {string} pathname - The pathname to update the navigation with.
 * @memberof DotCMSPageEditor
 * @example
 * updateNavigation('/home'); // Sends a message to the editor to update the navigation to '/home'
 */
export function updateNavigation(pathname: string): void {
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
        payload: {
            url: pathname === '/' ? 'index' : pathname?.replace('/', '')
        }
    });
}

/**
 * Checks if the code is running inside an editor.
 *
 * @returns {boolean} Returns true if the code is running inside an editor, otherwise false.
 * @example
 * ```ts
 * if (isInsideEditor()) {
 *     console.log('Running inside the editor');
 * } else {
 *     console.log('Running outside the editor');
 * }
 * ```
 */
export function isInsideEditor(): boolean {
    if (typeof window === 'undefined') {
        return false;
    }

    return window.parent !== window;
}

/**
 * Initializes the DotCMS page editor.
 *
 * @param {DotCMSPageEditorConfig} config - Optional configuration for the editor.
 * @example
 * ```ts
 * const config = { pathname: '/home' };
 * initEditor(config); // Initializes the editor with the provided configuration
 * ```
 */
export function initEditor(config: DotCMSPageEditorConfig): void {
    fetchPageDataFromInsideUVE(config.pathname);
    listenEditorMessages();
    listenHoveredContentlet();
    scrollHandler();
}

/**
 * Destroys the editor by removing event listeners and disconnecting observers.
 *
 * @example
 * ```ts
 * destroyEditor(); // Cleans up the editor by removing all event listeners and disconnecting observers
 * ```
 */
export function destroyEditor(): void {
    subscriptions.forEach((subscription) => {
        if (subscription.type === 'listener') {
            window.removeEventListener(subscription.event, subscription.callback as EventListener);
        }

        if (subscription.type === 'observer') {
            subscription.observer.disconnect();
        }
    });
}

/**
 * Adds a style class to empty contentlets.
 *
 * @export
 * @example
 * ```ts
 * addClassToEmptyContentlets(); // Adds the 'empty-contentlet' class to all contentlets that have no height
 * ```
 */
export function addClassToEmptyContentlets(): void {
    const contentlets = document.querySelectorAll('[data-dot-object="contentlet"]');

    contentlets.forEach((contentlet) => {
        if (contentlet.clientHeight) {
            return;
        }

        contentlet.classList.add('empty-contentlet');
    });
}
