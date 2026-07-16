import { DotCMSContainerBound } from './internal';

import { DotCMSBasicContentlet, DotCMSPageResponse } from '../page/public';

/**
 * Development mode
 *
 * @internal
 */
export const DEVELOPMENT_MODE = 'development';

/**
 * Production mode
 *
 * @internal
 */
export const PRODUCTION_MODE = 'production';

/**
 * Represents the state of the Universal Visual Editor (UVE)
 * @interface
 * @property {UVE_MODE} mode - The current mode of operation for UVE (EDIT, PREVIEW, LIVE, or UNKNOWN)
 * @property {string | null} persona - The selected persona for content personalization
 * @property {string | null} variantName - The name of the current content variant
 * @property {string | null} experimentId - The identifier for the current A/B testing experiment
 * @property {string | null} publishDate - The scheduled publish date for content
 * @property {string | null} languageId - The identifier for the current language selection
 * @property {string | null} dotCMSHost - The host of the dotCMS instance
 */
export interface UVEState {
    mode: UVE_MODE;
    persona: string | null;
    variantName: string | null;
    experimentId: string | null;
    publishDate: string | null;
    languageId: string | null;
    dotCMSHost: string | null;
}

/**
 * The mode of the page renderer component
 * @enum {string}
 */
export type DotCMSPageRendererMode = typeof PRODUCTION_MODE | typeof DEVELOPMENT_MODE;

/**
 * Possible modes of UVE (Universal Visual Editor)
 * @enum {string}
 *
 * @property {string} LIVE - Shows published and future content
 * @property {string} PREVIEW - Shows published and working content
 * @property {string} EDIT - Enables content editing functionality in UVE
 * @property {string} UNKNOWN - Error state, UVE should not remain in this mode
 */
export enum UVE_MODE {
    EDIT = 'EDIT_MODE',
    PREVIEW = 'PREVIEW_MODE',
    LIVE = 'LIVE',
    UNKNOWN = 'UNKNOWN'
}

/**
 * Callback function for UVE events
 * @callback UVEEventHandler
 * @param {unknown} eventData - The event data
 */
export type UVEEventHandler<T = unknown> = (eventData?: T) => void;

/**
 * Unsubscribe function for UVE events
 * @callback UVEUnsubscribeFunction
 */
export type UVEUnsubscribeFunction = () => void;

/**
 * UVE event subscription type
 * @typedef {Object} UVEEventSubscription
 * @property {UVEUnsubscribeFunction} unsubscribe - The unsubscribe function for the UVE event
 * @property {string} event - The event name
 */
export type UVEEventSubscription = {
    unsubscribe: UVEUnsubscribeFunction;
    event: string;
};

/**
 * UVE event type
 * @typedef {function} UVEEventSubscriber
 */
export type UVEEventSubscriber = (callback: UVEEventHandler) => UVEEventSubscription;

/**
 * Actions send to the dotcms editor
 *
 * @export
 * @enum {number}
 */
export enum DotCMSUVEAction {
    /**
     * Tell the dotcms editor that page change
     */
    NAVIGATION_UPDATE = 'set-url',
    /**
     * Send the element position of the rows, columnsm containers and contentlets
     */
    SET_BOUNDS = 'set-bounds',
    /**
     * Send the information of a *hovered* contentlet (fires on pointermove).
     * The editor uses this to render the transient hover overlay around the
     * contentlet under the cursor. Pairs with {@link SET_SELECTED_CONTENTLET}
     * for clicks. The name is kept as `SET_CONTENTLET` for backwards
     * compatibility with external SDK consumers — semantically it is
     * "set hovered contentlet".
     */
    SET_CONTENTLET = 'set-contentlet',
    /**
     * Send the information of a contentlet that was *clicked* inside the iframe.
     * The editor uses this to promote the clicked contentlet to "selected"
     * (persistent action toolbar + opens the quick-edit panel). Pairs with
     * {@link SET_CONTENTLET} which handles hover.
     */
    SET_SELECTED_CONTENTLET = 'set-selected-contentlet',
    /**
     * Tell the editor that the page is being scrolled
     */
    IFRAME_SCROLL = 'scroll',
    /**
     * Tell the editor that the page has stopped scrolling
     */
    IFRAME_SCROLL_END = 'scroll-end',
    /**
     * Ping the editor to see if the page is inside the editor
     */
    PING_EDITOR = 'ping-editor',
    /**
     * Tell the editor to init the inline editing editor.
     */
    INIT_INLINE_EDITING = 'init-inline-editing',
    /**
     * Tell the editor to open the Copy-contentlet dialog
     * To copy a content and then edit it inline.
     */
    COPY_CONTENTLET_INLINE_EDITING = 'copy-contentlet-inline-editing',
    /**
     * Tell the editor to save inline edited contentlet
     */
    UPDATE_CONTENTLET_INLINE_EDITING = 'update-contentlet-inline-editing',
    /**
     * Tell the editor to trigger a menu reorder
     */
    REORDER_MENU = 'reorder-menu',
    /**
     * Tell the editor to send the page info to iframe
     */
    GET_PAGE_DATA = 'get-page-data',
    /**
     * Tell the editor an user send a graphql query
     */
    CLIENT_READY = 'client-ready',
    /**
     * Tell the editor to edit a contentlet
     */
    EDIT_CONTENTLET = 'edit-contentlet',
    /**
     * Tell the editor to register style schemas
     */
    REGISTER_STYLE_SCHEMAS = 'register-style-schemas',
    /**
     * Tell the editor to report the iframe height
     */
    IFRAME_HEIGHT = 'iframe-height',
    /**
     * Tell the editor to create a contentlet without adding it to the page
     */
    CREATE_CONTENTLET = 'create-contentlet',
    /**
     * Tell the editor to do nothing
     */
    NOOP = 'noop',
    /**
     * Report the offsetTop of a page section so the editor can scroll to it
     */
    SECTION_OFFSET = 'section-offset'
}

