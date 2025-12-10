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
     * Send the information of the hovered contentlet
     */
    SET_CONTENTLET = 'set-contentlet',
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
     * Tell the editor to do nothing
     */
    NOOP = 'noop'
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
     * Triggered when the editor requests container bounds
     */
    REQUEST_BOUNDS = 'request-bounds',

    /**
     * Triggered when scroll action is needed inside the iframe
     */
    IFRAME_SCROLL = 'iframe-scroll',

    /**
     * Triggered when a contentlet is hovered
     */
    CONTENTLET_HOVERED = 'contentlet-hovered'
}

/**
 * Type definitions for each event's payload
 */
export type UVEEventPayloadMap = {
    [UVEEventType.CONTENT_CHANGES]: DotCMSPageResponse;
    [UVEEventType.PAGE_RELOAD]: undefined;
    [UVEEventType.REQUEST_BOUNDS]: DotCMSContainerBound[];
    [UVEEventType.IFRAME_SCROLL]: 'up' | 'down';
    // TODO: Add type here
    [UVEEventType.CONTENTLET_HOVERED]: unknown;
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
    variantId?: string;
}
