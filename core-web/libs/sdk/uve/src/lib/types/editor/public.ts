/**
 * Represents the state of the Universal Visual Editor (UVE)
 * @interface
 * @property {UVE_MODE} mode - The current mode of operation for UVE (EDIT, PREVIEW, LIVE, or UNKNOWN)
 * @property {string | null} persona - The selected persona for content personalization
 * @property {string | null} variantName - The name of the current content variant
 * @property {string | null} experimentId - The identifier for the current A/B testing experiment
 * @property {string | null} publishDate - The scheduled publish date for content
 * @property {string | null} languageId - The identifier for the current language selection
 */
export interface UVEState {
    mode: UVE_MODE;
    persona: string | null;
    variantName: string | null;
    experimentId: string | null;
    publishDate: string | null;
    languageId: string | null;
}

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
 * @callback UVECallback
 * @param {unknown} payload - The payload of the event
 */
export type UVECallback = (payload: unknown) => void;

/**
 * Unsubscribe function for UVE events
 * @callback UnsubscribeUVE
 */
export type UnsubscribeUVE = () => void;

/**
 * UVESubscription type
 * @typedef {Object} UVESubscription
 * @property {UnsubscribeUVE} unsubscribe - The unsubscribe function for the UVE event
 * @property {string} event - The event name
 */
export type UVESubscription = {
    unsubscribe: UnsubscribeUVE;
    event: string;
};

/**
 * UVE event type
 * @typedef {function} UVEEvent
 */
export type UVEEvent = (callback: UVECallback) => UVESubscription;

//TODO: Recheck this after changes
/**
 * Configuration type for DotCMS Editor
 * @typedef {Object} DotCMSEditoConfig
 * @property {Object} [params] - Parameters for Page API configuration
 * @property {number} [params.depth] - The depth level for fetching page data
 * @property {string} [query] - GraphQL query string for data fetching
 */
export type DotCMSEditorConfig = { params: { depth: number } } | { query: string };

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
     * Tell the editor to do nothing
     */
    NOOP = 'noop'
}
