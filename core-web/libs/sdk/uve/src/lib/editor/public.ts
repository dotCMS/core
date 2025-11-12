import {
    Contentlet,
    DotCMSUVEAction,
    DotCMSInlineEditingPayload,
    DotCMSInlineEditingType,
    DotCMSBasicContentlet,
    DotCMSPageResponse
} from '@dotcms/types'; // '../types/editor/public';
import { DotCMSReorderMenuConfig, DotCMSUVEMessage } from '@dotcms/types/internal'; //'../types/editor/internal';

import {
    addClassToEmptyContentlets,
    listenBlockEditorInlineEvent,
    registerUVEEvents,
    scrollHandler,
    setClientIsReady
} from '../../script/utils';

/**
 * Updates the navigation in the editor.
 *
 * @param {string} pathname - The pathname to update the navigation with.
 * @memberof DotCMSPageEditor
 * @example
 * updateNavigation('/home'); // Sends a message to the editor to update the navigation to '/home'
 */
export function updateNavigation(pathname: string): void {
    sendMessageToUVE({
        action: DotCMSUVEAction.NAVIGATION_UPDATE,
        payload: {
            url: pathname || '/'
        }
    });
}

/**
 * Post message to dotcms page editor
 *
 * @export
 * @template T
 * @param {DotCMSUVEMessage<T>} message
 */
export function sendMessageToUVE<T = unknown>(message: DotCMSUVEMessage<T>) {
    window.parent.postMessage(message, '*');
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
    sendMessageToUVE({
        action: DotCMSUVEAction.EDIT_CONTENTLET,
        payload: contentlet
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
export function reorderMenu(config?: DotCMSReorderMenuConfig): void {
    const { startLevel = 1, depth = 2 } = config || {};
    sendMessageToUVE({
        action: DotCMSUVEAction.REORDER_MENU,
        payload: { startLevel, depth }
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
    type: DotCMSInlineEditingType,
    data?: DotCMSInlineEditingPayload
): void {
    sendMessageToUVE({
        action: DotCMSUVEAction.INIT_INLINE_EDITING,
        payload: {
            type,
            data
        }
    });
}

/**
 * Initializes the block editor inline editing for a contentlet field.
 *
 * @example
 * ```html
 * <div onclick="enableBlockEditorInline(contentlet, 'MY_BLOCK_EDITOR_FIELD_VARIABLE')">
 *      ${My Content}
 * </div>
 * ```
 *
 * @export
 * @param {DotCMSBasicContentlet} contentlet
 * @param {string} fieldName
 * @return {*}  {void}
 */
export function enableBlockEditorInline<T extends DotCMSBasicContentlet>(
    contentlet: T extends DotCMSBasicContentlet ? T : never,
    fieldName: keyof T
): void {
    if (!contentlet?.[fieldName as keyof DotCMSBasicContentlet]) {
        console.error(
            `Contentlet ${contentlet?.identifier} does not have field ${fieldName as string}`
        );

        return;
    }

    const data = {
        fieldName: fieldName as string,
        inode: contentlet.inode,
        language: contentlet.languageId,
        contentType: contentlet.contentType,
        content: contentlet[fieldName] as Record<string, unknown>
    };

    initInlineEditing('BLOCK_EDITOR', data);
}

/**
 * Initializes the Universal Visual Editor (UVE) with required handlers and event listeners.
 *
 * This function sets up:
 * - Scroll handling
 * - Empty contentlet styling
 * - Block editor inline event listening
 * - Client ready state
 * - UVE event subscriptions
 *
 * @returns {Object} An object containing the cleanup function
 * @returns {Function} destroyUVESubscriptions - Function to clean up all UVE event subscriptions
 *
 * @example
 * ```typescript
 * const { destroyUVESubscriptions } = initUVE();
 *
 * // When done with UVE
 * destroyUVESubscriptions();
 * ```
 */
export function initUVE(config: DotCMSPageResponse = {} as DotCMSPageResponse): {
    destroyUVESubscriptions: () => void;
} {
    addClassToEmptyContentlets();
    setClientIsReady(config);

    const { subscriptions } = registerUVEEvents();
    const { destroyScrollHandler } = scrollHandler();
    const { destroyListenBlockEditorInlineEvent } = listenBlockEditorInlineEvent();

    return {
        destroyUVESubscriptions: () => {
            subscriptions.forEach((subscription) => subscription.unsubscribe());
            destroyScrollHandler();
            destroyListenBlockEditorInlineEvent();
        }
    };
}

/**
 * Registers a component style configuration in the editor.
 *
 * @param {string} variableName - The name of the variable to register the configuration for.
 * @param {Record<string, unknown>} config - The configuration to register.
 * @return {*}
 *
 * @example
 * ```typescript
 * registerComponentStyleConfiguration('MY_VARIABLE_NAME', { title: { color: 'red' } });
 * ```
 */
export function registerStyleSchemas(schemas: Record<string, Record<string, unknown>>): void {
    sendMessageToUVE({
        action: DotCMSUVEAction.REGISTER_STYLE_SCHEMAS,
        payload: {
            schemas
        }
    });
}