/**
 * The contentlet has the main fields and the custom fields of the content type.
 *
 * @template T - The custom fields of the content type.
 */
export type Contentlet<T> = T & DotCMSBasicContentlet;

/**
 * Available events in the Universal Visual Editor
 * @enum {string}
 */
export enum UVEEventType {
    /**
     * Triggered when page data changes from the editor
     */
    CONTENT_CHANGES = 'changes',

    /**
     * Triggered when the page needs to be reloaded
     */
    PAGE_RELOAD = 'page-reload',

    /**
     * Triggered when scroll action is needed inside the iframe
     */
    IFRAME_SCROLL = 'iframe-scroll',

    /**
     * Triggered when a contentlet is hovered
     */
    CONTENTLET_HOVERED = 'contentlet-hovered',

    /**
     * Triggered when a contentlet is clicked (capture-phase `click` event on
     * its element). Used by the editor to promote the clicked contentlet to
     * "selected" without the editor having to capture pointer events on its
     * hover overlay.
     */
    CONTENTLET_CLICKED = 'contentlet-clicked',

    /**
     * Triggered when the editor requests a scroll to a specific page section
     * @internal
     */
    SCROLL_TO_SECTION = 'scroll-to-section',

    /**
     * Triggered when the editor clears its selection (resize, scroll, navigation).
     * The SDK uses this to reset its "last selected" tracker so a subsequent click
     * on the same contentlet re-emits CONTENTLET_CLICKED instead of being treated
     * as a passthrough.
     * @internal
     */
    SELECTION_CLEARED = 'selection-cleared',

    /**
     * The single bounds-sync channel. The SDK observes the iframe document
     * and every `[data-dot-object="container"]` with a debounced
     * ResizeObserver and emits the full page bounds whenever the layout
     * settles (sidebar open/close, device/zoom change, media-query
     * reflows, image/font load shifts, scroll, etc.). The editor can
     * also send a UVE_FLUSH_BOUNDS message to bypass the debounce when it
     * needs an immediate snapshot (drag/drop dropzone).
     * @internal
     */
    AUTO_BOUNDS = 'auto-bounds'
}

/**
 * Type definitions for each event's payload
 */
export type UVEEventPayloadMap = {
    [UVEEventType.CONTENT_CHANGES]: DotCMSPageResponse;
    [UVEEventType.PAGE_RELOAD]: undefined;
    [UVEEventType.IFRAME_SCROLL]: 'up' | 'down';
    // TODO: Add type here
    [UVEEventType.CONTENTLET_HOVERED]: unknown;
    [UVEEventType.CONTENTLET_CLICKED]: unknown;
    [UVEEventType.SCROLL_TO_SECTION]: { sectionIndex: number };
    [UVEEventType.SELECTION_CLEARED]: undefined;
    [UVEEventType.AUTO_BOUNDS]: DotCMSContainerBound[];
};

/**
 *
 * Interface representing the data needed for container editing
 * @interface EditableContainerData
 */
export interface EditableContainerData {
    uuid: string;
    identifier: string;
    acceptTypes: string;
    maxContentlets: number;
}
