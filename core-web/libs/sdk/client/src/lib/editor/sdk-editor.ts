import {
    fetchPageDataFromInsideUVE,
    listenEditorMessages,
    listenHoveredContentlet,
    scrollHandler,
    subscriptions
} from './listeners/listeners';
import { CLIENT_ACTIONS, INITIAL_DOT_UVE, postMessageToEditor } from './models/client.model';
import { DotCMSPageEditorConfig, ReorderMenuConfig } from './models/editor.model';
import { INLINE_EDITING_EVENT_KEY, InlineEditEventData } from './models/inline-event.model';

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
 * Initializes the inline editing in the editor.
 *
 * @export
 * @param {INLINE_EDITING_EVENT_KEY} type
 * @param {InlineEditEventData} eventData
 * @return {*}
 *
 *  * @example
 * ```html
 * <div onclick="initInlineEditing('BLOCK_EDITOR', { inode, languageId, contentType, fieldName, content })">
 *      ${My Content}
 * </div>
 * ```
 */
export function initInlineEditing(
    type: INLINE_EDITING_EVENT_KEY,
    data?: InlineEditEventData
): void {
    postMessageToEditor({
        action: CLIENT_ACTIONS.INIT_INLINE_EDITING,
        payload: {
            type,
            data
        }
    });
}

/*
 * Reorders the menu based on the provided configuration.
 *
 * @param {ReorderMenuConfig} [config] - Optional configuration for reordering the menu.
 * @param {number} [config.startLevel=1] - The starting level of the menu to reorder.
 * @param {number} [config.depth=2] - The depth of the menu to reorder.
 *
 * This function constructs a URL for the reorder menu page with the specified
 * start level and depth, and sends a message to the editor to perform the reorder action.
 */
export function reorderMenu(config?: ReorderMenuConfig): void {
    const { startLevel = 1, depth = 2 } = config || {};

    postMessageToEditor({
        action: CLIENT_ACTIONS.REORDER_MENU,
        payload: { startLevel, depth }
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
