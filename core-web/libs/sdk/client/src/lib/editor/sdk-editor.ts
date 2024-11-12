import {
    fetchPageDataFromInsideUVE,
    listenEditorMessages,
    listenHoveredContentlet,
    scrollHandler,
    subscriptions
} from './listeners/listeners';
import { CLIENT_ACTIONS, INITIAL_DOT_UVE, postMessageToEditor } from './models/client.model';
import { DotCMSPageEditorConfig } from './models/editor.model';

import { Contentlet } from '../client/content/shared/types';

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
        action: CLIENT_ACTIONS.NAVIGATION_UPDATE,
        payload: {
            url: pathname === '/' ? 'index' : pathname?.replace('/', '')
        }
    });
}

/**
 * You can use this function to edit a contentlet in the editor.
 *
 * Calling this function inside the editor, will prompt the UVE to open a dialog to edit the contentlet.
 *
 * @export
 * @template T
 * @param {Contentlet<T>} contentlet - The contentlet to edit.
 */
export function editContentlet<T>(contentlet: Contentlet<T>) {
    postMessageToEditor({
        action: CLIENT_ACTIONS.EDIT_CONTENTLET,
        payload: contentlet
    });
}

/**
 * Reorders the menu starting from a specified level.
 *
 * @param {number} [startLevel=1] - The level from which to start reordering the menu. Defaults to 1.
 *
 * This function constructs a URL for the reorder menu page with the given start level and a fixed depth of 2.
 * It then sends a message to the editor with the action to reorder the menu and the constructed URL as the payload.
 */
export function reorderMenu(startLevel = 1) {
    // This is the URL for the reorder menu page
    // All params are hardcoded on the jsp, so here we just need to send the same URL
    const reorderUrl = `/c/portal/layout?p_l_id=2df9f117-b140-44bf-93d7-5b10a36fb7f9&p_p_id=site-browser&p_p_action=1&p_p_state=maximized&_site_browser_struts_action=%2Fext%2Ffolders%2Forder_menu&startLevel=${startLevel}&depth=2`;

    postMessageToEditor({
        action: CLIENT_ACTIONS.REORDER_MENU,
        payload: { reorderUrl }
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

export function initDotUVE() {
    window.dotUVE = INITIAL_DOT_UVE;
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
    initDotUVE();
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
